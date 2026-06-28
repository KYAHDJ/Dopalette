package com.dopalette.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

enum class BrushStyle {
    MARKER, NEON_GLOW, AIRBRUSH, WATERCOLOR, CHISEL, REGION_FILL
}

enum class PathActionType {
    MOVE_TO, LINE_TO, QUAD_TO
}

// Concrete serializable representations of individual drawing vectors
data class SerializablePathAction(
    val type: PathActionType,
    val x: Float,
    val y: Float,
    val x2: Float = 0f,
    val y2: Float = 0f
)

data class StrokeData(
    val path: Path,
    val color: Color,
    val width: Float,
    val style: BrushStyle = BrushStyle.MARKER,
    val canvasWidth: Float = 0f,
    val canvasHeight: Float = 0f,
    val regionSeedX: Float = -1f,
    val regionSeedY: Float = -1f,
    // The explicit backing points array that makes permanent local storage possible
    val serializableActions: MutableList<SerializablePathAction> = mutableListOf()
)

enum class DisplayLayer {
    DRAFT, FINISHED
}

data class FinishedArtwork(
    val title: String,
    val strokes: List<StrokeData>,
    val layerName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = "${title}_${timestamp}"
)

object ArtworkStore {
    // SAVE V2 TEST MODE
    // Old save folders are intentionally NOT deleted. They are hidden by switching
    // all load/save paths to the V2 folders below. This makes testing honest:
    // if V2 fails, the old format will not silently rescue the artwork.
    private const val USE_SAVE_V2_ONLY = true
    private const val V2_DRAFT_FOLDER = "drafts_v2"
    private const val V2_FINISHED_FOLDER = "finished_v2"
    private val draftLayers = mutableMapOf<String, List<StrokeData>>()
    private val draftFileTitles = mutableSetOf<String>()
    private val finishedLayers = mutableMapOf<String, MutableList<FinishedArtwork>>()
    private val displayLayers = mutableMapOf<String, DisplayLayer>()
    private val displayFinishedIndexes = mutableMapOf<String, Int>()

    const val MAX_FINISHED_LAYERS = 2

    val globalUpdateTick = mutableIntStateOf(0)
    val version: androidx.compose.runtime.MutableIntState get() = globalUpdateTick

    private lateinit var context: Context
    private val gson = Gson()
    private val storageExecutor = Executors.newSingleThreadExecutor()
    private val storageLock = Any()
    private val storageEpoch = AtomicLong(0L)
    private val saveGenerations = ConcurrentHashMap<String, Long>()
    private val previewVersions = ConcurrentHashMap<String, Long>()


    fun initialize(appContext: Context) {
        context = appContext.applicationContext
        clearInMemoryIndexes()
        recreateLocalArtworkFolders()
        clearLegacyThumbnailCacheOnce()
        loadAllArtwork()
        ArtworkCloudSync.initialize(context)
        bump()
    }

    fun loadDraft(title: String): List<StrokeData> {
        draftLayers[title]?.let { return it }
        if (!draftFileTitles.contains(title)) return emptyList()
        val loaded = loadDraftFromDisk(title)
        if (loaded.isNotEmpty()) draftLayers[title] = loaded
        return loaded
    }

    fun saveDraft(title: String, strokes: List<StrokeData>) {
        saveDraft(title, strokes, renderPreview = true)
    }

    fun saveDraftDataOnly(title: String, strokes: List<StrokeData>) {
        saveDraft(title, strokes, renderPreview = false)
    }

    fun saveDraftWithPreviewNow(title: String, strokes: List<StrokeData>) {
        val copied = deepCopyStrokes(strokes)
        draftLayers[title] = copied
        displayLayers[title] = DisplayLayer.DRAFT
        val generation = nextSaveGeneration(title)
        persistDraft(title, copied, renderPreview = true, generation = generation)
        bump()
    }

    private fun saveDraft(title: String, strokes: List<StrokeData>, renderPreview: Boolean) {
        val copied = deepCopyStrokes(strokes)
        draftLayers[title] = copied
        displayLayers[title] = DisplayLayer.DRAFT
        val generation = nextSaveGeneration(title)
        persistDraftAsync(title, copied, renderPreview, generation)
        bump()
    }

    fun touch() {
        bump()
    }

    fun clearDraft(title: String) {
        draftLayers.remove(title)
        if (displayLayers[title] == DisplayLayer.DRAFT) {
            if (finishedFor(title).isNotEmpty()) {
                displayLayers[title] = DisplayLayer.FINISHED
                displayFinishedIndexes[title] = finishedFor(title).lastIndex
            } else {
                displayLayers.remove(title)
                displayFinishedIndexes.remove(title)
            }
        }
        deleteDraftFileAsync(title)
        bump()
    }

    fun hasDraft(title: String): Boolean = draftLayers[title]?.isNotEmpty() == true || draftFileTitles.contains(title)

    fun hasAnyProgress(title: String): Boolean = hasDraft(title) || finishedFor(title).isNotEmpty()

    fun draftStrokeCount(title: String): Int = draftLayers[title]?.size ?: if (draftFileTitles.contains(title)) -1 else 0

    fun finishedFor(title: String): List<FinishedArtwork> = finishedLayers[title].orEmpty()

    fun hasFinished(title: String): Boolean = finishedFor(title).isNotEmpty()

    fun latestFinished(title: String): FinishedArtwork? = finishedFor(title).maxByOrNull { it.timestamp }

    fun canAddFinished(title: String): Boolean = finishedFor(title).size < MAX_FINISHED_LAYERS

    fun previewStrokes(title: String): List<StrokeData> {
        // Important: do NOT lazy-load every JSON just to build Home/Me thumbnails.
        // If a draft/finished file exists but is not loaded yet, Home uses its flattened preview PNG.
        val draft = draftLayers[title].orEmpty()
        if (draft.isNotEmpty()) return draft

        val stack = finishedFor(title)
        if (stack.isEmpty()) return emptyList()

        val displayedIndex = if (displayLayers[title] == DisplayLayer.FINISHED) {
            displayFinishedIndexes[title]?.coerceIn(0, stack.lastIndex)
        } else {
            null
        }
        return stack.getOrNull(displayedIndex ?: stack.lastIndex)?.strokes.orEmpty()
    }

    fun addFinished(title: String, strokes: List<StrokeData>): FinishedArtwork? {
        return addFinishedInternal(title, strokes, blocking = false)
    }

    fun addFinishedWithPreviewNow(title: String, strokes: List<StrokeData>): FinishedArtwork? {
        return addFinishedInternal(title, strokes, blocking = true)
    }

    private fun addFinishedInternal(title: String, strokes: List<StrokeData>, blocking: Boolean): FinishedArtwork? {
        val stack = finishedLayers.getOrPut(title) { mutableListOf() }
        if (stack.size >= MAX_FINISHED_LAYERS) {
            displayLayers[title] = DisplayLayer.FINISHED
            displayFinishedIndexes[title] = stack.lastIndex
            bump()
            return null
        }

        val createdAt = System.currentTimeMillis()
        val final = FinishedArtwork(
            title = title,
            strokes = deepCopyStrokes(strokes),
            layerName = "Finished Layer ${stack.size + 1}",
            timestamp = createdAt,
            id = "${title.sanitizeFilename()}_${createdAt}_${stack.size + 1}"
        )
        stack.add(final)
        displayLayers[title] = DisplayLayer.FINISHED
        displayFinishedIndexes[title] = stack.lastIndex
        val generation = nextSaveGeneration(title)
        if (blocking) {
            persistFinishedArtwork(final, renderPreview = true, generation = generation)
        } else {
            persistFinishedArtworkAsync(final, renderPreview = true, generation = generation)
        }
        bump()
        return final
    }

    fun updateFinishedArtwork(id: String, strokes: List<StrokeData>): FinishedArtwork? {
        return updateFinishedArtworkInternal(id, strokes, renderPreview = true, blocking = false)
    }

    fun updateFinishedArtworkDataOnly(id: String, strokes: List<StrokeData>): FinishedArtwork? {
        return updateFinishedArtworkInternal(id, strokes, renderPreview = false, blocking = false)
    }

    fun updateFinishedArtworkWithPreviewNow(id: String, strokes: List<StrokeData>): FinishedArtwork? {
        return updateFinishedArtworkInternal(id, strokes, renderPreview = true, blocking = true)
    }

    private fun updateFinishedArtworkInternal(
        id: String,
        strokes: List<StrokeData>,
        renderPreview: Boolean,
        blocking: Boolean
    ): FinishedArtwork? {
        finishedLayers.forEach { (title, stack) ->
            val index = stack.indexOfFirst { it.id == id }
            if (index >= 0) {
                val existing = if (stack[index].strokes.isEmpty()) loadFinishedArtworkFromDisk(id) ?: stack[index] else stack[index]
                val updated = existing.copy(
                    strokes = deepCopyStrokes(strokes),
                    timestamp = System.currentTimeMillis()
                )
                stack[index] = updated
                displayLayers[title] = DisplayLayer.FINISHED
                displayFinishedIndexes[title] = index
                val generation = nextSaveGeneration(title)
                if (blocking) {
                    persistFinishedArtwork(updated, renderPreview, generation)
                } else {
                    persistFinishedArtworkAsync(updated, renderPreview, generation)
                }
                bump()
                return updated
            }
        }
        return null
    }

    fun displayedStrokes(title: String): List<StrokeData> {
        return when (displayLayers[title]) {
            DisplayLayer.FINISHED -> {
                val stack = finishedFor(title)
                val index = displayFinishedIndexes[title]?.coerceIn(0, stack.lastIndex) ?: stack.lastIndex
                stack.getOrNull(index)?.strokes.orEmpty()
            }
            else -> loadDraft(title)
        }
    }

    fun displayDraft(title: String) {
        displayLayers[title] = DisplayLayer.DRAFT
        bump()
    }

    fun displayFinished(title: String, index: Int = finishedFor(title).lastIndex) {
        if (index >= 0) {
            displayLayers[title] = DisplayLayer.FINISHED
            displayFinishedIndexes[title] = index
            bump()
        }
    }

    fun allProgressTitles(): List<String> {
        return (draftFileTitles + draftLayers.keys + finishedLayers.keys).distinct().sorted()
    }

    fun getFinishedArtworkById(id: String): FinishedArtwork? {
        finishedLayers.values.forEach { stack ->
            stack.forEachIndexed { index, artwork ->
                if (artwork.id == id) {
                    if (artwork.strokes.isNotEmpty()) return artwork
                    val loaded = loadFinishedArtworkFromDisk(id) ?: return artwork
                    stack[index] = loaded
                    return loaded
                }
            }
        }
        return null
    }

    fun deleteFinishedArtwork(id: String) {
        val emptyTitles = mutableListOf<String>()
        finishedLayers.forEach { (title, stack) ->
            val removedIndex = stack.indexOfFirst { it.id == id }
            if (removedIndex >= 0) {
                stack.removeAt(removedIndex)
                if (stack.isEmpty()) {
                    emptyTitles.add(title)
                    if (displayLayers[title] == DisplayLayer.FINISHED) {
                        displayLayers.remove(title)
                        displayFinishedIndexes.remove(title)
                    }
                } else {
                    val safeIndex = (displayFinishedIndexes[title] ?: stack.lastIndex).coerceIn(0, stack.lastIndex)
                    displayFinishedIndexes[title] = safeIndex
                    if (displayLayers[title] == DisplayLayer.FINISHED) displayLayers[title] = DisplayLayer.FINISHED
                }
            }
        }
        emptyTitles.forEach { finishedLayers.remove(it) }
        deleteFinishedFileAsync(id)
        bump()
    }


    fun hasAnyLocalArtwork(): Boolean {
        return draftLayers.isNotEmpty() || draftFileTitles.isNotEmpty() || finishedLayers.values.any { it.isNotEmpty() }
    }

    fun eraseAllLocalArtwork() {
        try {
            // Invalidate every queued/old async save before deleting files. This prevents
            // a pre-reset brush/preview write from recreating stale JSON after Delete Account.
            storageEpoch.incrementAndGet()
            clearInMemoryIndexes()
            if (::context.isInitialized) {
                synchronized(storageLock) {
                    getStorageDir().deleteRecursively()
                    recreateLocalArtworkFolders()
                }
            }
            bump()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearInMemoryIndexes() {
        draftLayers.clear()
        draftFileTitles.clear()
        finishedLayers.clear()
        displayLayers.clear()
        displayFinishedIndexes.clear()
        previewVersions.clear()
        saveGenerations.clear()
    }


    fun recreateLocalArtworkFolders() {
        if (!::context.isInitialized) return
        try {
            getDraftsDir()
            getFinishedDir()
            getPreviewsDir()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deepCopyStrokes(strokes: List<StrokeData>): List<StrokeData> {
        return strokes.map { stroke ->
            val copiedActions = stroke.serializableActions.toMutableList()
            val rebuiltPath = Path()
            copiedActions.forEach { action ->
                when (action.type) {
                    PathActionType.MOVE_TO -> rebuiltPath.moveTo(action.x, action.y)
                    PathActionType.LINE_TO -> rebuiltPath.lineTo(action.x, action.y)
                    PathActionType.QUAD_TO -> rebuiltPath.quadraticBezierTo(action.x, action.y, action.x2, action.y2)
                }
            }
            stroke.copy(path = rebuiltPath, serializableActions = copiedActions)
        }
    }

    private fun bump() {
        globalUpdateTick.intValue += 1
    }

    private fun nextSaveGeneration(title: String): Long {
        val generation = System.currentTimeMillis()
        saveGenerations[title] = generation
        return generation
    }

    private fun isLatestGeneration(title: String, generation: Long): Boolean {
        return saveGenerations[title] == generation
    }

    fun previewVersion(title: String): Long {
        return maxOf(previewVersions[title] ?: 0L, latestPreviewModified(title), latestArtworkSourceModified(title))
    }

    fun latestArtworkSourceModified(title: String): Long {
        val draftFile = File(getDraftsDir(), "${title.sanitizeFilename()}.json")
        val draftModified = if (draftFile.exists()) draftFile.lastModified() else 0L
        val finishedModified = finishedFor(title).maxOfOrNull { artwork ->
            val file = File(getFinishedDir(), "${artwork.id}.json")
            if (file.exists()) file.lastModified() else artwork.timestamp
        } ?: 0L
        return maxOf(draftModified, finishedModified)
    }

    fun isDisplayPreviewFresh(title: String): Boolean {
        val preview = displayPreviewFile(title)
        if (!preview.exists() || preview.length() <= 0L) return false
        val sourceModified = latestArtworkSourceModified(title)
        return sourceModified <= 0L || preview.lastModified() >= sourceModified
    }

    fun loadThumbnailSourceStrokes(title: String): List<StrokeData> {
        if (hasDraft(title)) {
            val draft = loadDraft(title)
            if (draft.isNotEmpty()) return draft
        }

        // Use the real mutable finished stack here. finishedFor() intentionally
        // exposes a read-only List, so assigning through it breaks Kotlin
        // compilation. The thumbnail loader may hydrate an empty in-memory
        // finished layer from disk, so it must update finishedLayers directly.
        val stack = finishedLayers[title] ?: return emptyList()
        if (stack.isNotEmpty()) {
            val index = (displayFinishedIndexes[title] ?: stack.lastIndex).coerceIn(0, stack.lastIndex)
            val artwork = stack.getOrNull(index)
            if (artwork != null) {
                if (artwork.strokes.isNotEmpty()) return artwork.strokes
                val loaded = loadFinishedArtworkFromDisk(artwork.id)
                if (loaded != null) {
                    stack[index] = loaded
                    return loaded.strokes
                }
            }
        }
        return emptyList()
    }

    private fun updatePreviewVersion(title: String) {
        previewVersions[title] = System.currentTimeMillis()
        bump()
    }

    private fun clearLegacyThumbnailCacheOnce() {
        try {
            val prefs = context.getSharedPreferences("dopalette_thumbnail_v2", Context.MODE_PRIVATE)
            if (prefs.getBoolean("cleared_thumbnail_v3_strict", false)) return
            val dir = File(getStorageDir(), "previews")
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension.equals("png", ignoreCase = true)) file.delete()
            }
            prefs.edit().putBoolean("cleared_thumbnail_v3_strict", true).apply()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    // Storage Sandbox Configuration
    private fun getStorageDir(): File {
        val baseDir = File(context.filesDir, "dopalette_artwork")
        if (!baseDir.exists()) baseDir.mkdirs()
        return baseDir
    }

    private fun getDraftsDir(): File {
        val folder = if (USE_SAVE_V2_ONLY) V2_DRAFT_FOLDER else "drafts"
        val dir = File(getStorageDir(), folder)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getFinishedDir(): File {
        val folder = if (USE_SAVE_V2_ONLY) V2_FINISHED_FOLDER else "finished"
        val dir = File(getStorageDir(), folder)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getPreviewsDir(): File {
        val dir = File(getStorageDir(), "previews")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun draftPreviewFile(title: String): File = File(getPreviewsDir(), "draft_${title.sanitizeFilename()}.png")
    private fun displayPreviewFile(title: String): File = File(getPreviewsDir(), "display_${title.sanitizeFilename()}.png")
    private fun finishedPreviewFile(id: String): File = File(getPreviewsDir(), "finished_${id.sanitizeFilename()}.png")

    private fun latestPreviewModified(title: String): Long {
        val display = displayPreviewFile(title)
        val draft = draftPreviewFile(title)
        return maxOf(
            if (display.exists()) display.lastModified() else 0L,
            if (draft.exists()) draft.lastModified() else 0L
        )
    }

    fun ensureFreshDisplayPreviewBitmap(title: String): Bitmap? {
        return try {
            val existing = loadDisplayPreviewBitmap(title)
            if (existing != null) return existing

            val sourceStrokes = loadThumbnailSourceStrokes(title)
            if (sourceStrokes.isEmpty()) return null

            val (baseWidth, baseHeight) = SelectionArtworkAssets.baseSize(context, title)
            val previewWidth = 512
            val previewHeight = ((previewWidth.toFloat() * baseHeight.toFloat() / baseWidth.toFloat().coerceAtLeast(1f)).toInt()).coerceAtLeast(1)
            val rebuilt = ArtworkRenderer.renderArtworkBitmap(context, sourceStrokes, width = previewWidth, height = previewHeight, title = title) ?: return null
            writePreviewAtomically(displayPreviewFile(title), rebuilt)
            val sourceModified = latestArtworkSourceModified(title).coerceAtLeast(System.currentTimeMillis())
            displayPreviewFile(title).setLastModified(sourceModified)
            updatePreviewVersion(title)
            rebuilt
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    fun loadDisplayPreviewBitmap(title: String): Bitmap? {
        return try {
            if (!isDisplayPreviewFresh(title)) return null
            val display = displayPreviewFile(title)
            val draft = draftPreviewFile(title)
            val file = when {
                displayLayers[title] == DisplayLayer.DRAFT && draft.exists() && draft.lastModified() >= latestArtworkSourceModified(title) -> draft
                display.exists() -> display
                draft.exists() && draft.lastModified() >= latestArtworkSourceModified(title) -> draft
                else -> null
            }
            file?.let { BitmapFactory.decodeFile(it.absolutePath) }
        } catch (e: Exception) {
            null
        }
    }

    private fun persistDraftAsync(title: String, strokes: List<StrokeData>, renderPreview: Boolean, generation: Long) {
        val epoch = storageEpoch.get()
        storageExecutor.execute { if (epoch == storageEpoch.get()) persistDraft(title, strokes, renderPreview, generation, epoch) }
    }

    private fun persistFinishedArtworkAsync(artwork: FinishedArtwork, renderPreview: Boolean, generation: Long) {
        val epoch = storageEpoch.get()
        storageExecutor.execute { if (epoch == storageEpoch.get()) persistFinishedArtwork(artwork, renderPreview, generation, epoch) }
    }

    private fun deleteDraftFileAsync(title: String) {
        val epoch = storageEpoch.get()
        storageExecutor.execute { if (epoch == storageEpoch.get()) deleteDraftFile(title) }
    }

    private fun deleteFinishedFileAsync(id: String) {
        val epoch = storageEpoch.get()
        storageExecutor.execute { if (epoch == storageEpoch.get()) deleteFinishedFile(id) }
    }

    private fun persistDraft(title: String, strokes: List<StrokeData>, renderPreview: Boolean, generation: Long, epoch: Long = storageEpoch.get()) {
        try {
            if (epoch != storageEpoch.get()) return
            synchronized(storageLock) {
                if (epoch != storageEpoch.get()) return
                val json = strokesListToJson(strokes)
                val file = File(getDraftsDir(), "${title.sanitizeFilename()}.json")
                file.writeText(json)
                // Local-first save. Cloud sync is intentionally triggered only when the
                // editor Back/Done flow confirms real user changes, so normal autosaves
                // do not slow down painting or app navigation.
                draftFileTitles.add(title)
                if (renderPreview && isLatestGeneration(title, generation) && epoch == storageEpoch.get()) {
                    persistPreviewBitmaps(title, strokes, null, generation)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistFinishedArtwork(artwork: FinishedArtwork, renderPreview: Boolean, generation: Long, epoch: Long = storageEpoch.get()) {
        try {
            if (epoch != storageEpoch.get()) return
            synchronized(storageLock) {
                if (epoch != storageEpoch.get()) return
                val json = finishedArtworkToJson(artwork)
                val file = File(getFinishedDir(), "${artwork.id}.json")
                file.writeText(json)
                // Local-first save. Cloud sync is intentionally triggered only when the
                // editor Back/Done flow confirms real user changes.
                if (renderPreview && isLatestGeneration(artwork.title, generation) && epoch == storageEpoch.get()) {
                    persistPreviewBitmaps(artwork.title, artwork.strokes, artwork.id, generation)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteDraftFile(title: String) {
        try {
            val file = File(getDraftsDir(), "${title.sanitizeFilename()}.json")
            if (file.exists()) file.delete()
            ArtworkCloudSync.deleteDraft(title)
            draftFileTitles.remove(title)
            val preview = draftPreviewFile(title)
            if (preview.exists()) preview.delete()
            updatePreviewVersion(title)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteFinishedFile(id: String) {
        try {
            val file = File(getFinishedDir(), "$id.json")
            if (file.exists()) file.delete()
            ArtworkCloudSync.deleteFinished(id)
            val preview = finishedPreviewFile(id)
            if (preview.exists()) preview.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAllArtwork() {
        try {
            draftLayers.clear()
            draftFileTitles.clear()
            finishedLayers.clear()

            // Startup must stay light. Only remember draft file names here.
            // The heavy editable stroke JSON is loaded only when the user opens that one artwork.
            getDraftsDir().listFiles()?.forEach { file ->
                if (file.extension.equals("json", ignoreCase = true)) {
                    draftFileTitles.add(file.nameWithoutExtension)
                }
            }

            // Load only finished metadata. Full stroke data stays on disk until that layer is opened.
            getFinishedDir().listFiles()?.forEach { file ->
                try {
                    val json = file.readText()
                    val artwork = jsonToFinishedArtworkMetadata(json, file.nameWithoutExtension)
                    val stack = finishedLayers.getOrPut(artwork.title) { mutableListOf() }
                    stack.add(artwork)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            finishedLayers.forEach { (title, stack) ->
                stack.sortBy { it.timestamp }
                if (stack.size > MAX_FINISHED_LAYERS) {
                    val excess = stack.dropLast(MAX_FINISHED_LAYERS)
                    excess.forEach { deleteFinishedFile(it.id) }
                    val kept = stack.takeLast(MAX_FINISHED_LAYERS).toMutableList()
                    stack.clear()
                    stack.addAll(kept)
                }
                if (stack.isNotEmpty()) {
                    displayLayers[title] = DisplayLayer.FINISHED
                    displayFinishedIndexes[title] = stack.lastIndex
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun loadDraftFromDisk(title: String): List<StrokeData> {
        return try {
            val file = File(getDraftsDir(), "${title.sanitizeFilename()}.json")
            if (!file.exists()) emptyList() else jsonToStrokesList(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistPreviewBitmaps(title: String, strokes: List<StrokeData>, finishedId: String?, generation: Long) {
        try {
            if (strokes.isEmpty() || !isLatestGeneration(title, generation)) return
            val (baseWidth, baseHeight) = SelectionArtworkAssets.baseSize(context, title)
            val previewWidth = 512
            val previewHeight = ((previewWidth.toFloat() * baseHeight.toFloat() / baseWidth.toFloat().coerceAtLeast(1f)).toInt()).coerceAtLeast(1)
            val preview = ArtworkRenderer.renderArtworkBitmap(context, strokes, width = previewWidth, height = previewHeight, title = title) ?: return
            if (!isLatestGeneration(title, generation)) {
                if (!preview.isRecycled) preview.recycle()
                return
            }
            val target = if (finishedId == null) draftPreviewFile(title) else finishedPreviewFile(finishedId)
            writePreviewAtomically(target, preview)
            writePreviewAtomically(displayPreviewFile(title), preview)
            val sourceModified = latestArtworkSourceModified(title).coerceAtLeast(System.currentTimeMillis())
            target.setLastModified(sourceModified)
            displayPreviewFile(title).setLastModified(sourceModified)
            updatePreviewVersion(title)
            if (!preview.isRecycled) preview.recycle()
        } catch (e: Throwable) {
            // A preview must never crash saving. Editable JSON is still saved above.
            e.printStackTrace()
        }
    }

    private fun writePreviewAtomically(target: File, bitmap: Bitmap) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 96, out) }
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    }

    private fun loadFinishedArtworkFromDisk(id: String): FinishedArtwork? {
        return try {
            val file = File(getFinishedDir(), "$id.json")
            if (!file.exists()) null else jsonToFinishedArtwork(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    private fun strokesListToJson(strokes: List<StrokeData>): String {
        val obj = JsonObject()
        obj.addProperty("v", 2)
        obj.add("s", strokesToCompactJsonArray(strokes))
        return obj.toString()
    }

    private fun strokesToCompactJsonArray(strokes: List<StrokeData>): JsonArray {
        val arr = JsonArray()
        strokes.forEach { stroke -> arr.add(strokeToCompactJson(stroke)) }
        return arr
    }

    private fun strokeToCompactJson(stroke: StrokeData): JsonArray {
        val strokeArr = JsonArray()
        strokeArr.add(stroke.color.toArgb())
        strokeArr.add(stroke.width)
        strokeArr.add(stroke.style.ordinal)
        strokeArr.add(stroke.canvasWidth)
        strokeArr.add(stroke.canvasHeight)
        strokeArr.add(stroke.regionSeedX)
        strokeArr.add(stroke.regionSeedY)

        val actionsArr = JsonArray()
        stroke.serializableActions.forEach { action ->
            val act = JsonArray()
            act.add(action.type.ordinal)
            act.add(action.x)
            act.add(action.y)
            act.add(action.x2)
            act.add(action.y2)
            actionsArr.add(act)
        }
        strokeArr.add(actionsArr)
        return strokeArr
    }

    private fun strokeToJson(stroke: StrokeData): JsonObject {
        val obj = JsonObject()
        obj.addProperty("color", stroke.color.toArgb())
        obj.addProperty("width", stroke.width)
        obj.addProperty("style", stroke.style.name)
        obj.addProperty("canvasWidth", stroke.canvasWidth)
        obj.addProperty("canvasHeight", stroke.canvasHeight)
        obj.addProperty("regionSeedX", stroke.regionSeedX)
        obj.addProperty("regionSeedY", stroke.regionSeedY)

        val actionsArr = JsonArray()
        stroke.serializableActions.forEach { action ->
            val actObj = JsonObject()
            actObj.addProperty("type", action.type.name)
            actObj.addProperty("x", action.x)
            actObj.addProperty("y", action.y)
            actObj.addProperty("x2", action.x2)
            actObj.addProperty("y2", action.y2)
            actionsArr.add(actObj)
        }
        obj.add("actions", actionsArr)
        return obj
    }

    private fun jsonToStrokesList(json: String): List<StrokeData> {
        return try {
            val root = JsonParser.parseString(json)
            when {
                root.isJsonObject && root.asJsonObject.has("s") -> {
                    root.asJsonObject.getAsJsonArray("s").map { jsonToCompactStroke(it.asJsonArray) }
                }
                root.isJsonArray -> {
                    root.asJsonArray.map { jsonToStroke(it.asJsonObject) }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun jsonToCompactStroke(arr: JsonArray): StrokeData {
        val colorInt = arr[0].asInt
        val widthFloat = arr[1].asFloat
        val styleIndex = arr[2].asInt.coerceIn(0, BrushStyle.values().lastIndex)
        val styleEnum = BrushStyle.values()[styleIndex]
        val canvasW = arr[3].asFloat
        val canvasH = arr[4].asFloat
        val regionSeedX = arr[5].asFloat
        val regionSeedY = arr[6].asFloat
        val actions = mutableListOf<SerializablePathAction>()

        val actionsArr = arr[7].asJsonArray
        actionsArr.forEach { element ->
            val act = element.asJsonArray
            val typeIndex = act[0].asInt.coerceIn(0, PathActionType.values().lastIndex)
            actions.add(
                SerializablePathAction(
                    type = PathActionType.values()[typeIndex],
                    x = act[1].asFloat,
                    y = act[2].asFloat,
                    x2 = act[3].asFloat,
                    y2 = act[4].asFloat
                )
            )
        }
        return rebuildStrokeFromActions(Color(colorInt), widthFloat, styleEnum, canvasW, canvasH, regionSeedX, regionSeedY, actions)
    }

    private fun jsonToStroke(obj: JsonObject): StrokeData {
        val colorInt = obj.get("color").asInt
        val widthFloat = obj.get("width").asFloat
        val styleEnum = BrushStyle.valueOf(obj.get("style").asString)
        val canvasW = obj.get("canvasWidth").asFloat
        val canvasH = obj.get("canvasHeight").asFloat
        val regionSeedX = if (obj.has("regionSeedX")) obj.get("regionSeedX").asFloat else -1f
        val regionSeedY = if (obj.has("regionSeedY")) obj.get("regionSeedY").asFloat else -1f

        val actions = mutableListOf<SerializablePathAction>()
        if (obj.has("actions")) {
            val actionsArr = obj.getAsJsonArray("actions")
            actionsArr.forEach { element ->
                val actObj = element.asJsonObject
                actions.add(
                    SerializablePathAction(
                        type = PathActionType.valueOf(actObj.get("type").asString),
                        x = actObj.get("x").asFloat,
                        y = actObj.get("y").asFloat,
                        x2 = if (actObj.has("x2")) actObj.get("x2").asFloat else 0f,
                        y2 = if (actObj.has("y2")) actObj.get("y2").asFloat else 0f
                    )
                )
            }
        }
        return rebuildStrokeFromActions(Color(colorInt), widthFloat, styleEnum, canvasW, canvasH, regionSeedX, regionSeedY, actions)
    }

    private fun rebuildStrokeFromActions(
        color: Color,
        width: Float,
        style: BrushStyle,
        canvasWidth: Float,
        canvasHeight: Float,
        regionSeedX: Float,
        regionSeedY: Float,
        actions: MutableList<SerializablePathAction>
    ): StrokeData {
        val pathInstance = Path()
        actions.forEach { action ->
            when (action.type) {
                PathActionType.MOVE_TO -> pathInstance.moveTo(action.x, action.y)
                PathActionType.LINE_TO -> pathInstance.lineTo(action.x, action.y)
                PathActionType.QUAD_TO -> pathInstance.quadraticBezierTo(action.x, action.y, action.x2, action.y2)
            }
        }
        return StrokeData(
            path = pathInstance,
            color = color,
            width = width,
            style = style,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            regionSeedX = regionSeedX,
            regionSeedY = regionSeedY,
            serializableActions = actions
        )
    }

    private fun finishedArtworkToJson(artwork: FinishedArtwork): String {
        val obj = JsonObject()
        obj.addProperty("v", 2)
        obj.addProperty("id", artwork.id)
        obj.addProperty("t", artwork.title)
        obj.addProperty("n", artwork.layerName)
        obj.addProperty("ms", artwork.timestamp)
        obj.add("s", strokesToCompactJsonArray(artwork.strokes))
        return obj.toString()
    }

    private fun jsonToFinishedArtworkMetadata(json: String, fallbackId: String): FinishedArtwork {
        return try {
            val obj = JsonParser.parseString(json).asJsonObject
            val title = when {
                obj.has("t") -> obj.get("t").asString
                obj.has("title") -> obj.get("title").asString
                else -> fallbackId
            }
            val id = if (obj.has("id")) obj.get("id").asString else fallbackId
            val layerName = when {
                obj.has("n") -> obj.get("n").asString
                obj.has("layerName") -> obj.get("layerName").asString
                else -> "Finished Layer"
            }
            val timestamp = when {
                obj.has("ms") -> obj.get("ms").asLong
                obj.has("timestamp") -> obj.get("timestamp").asLong
                else -> 0L
            }
            FinishedArtwork(title = title, strokes = emptyList(), layerName = layerName, timestamp = timestamp, id = id)
        } catch (e: Exception) {
            val title = Regex(""""title"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.getOrNull(1) ?: fallbackId
            val id = Regex(""""id"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.getOrNull(1) ?: fallbackId
            val layerName = Regex(""""layerName"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.getOrNull(1) ?: "Finished Layer"
            val timestamp = Regex(""""timestamp"\s*:\s*(\d+)""").find(json)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
            FinishedArtwork(title = title, strokes = emptyList(), layerName = layerName, timestamp = timestamp, id = id)
        }
    }

    private fun jsonToFinishedArtwork(json: String): FinishedArtwork {
        val obj = JsonParser.parseString(json).asJsonObject
        return if (obj.has("s")) {
            FinishedArtwork(
                id = obj.get("id").asString,
                title = obj.get("t").asString,
                layerName = obj.get("n").asString,
                timestamp = obj.get("ms").asLong,
                strokes = obj.getAsJsonArray("s").map { jsonToCompactStroke(it.asJsonArray) }
            )
        } else {
            val strokesArr = obj.getAsJsonArray("strokes")
            val strokes = strokesArr.map { jsonToStroke(it.asJsonObject) }
            FinishedArtwork(
                id = obj.get("id").asString,
                title = obj.get("title").asString,
                layerName = obj.get("layerName").asString,
                timestamp = obj.get("timestamp").asLong,
                strokes = strokes
            )
        }
    }


    data class CloudArtworkFile(
        val id: String,
        val title: String,
        val json: String,
        val modifiedAtMs: Long
    )

    fun reloadAfterCloudRestore() {
        clearInMemoryIndexes()
        loadAllArtwork()
        bump()
    }

    fun localDraftCloudFiles(): List<CloudArtworkFile> {
        return try {
            getDraftsDir().listFiles()
                ?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
                ?.map { file ->
                    val json = file.readText()
                    val title = try {
                        val root = JsonParser.parseString(json).asJsonObject
                        if (root.has("t")) root.get("t").asString else file.nameWithoutExtension
                    } catch (_: Throwable) {
                        file.nameWithoutExtension
                    }
                    CloudArtworkFile(file.nameWithoutExtension, title, json, file.lastModified())
                }.orEmpty()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun localFinishedCloudFiles(): List<CloudArtworkFile> {
        return try {
            getFinishedDir().listFiles()
                ?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
                ?.mapNotNull { file ->
                    try {
                        val json = file.readText()
                        val meta = jsonToFinishedArtworkMetadata(json, file.nameWithoutExtension)
                        CloudArtworkFile(meta.id, meta.title, json, file.lastModified())
                    } catch (_: Throwable) {
                        null
                    }
                }.orEmpty()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun importCloudDraft(title: String, json: String, cloudModifiedAtMs: Long) {
        try {
            val safeTitle = title.sanitizeFilename()
            val file = File(getDraftsDir(), "$safeTitle.json")
            val shouldReplace = !file.exists() || cloudModifiedAtMs >= file.lastModified()
            if (!shouldReplace) return
            file.writeText(json)
            if (cloudModifiedAtMs > 0L) file.setLastModified(cloudModifiedAtMs)
            draftPreviewFile(title).delete()
            displayPreviewFile(title).delete()
            updatePreviewVersion(title)
            draftFileTitles.add(title)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun importCloudFinished(id: String, json: String, cloudModifiedAtMs: Long) {
        try {
            val safeId = id.sanitizeFilename()
            val file = File(getFinishedDir(), "$safeId.json")
            val shouldReplace = !file.exists() || cloudModifiedAtMs >= file.lastModified()
            if (!shouldReplace) return
            file.writeText(json)
            if (cloudModifiedAtMs > 0L) file.setLastModified(cloudModifiedAtMs)
            val title = runCatching { jsonToFinishedArtworkMetadata(json, id).title }.getOrDefault(id)
            finishedPreviewFile(id).delete()
            displayPreviewFile(title).delete()
            updatePreviewVersion(title)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun String.sanitizeFilename(): String {
        return this.replace(Regex("[/\\\\:*?\"<>|]"), "_")
    }
}