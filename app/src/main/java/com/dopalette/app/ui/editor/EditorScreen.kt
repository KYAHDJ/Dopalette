package com.dopalette.app.ui.editor

import android.content.ContentValues
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dopalette.app.R
import com.dopalette.app.data.ArtworkStore
import com.dopalette.app.data.CommunityRepository
import com.dopalette.app.data.GoogleAuthController
import com.dopalette.app.data.ArtworkCloudSync
import com.dopalette.app.data.ArtworkRenderer
import com.dopalette.app.data.AutoRegionDetector
import com.dopalette.app.data.SelectionArtworkAssets
import com.dopalette.app.data.AchievementStore
import com.dopalette.app.data.MonetizationStore
import com.dopalette.app.data.AdMobManager
import com.dopalette.app.data.BrushStyle
import com.dopalette.app.data.FinishedArtwork
import com.dopalette.app.data.PathActionType
import com.dopalette.app.data.SerializablePathAction
import com.dopalette.app.data.StrokeData
import com.dopalette.app.ui.components.ClearCanvasDialog
import com.dopalette.app.ui.components.FinishedArtworkDialog
import com.dopalette.app.ui.components.MakeNewDraftDialog
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private val DoseviaPinkPrimary = Color(0xFF2E6BFF)
private val DoseviaPinkDark = Color(0xFFE2E8F0)
private val DoseviaOrangeAccent = Color(0xFFF17878)
private val DoseviaCard = Color(0xFFFFFFFF)
private val DoseviaPanel = Color(0xFFFFFFFF)
private val DoseviaSoftPanel = Color(0xFFE9EEF6)
private val DoseviaText = Color(0xFF001233)
private val DoseviaMuted = Color(0xFF53657C)
private val DoseviaBorder = Color(0xFFC4CEDD)
private val DoseviaGreen = Color(0xFFE8CA4C)
private val DoseviaRed = Color(0xFFF17878)
private val DoseviaBackgroundBrush = Brush.linearGradient(
    listOf(
        Color(0xFFD8DCE4),
        Color(0xFFE7ECF3),
        Color(0xFFD0D6E1)
    )
)

private enum class AppMode {
    PAINT, ZOOM_PAN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    artworkTitle: String,
    artworkCategory: String = "",
    finishedArtworkId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val isEditingFinished = finishedArtworkId != null
    val finishedArtwork = if (isEditingFinished) ArtworkStore.getFinishedArtworkById(finishedArtworkId!!) else null

    val strokes = remember(artworkTitle, finishedArtworkId) {
        mutableStateListOf<StrokeData>().apply {
            addAll(finishedArtwork?.strokes ?: ArtworkStore.loadDraft(artworkTitle))
        }
    }
    val redoStack = remember(artworkTitle, finishedArtworkId) { mutableStateListOf<StrokeData>() }

    // Undo/redo is session-only. Loaded/restored V2 save data is the new baseline,
    // so after Back/Done and reopening, old actions cannot be undone/redone anymore.
    var undoBaselineSize by remember(artworkTitle, finishedArtworkId) { mutableIntStateOf(strokes.size) }

    var currentStroke by remember { mutableStateOf<StrokeData?>(null) }
    var isArtworkLoading by remember(artworkTitle, finishedArtworkId) { mutableStateOf(strokes.isNotEmpty()) }
    var redrawTick by remember { mutableIntStateOf(0) }
    var previousPoint by remember { mutableStateOf(Offset.Zero) }

    var selectedColor by remember { mutableStateOf(DoseviaPinkPrimary) }
    var brushSize by remember { mutableFloatStateOf(24f) }
    var activeBrushStyle by remember { mutableStateOf(BrushStyle.MARKER) }
    var isEraserMode by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf(AppMode.PAINT) }

    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    var showClearDialog by remember { mutableStateOf(false) }
    var showHelpGuide by remember { mutableStateOf(false) }
    var showFinishedDialog by remember { mutableStateOf(false) }
    var showAchievement by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var savingMessage by remember { mutableStateOf("SAVING") }
    var dirtyTick by remember(artworkTitle, finishedArtworkId) { mutableIntStateOf(0) }
    var hasEditorChanges by remember(artworkTitle, finishedArtworkId) { mutableStateOf(false) }
    var activeFinishedArtwork by remember { mutableStateOf<FinishedArtwork?>(null) }
    var communityShareMessage by remember { mutableStateOf<String?>(null) }
    var finishedCount by remember(artworkTitle) { mutableIntStateOf(ArtworkStore.finishedFor(artworkTitle).size) }
    var showDoneInterstitialPlaceholder by remember { mutableStateOf(false) }

    val completePalette = remember { createCompletePalette() }
    val userSavedRecentSwatches = remember {
        mutableStateListOf<Color>().apply {
            addAll(loadSavedEditorColors(context).ifEmpty { listOf(DoseviaPinkPrimary, Color(0xFFF17878), Color(0xFFE8CA4C)) })
        }
    }

    LaunchedEffect(Unit, MonetizationStore.updateTick.intValue) {
        // Keep the Done interstitial ready while the user is coloring.
        AdMobManager.preloadInterstitial(context, force = true)
    }

    val baseBitmap = remember(artworkTitle) { SelectionArtworkAssets.loadBaseBitmap(context, artworkTitle) }
    val baseImage = remember(baseBitmap) { baseBitmap?.asImageBitmap() }
    var solidOutlineBitmap by remember(artworkTitle) { mutableStateOf<Bitmap?>(null) }
    val regionMaskCache = remember(artworkTitle) { mutableStateMapOf<String, Bitmap>() }
    var preparedRegionSource by remember(artworkTitle) { mutableStateOf<AutoRegionDetector.PreparedLineArt?>(null) }
    var canvasPixelBounds by remember { mutableStateOf(IntSize.Zero) }
    var editorViewportBounds by remember { mutableStateOf(IntSize.Zero) }

    fun brushMaskKey(width: Int, height: Int, x: Float, y: Float): String {
        return "${width}x${height}_${x.roundToInt()}_${y.roundToInt()}"
    }

    fun limitedCanvasOffset(candidate: Offset, targetScale: Float = scale): Offset {
        val canvasWidth = canvasPixelBounds.width.toFloat()
        val canvasHeight = canvasPixelBounds.height.toFloat()
        val viewportWidth = editorViewportBounds.width.toFloat()
        val viewportHeight = editorViewportBounds.height.toFloat()
        if (canvasWidth <= 1f || canvasHeight <= 1f || viewportWidth <= 1f || viewportHeight <= 1f) {
            return Offset.Zero
        }

        val scaledWidth = canvasWidth * targetScale
        val scaledHeight = canvasHeight * targetScale
        val horizontalLimit = ((scaledWidth - viewportWidth) / 2f).coerceAtLeast(0f)
        val verticalLimit = ((scaledHeight - viewportHeight) / 2f).coerceAtLeast(0f)

        return Offset(
            x = candidate.x.coerceIn(-horizontalLimit, horizontalLimit),
            y = candidate.y.coerceIn(-verticalLimit, verticalLimit)
        )
    }

    fun applyCanvasTransform(zoomChange: Float, panChange: Offset, centroid: Offset) {
        val minimumScale = 1f
        val maximumScale = 8f
        val oldScale = scale
        val newScale = (oldScale * zoomChange).coerceIn(minimumScale, maximumScale)
        val scaleRatio = if (oldScale > 0f) newScale / oldScale else 1f
        val viewportCenter = Offset(
            editorViewportBounds.width / 2f,
            editorViewportBounds.height / 2f
        )
        val centroidFromCenter = centroid - viewportCenter

        val zoomAnchoredOffset = (offset * scaleRatio) + (centroidFromCenter * (1f - scaleRatio))
        val zoomResponsivePan = panChange * oldScale.coerceIn(1f, 3.5f)

        scale = newScale
        offset = limitedCanvasOffset(zoomAnchoredOffset + zoomResponsivePan, newScale)
    }

    LaunchedEffect(baseBitmap, canvasPixelBounds) {
        val width = canvasPixelBounds.width
        val height = canvasPixelBounds.height
        if (width > 1 && height > 1) {
            val preparedAssets = withContext(Dispatchers.Default) {
                // Fruits pipeline: use the original smooth base image as the final
                // line-art layer. Only prepare region masks here; do not generate a
                // hard binary outline overlay because it creates jagged/white rims.
                AutoRegionDetector.prepareLineArt(baseBitmap, width, height)
            }
            solidOutlineBitmap = null
            preparedRegionSource = preparedAssets

            // Warm the region-mask engine before the user touches the canvas.
            // The old first real stroke could stutter because mask allocation/JIT happened on first paint.
            val warmupX = width / 2f
            val warmupY = height / 2f
            val warmupKey = brushMaskKey(width, height, warmupX, warmupY)
            if (!regionMaskCache.containsKey(warmupKey)) {
                val warmupMask = withContext(Dispatchers.Default) {
                    AutoRegionDetector.createMaskForTap(preparedAssets, warmupX, warmupY)?.bitmap
                }
                if (warmupMask != null) {
                    regionMaskCache[warmupKey] = warmupMask
                }
            }
        }
    }

    LaunchedEffect(baseBitmap, canvasPixelBounds, strokes.size) {
        val width = canvasPixelBounds.width
        val height = canvasPixelBounds.height
        val snapshot = strokes.toList()
        if (snapshot.isEmpty()) {
            isArtworkLoading = false
        }
        if (width > 1 && height > 1 && snapshot.isNotEmpty()) {
            val prepared = withContext(Dispatchers.Default) {
                AutoRegionDetector.prepareLineArt(baseBitmap, width, height)
            }
            preparedRegionSource = prepared
            val builtMasks = withContext(Dispatchers.Default) {
                val map = mutableMapOf<String, Bitmap>()
                snapshot.forEach { stroke ->
                    if (stroke.regionSeedX >= 0f && stroke.regionSeedY >= 0f) {
                        val sourceWidth = stroke.canvasWidth.takeIf { it > 0f } ?: width.toFloat()
                        val sourceHeight = stroke.canvasHeight.takeIf { it > 0f } ?: height.toFloat()
                        val seedX = stroke.regionSeedX * (width.toFloat() / sourceWidth)
                        val seedY = stroke.regionSeedY * (height.toFloat() / sourceHeight)
                        val key = brushMaskKey(width, height, seedX, seedY)
                        if (!regionMaskCache.containsKey(key)) {
                            AutoRegionDetector.createMaskForTap(prepared, seedX, seedY)?.bitmap?.let { map[key] = it }
                        }
                    }
                }
                map
            }
            if (builtMasks.isNotEmpty()) {
                regionMaskCache.putAll(builtMasks)
                redrawTick++
            }
            isArtworkLoading = false
        }
    }

    fun cachedRegionMask(stroke: StrokeData, width: Int, height: Int): Bitmap? {
        if (stroke.regionSeedX < 0f || stroke.regionSeedY < 0f || width <= 1 || height <= 1) return null
        val sourceWidth = stroke.canvasWidth.takeIf { it > 0f } ?: width.toFloat()
        val sourceHeight = stroke.canvasHeight.takeIf { it > 0f } ?: height.toFloat()
        val seedX = stroke.regionSeedX * (width.toFloat() / sourceWidth)
        val seedY = stroke.regionSeedY * (height.toFloat() / sourceHeight)
        val key = brushMaskKey(width, height, seedX, seedY)
        return regionMaskCache[key]?.takeIf { it.width == width && it.height == height }
    }
    val artworkAspectRatio = remember(baseBitmap) {
        val safeWidth = baseBitmap?.width?.takeIf { it > 0 } ?: 1080
        val safeHeight = baseBitmap?.height?.takeIf { it > 0 } ?: 1350
        safeWidth.toFloat() / safeHeight.toFloat()
    }
    val activeHex = remember(selectedColor) { selectedColor.toHexString() }


    fun markArtworkDirty() {
        hasEditorChanges = true
        dirtyTick++
    }

    fun persistSnapshot(snapshot: List<StrokeData>, renderPreviewNow: Boolean = false) {
        // Important for V2 testing: an empty visible canvas is still a real state.
        // Do not skip saving just because the user undid/cleared everything.
        if (snapshot.isEmpty()) {
            if (isEditingFinished && finishedArtworkId != null) {
                if (renderPreviewNow) {
                    ArtworkStore.updateFinishedArtworkWithPreviewNow(finishedArtworkId, emptyList())
                } else {
                    ArtworkStore.updateFinishedArtworkDataOnly(finishedArtworkId, emptyList())
                }
            } else {
                ArtworkStore.clearDraft(artworkTitle)
            }
            return
        }
        if (isEditingFinished && finishedArtworkId != null) {
            if (renderPreviewNow) {
                ArtworkStore.updateFinishedArtworkWithPreviewNow(finishedArtworkId, snapshot)
            } else {
                ArtworkStore.updateFinishedArtworkDataOnly(finishedArtworkId, snapshot)
            }
        } else {
            if (renderPreviewNow) {
                ArtworkStore.saveDraftWithPreviewNow(artworkTitle, snapshot)
            } else {
                ArtworkStore.saveDraftDataOnly(artworkTitle, snapshot)
            }
        }
    }

    fun persistCurrentWork(renderPreviewNow: Boolean = false) {
        persistSnapshot(strokes.toList(), renderPreviewNow)
    }

    fun saveDraftAndReturn() {
        if (isSaving) return
        isSaving = true
        savingMessage = "SAVING"
        val snapshot = strokes.toList()
        redoStack.clear()
        undoBaselineSize = snapshot.size
        scope.launch {
            val shouldSync = hasEditorChanges
            withContext(Dispatchers.IO) {
                persistSnapshot(snapshot, renderPreviewNow = true)
                if (shouldSync) {
                    if (isEditingFinished && finishedArtworkId != null) {
                        ArtworkCloudSync.syncFinishedOnly(finishedArtworkId)
                    } else {
                        ArtworkCloudSync.syncDraftOnly(artworkTitle)
                    }
                }
            }
            hasEditorChanges = false
            isSaving = false
            onBack()
        }
    }

    BackHandler { saveDraftAndReturn() }

    LaunchedEffect(dirtyTick, artworkTitle, finishedArtworkId) {
        if (dirtyTick <= 0) return@LaunchedEffect

        // Fast safety save: stores editable data only. No heavy thumbnail render while painting.
        delay(1200L)
        val draftSnapshot = strokes.toList()
        withContext(Dispatchers.IO) {
            persistSnapshot(draftSnapshot, renderPreviewNow = false)
        }

        // Visual preview save: only after the user pauses. This job is cancelled automatically
        // when dirtyTick changes, so old white/stale previews cannot overwrite newer work.
        delay(1800L)
        val previewSnapshot = strokes.toList()
        withContext(Dispatchers.IO) {
            persistSnapshot(previewSnapshot, renderPreviewNow = true)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, artworkTitle, finishedArtworkId) {
        val observer = LifecycleEventObserver { _, event ->
            if ((event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) && strokes.isNotEmpty()) {
                val snapshot = strokes.toList()
                scope.launch {
                    withContext(Dispatchers.IO) {
                        persistSnapshot(snapshot, renderPreviewNow = true)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (strokes.isNotEmpty()) {
                persistCurrentWork(renderPreviewNow = false)
            }
        }
    }

    BottomSheetScaffold(
        sheetPeekHeight = 176.dp,
        sheetContainerColor = DoseviaCard.copy(alpha = 0.96f),
        sheetContentColor = DoseviaText,
        sheetShadowElevation = 12.dp,
        sheetDragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .background(DoseviaBorder, RoundedCornerShape(2.dp))
            )
        },
        sheetContent = {
            StudioPanel(
                activeColor = selectedColor,
                activeHex = activeHex,
                colors = completePalette,
                recentColors = userSavedRecentSwatches,
                finishedCount = finishedCount,
                activeBrushStyle = activeBrushStyle,
                brushSize = brushSize,
                isEraserMode = isEraserMode,
                currentMode = currentMode,
                canUndo = strokes.size > undoBaselineSize,
                canRedo = redoStack.isNotEmpty(),
                onHelp = { showHelpGuide = true },
                onColorSelected = {
                    selectedColor = it
                    isEraserMode = false
                },
                onSaveCustomColor = {
                    selectedColor = it
                    isEraserMode = false
                    if (it !in userSavedRecentSwatches) {
                        if (userSavedRecentSwatches.size >= 12) userSavedRecentSwatches.removeLast()
                        userSavedRecentSwatches.add(0, it)
                        saveEditorColors(context, userSavedRecentSwatches)
                    }
                },
                onBrushSelected = {
                    activeBrushStyle = it
                    isEraserMode = false
                },
                onBrushSizeChanged = { brushSize = it },
                onEraserChanged = { isEraserMode = it },
                onModeChanged = { currentMode = it },
                onUndo = {
                    if (strokes.size > undoBaselineSize) {
                        redoStack.add(strokes.removeAt(strokes.lastIndex))
                        AchievementStore.recordUndo()
                        markArtworkDirty()
                        redrawTick++
                    }
                },
                onRedo = {
                    if (redoStack.isNotEmpty()) {
                        strokes.add(redoStack.removeAt(redoStack.lastIndex))
                        markArtworkDirty()
                        redrawTick++
                    }
                },
                onClear = { showClearDialog = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE9EDF5))
                .padding(innerPadding)
                .onGloballyPositioned { coordinates ->
                    editorViewportBounds = coordinates.size
                    offset = limitedCanvasOffset(offset, scale)
                }
                .pointerInput(artworkTitle) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.filter { pointerChange -> pointerChange.pressed }
                            if (pressed.isEmpty()) break

                            if (pressed.size >= 2) {
                                val centroid = Offset(
                                    x = pressed.map { pointerChange -> pointerChange.position.x }.average().toFloat(),
                                    y = pressed.map { pointerChange -> pointerChange.position.y }.average().toFloat()
                                )
                                applyCanvasTransform(
                                    zoomChange = event.calculateZoom(),
                                    panChange = event.calculatePan(),
                                    centroid = centroid
                                )
                                event.changes.forEach { pointerChange -> pointerChange.consume() }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val strokeRenderVersion = redrawTick
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(artworkAspectRatio)
                        .onGloballyPositioned { coordinates -> canvasPixelBounds = coordinates.size }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .pointerInput(artworkTitle, isEditingFinished, selectedColor, brushSize, isEraserMode, activeBrushStyle, preparedRegionSource) {
                            awaitEachGesture {
                                val firstDown = awaitFirstDown(requireUnconsumed = false)
                                var drewDuringGesture = false

                                fun beginStroke(position: Offset): Boolean {
                                    if (canvasPixelBounds.width <= 1 || canvasPixelBounds.height <= 1) return false
                                    if (baseBitmap == null || baseBitmap.isRecycled) return false
                                    if (!SelectionArtworkAssets.hasBaseArtworkForTitle(context, artworkTitle)) return false
                                    SelectionArtworkAssets.loadBaseBitmap(context, artworkTitle)?.takeIf { !it.isRecycled } ?: return false

                                    val initialMask = try {
                                        AutoRegionDetector.createMaskForTap(
                                            preparedRegionSource?.takeIf { source ->
                                                source.width == canvasPixelBounds.width && source.height == canvasPixelBounds.height
                                            },
                                            position.x,
                                            position.y
                                        ) ?: AutoRegionDetector.createMaskForTap(
                                            lineArt = baseBitmap,
                                            tapX = position.x,
                                            tapY = position.y,
                                            canvasWidth = canvasPixelBounds.width,
                                            canvasHeight = canvasPixelBounds.height
                                        )
                                    } catch (t: Throwable) {
                                        t.printStackTrace()
                                        null
                                    } ?: return false

                                    redoStack.clear()
                                    val w = canvasPixelBounds.width.toFloat().coerceAtLeast(1f)
                                    val h = canvasPixelBounds.height.toFloat().coerceAtLeast(1f)
                                    val pathWidth = if (activeBrushStyle == BrushStyle.REGION_FILL) 1f else brushSize
                                    val pathColor = if (isEraserMode) Color.Transparent else selectedColor
                                    val actions = if (activeBrushStyle == BrushStyle.REGION_FILL) {
                                        mutableListOf(
                                            SerializablePathAction(PathActionType.MOVE_TO, 0f, 0f),
                                            SerializablePathAction(PathActionType.LINE_TO, w, 0f),
                                            SerializablePathAction(PathActionType.LINE_TO, w, h),
                                            SerializablePathAction(PathActionType.LINE_TO, 0f, h),
                                            SerializablePathAction(PathActionType.LINE_TO, 0f, 0f)
                                        )
                                    } else {
                                        mutableListOf(SerializablePathAction(PathActionType.MOVE_TO, position.x, position.y))
                                    }
                                    val path = Path().apply {
                                        if (activeBrushStyle == BrushStyle.REGION_FILL) {
                                            moveTo(0f, 0f)
                                            lineTo(w, 0f)
                                            lineTo(w, h)
                                            lineTo(0f, h)
                                            lineTo(0f, 0f)
                                        } else {
                                            moveTo(position.x, position.y)
                                        }
                                    }
                                    val stroke = StrokeData(
                                        path = path,
                                        color = pathColor,
                                        width = pathWidth,
                                        style = activeBrushStyle,
                                        canvasWidth = w,
                                        canvasHeight = h,
                                        regionSeedX = position.x,
                                        regionSeedY = position.y,
                                        serializableActions = actions
                                    )
                                    val cacheKey = brushMaskKey(canvasPixelBounds.width, canvasPixelBounds.height, position.x, position.y)
                                    regionMaskCache[cacheKey] = initialMask.bitmap
                                    try {
                                        if (activeBrushStyle == BrushStyle.REGION_FILL) {
                                            AchievementStore.recordFill(artworkTitle)
                                        } else {
                                            AchievementStore.recordBrushStroke(artworkTitle)
                                        }
                                    } catch (t: Throwable) {
                                        t.printStackTrace()
                                    }
                                    currentStroke = if (activeBrushStyle == BrushStyle.REGION_FILL) null else stroke
                                    previousPoint = position
                                    strokes.add(stroke)
                                    drewDuringGesture = true
                                    redrawTick++
                                    return true
                                }

                                fun continueStroke(position: Offset) {
                                    val mid = Offset(
                                        (previousPoint.x + position.x) / 2f,
                                        (previousPoint.y + position.y) / 2f
                                    )
                                    currentStroke?.path?.quadraticBezierTo(
                                        previousPoint.x,
                                        previousPoint.y,
                                        mid.x,
                                        mid.y
                                    )
                                    currentStroke?.serializableActions?.add(
                                        SerializablePathAction(
                                            type = PathActionType.QUAD_TO,
                                            x = previousPoint.x,
                                            y = previousPoint.y,
                                            x2 = mid.x,
                                            y2 = mid.y
                                        )
                                    )
                                    previousPoint = position
                                    redrawTick++
                                }

                                var transformMode = false
                                var hasStartedStroke = false
                                var firstPaintPosition = firstDown.position
                                var lastPaintPosition = firstDown.position

                                // Do not start painting on the first finger-down.
                                // Waiting for real one-finger movement keeps two-finger zoom/pan smooth
                                // and avoids treating a quick two-finger gesture as paint.

                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    val pressed = event.changes.filter { pointerChange -> pointerChange.pressed }
                                    if (pressed.isEmpty()) break

                                    if (pressed.size >= 2) {
                                        transformMode = true
                                        hasStartedStroke = false
                                        currentStroke = null
                                        event.changes.forEach { pointerChange -> pointerChange.consume() }
                                    } else {
                                        val change = pressed.first()

                                        if (!transformMode && currentMode == AppMode.PAINT) {
                                            if (!hasStartedStroke) {
                                                if (firstPaintPosition == Offset.Zero) {
                                                    firstPaintPosition = change.position
                                                    lastPaintPosition = change.position
                                                }

                                                val movedFromStart = (change.position - firstPaintPosition).getDistance()
                                                if (movedFromStart > 3.5f) {
                                                    if (beginStroke(firstPaintPosition)) {
                                                        hasStartedStroke = true
                                                        previousPoint = firstPaintPosition
                                                        continueStroke(change.position)
                                                    }
                                                }
                                            } else {
                                                val movedFromLast = (change.position - lastPaintPosition).getDistance()
                                                if (movedFromLast > 0.85f) {
                                                    continueStroke(change.position)
                                                }
                                            }

                                            lastPaintPosition = change.position
                                        }

                                        change.consume()
                                    }
                                }

                                if (!transformMode && !hasStartedStroke && currentMode == AppMode.PAINT && activeBrushStyle == BrushStyle.REGION_FILL) {
                                    hasStartedStroke = beginStroke(firstPaintPosition)
                                }

                                if (currentStroke != null || drewDuringGesture) {
                                    currentStroke = null
                                    if (hasStartedStroke || drewDuringGesture) markArtworkDirty()
                                    redrawTick++
                                }
                            }
                        }
                ) {
                    if (strokeRenderVersion < 0) return@Canvas
                    drawRect(Color.White)
                    val nativeCanvas = drawContext.canvas.nativeCanvas

                    val widthPx = size.width.roundToInt()
                    val heightPx = size.height.roundToInt()

                    // Keep all user color on its own transparent layer. Eraser strokes use
                    // BlendMode.Clear on this layer only, so they remove brush/fill marks
                    // without turning into white paint and without damaging the artwork base,
                    // outline, or background.
                    val userColorLayer = nativeCanvas.saveLayer(null, null)
                    strokes.forEach { stroke ->
                        val isClearStroke = stroke.color == Color.Transparent
                        if (isClearStroke) {
                            if (stroke.style == BrushStyle.REGION_FILL) {
                                cachedRegionMask(stroke = stroke, width = widthPx, height = heightPx)?.takeIf { !it.isRecycled }?.let { regionMask ->
                                    drawImage(
                                        image = regionMask.asImageBitmap(),
                                        dstSize = IntSize(widthPx, heightPx),
                                        blendMode = BlendMode.Clear
                                    )
                                }
                            } else {
                                drawStroke(stroke = stroke)
                            }
                        } else {
                            val regionMask = cachedRegionMask(
                                stroke = stroke,
                                width = widthPx,
                                height = heightPx
                            )
                            if (regionMask != null && !regionMask.isRecycled) {
                                val strokeCheckpoint = nativeCanvas.saveLayer(null, null)
                                drawStroke(stroke = stroke)
                                drawImage(
                                    image = regionMask.asImageBitmap(),
                                    dstSize = IntSize(widthPx, heightPx),
                                    blendMode = BlendMode.DstIn
                                )
                                nativeCanvas.restoreToCount(strokeCheckpoint)
                            } else {
                                // Never draw color strokes without their strict mask. This prevents painting over the
                                // background while masks are preparing or rebuilding.
                            }
                        }
                    }
                    nativeCanvas.restoreToCount(userColorLayer)

                    // Final top layer: original smooth artwork, same as Fruits.
                    // White pixels multiply away over the color layer; black and gray
                    // anti-aliased line pixels stay smooth on top.
                    baseImage?.let { image ->
                        drawImage(
                            image = image,
                            dstSize = IntSize(widthPx, heightPx),
                            blendMode = BlendMode.Multiply,
                                    )
                    }
                }

                AnimatedVisibility(
                    visible = isArtworkLoading,
                    enter = fadeIn(animationSpec = tween(120)),
                    exit = fadeOut(animationSpec = tween(180)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(artworkAspectRatio)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    EditorLoadingWaveIndicator(modifier = Modifier.fillMaxSize())
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 14.dp, end = 14.dp),
                shape = RoundedCornerShape(50.dp),
                color = DoseviaCard.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockButton("Back") { saveDraftAndReturn() }
                    DockButton("Center Canvas") {
                        scale = 1f
                        offset = Offset.Zero
                    }
                    Button(
                        onClick = {
                            if (isEditingFinished || ArtworkStore.canAddFinished(artworkTitle)) {
                                showFinishedDialog = true
                            }
                        },
                        enabled = isEditingFinished || ArtworkStore.canAddFinished(artworkTitle),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary)
                    ) {
                        Text("DONE", color = Color(0xFF070B13), fontWeight = FontWeight.Bold)
                    }
                }
            }

        }
    }

    if (showHelpGuide) {
        HelpGuideDialog(
            onClose = { showHelpGuide = false }
        )
    }

    if (showClearDialog) {
        ClearCanvasDialog(
            onCancel = { showClearDialog = false },
            onClear = {
                strokes.clear()
                redoStack.clear()
                AchievementStore.recordClear()
                if (isEditingFinished && finishedArtworkId != null) {
                    ArtworkStore.updateFinishedArtwork(finishedArtworkId, strokes)
                } else {
                    ArtworkStore.clearDraft(artworkTitle)
                }
                scale = 1f
                offset = Offset.Zero
                markArtworkDirty()
                redrawTick++
                showClearDialog = false
            }
        )
    }

    if (showFinishedDialog) {
        FinishedArtworkDialog(
            onContinueEditing = { showFinishedDialog = false },
            onFinishArtwork = {
                if (!isSaving) {
                    isSaving = true
                    savingMessage = "SAVING"
                    val snapshot = strokes.toList()
                    scope.launch {
                        redoStack.clear()
                        undoBaselineSize = snapshot.size
                        val shouldSync = hasEditorChanges || !isEditingFinished
                        val savedArtwork = withContext(Dispatchers.IO) {
                            val finished = if (isEditingFinished && finishedArtworkId != null) {
                                ArtworkStore.updateFinishedArtworkWithPreviewNow(finishedArtworkId, snapshot)
                            } else {
                                ArtworkStore.addFinishedWithPreviewNow(artworkTitle, snapshot)
                            }
                            if (!isEditingFinished && finished != null) ArtworkStore.clearDraft(artworkTitle)
                            if (shouldSync && finished != null) ArtworkCloudSync.syncFinishedOnly(finished.id)
                            finished
                        }
                        hasEditorChanges = false
                        if (savedArtwork != null) {
                            val catalog = withContext(Dispatchers.IO) { SelectionArtworkAssets.loadCatalog(context) }
                            val resolvedCategory = artworkCategory.ifBlank { catalog.firstOrNull { it.title == artworkTitle }?.category.orEmpty() }
                            val categoryItems = catalog.filter { it.category.equals(resolvedCategory, ignoreCase = true) }
                            val categoryFinished = categoryItems.count { ArtworkStore.hasFinished(it.title) }
                            AchievementStore.recordFinishedArtwork(
                                artworkTitle = artworkTitle,
                                category = resolvedCategory,
                                categoryFinished = categoryFinished,
                                categoryTotal = categoryItems.size
                            )
                            AchievementStore.evaluateAllProgress(context)
                        }
                        activeFinishedArtwork = savedArtwork
                        finishedCount = ArtworkStore.finishedFor(artworkTitle).size
                        showFinishedDialog = false
                        showAchievement = savedArtwork != null
                        if (savedArtwork != null && MonetizationStore.shouldShowInterstitialAfterDone()) {
                            showDoneInterstitialPlaceholder = true
                        }
                        isSaving = false
                    }
                }
            }
        )
    }

    if (isSaving && !showAchievement) {
        SavingProgressOverlay(message = savingMessage)
    }

    if (showAchievement) {
        AchievementReveal(
            artwork = activeFinishedArtwork,
            isSaving = isSaving,
            canMakeNew = !isEditingFinished && ArtworkStore.canAddFinished(artworkTitle),
            onDismiss = { if (!isSaving) showAchievement = false },
            onShareCommunity = {
                activeFinishedArtwork?.let { artwork ->
                    scope.launch {
                        isSaving = true
                        val resultMessage = withContext(Dispatchers.IO) {
                            if (GoogleAuthController.refreshCurrentAccount(context) == null) {
                                "Sign in to share your artwork."
                            } else {
                                CommunityRepository.shareFinishedArtwork(context, artwork, null).message
                            }
                        }
                        isSaving = false
                        communityShareMessage = resultMessage
                    }
                }
            },
            onDownload = { renderedBitmap ->
                activeFinishedArtwork?.let { artwork ->
                    isSaving = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            if (renderedBitmap != null) {
                                saveArtworkBitmapToGallery(
                                    context = context,
                                    title = artwork.title,
                                    layerName = artwork.layerName,
                                    bitmap = renderedBitmap
                                )
                            } else {
                                saveArtworkToGallery(
                                    context = context,
                                    title = artwork.title,
                                    layerName = artwork.layerName,
                                    strokes = artwork.strokes
                                )
                            }
                        }
                        AchievementStore.recordDownload()
                        isSaving = false
                        showAchievement = false
                    }
                }
            },
            onMakeNew = {
                strokes.clear()
                redoStack.clear()
                undoBaselineSize = 0
                AchievementStore.recordClear()
                scale = 1f
                offset = Offset.Zero
                markArtworkDirty()
                redrawTick++
                if (!isEditingFinished) ArtworkStore.clearDraft(artworkTitle)
                showAchievement = false
            }
        )
    }

    if (showDoneInterstitialPlaceholder) {
        LaunchedEffect(showDoneInterstitialPlaceholder) {
            val hostActivity = activity
            if (hostActivity == null) {
                showDoneInterstitialPlaceholder = false
            } else {
                AdMobManager.showInterstitialAfterDone(hostActivity) {
                    showDoneInterstitialPlaceholder = false
                }
            }
        }
    }

    communityShareMessage?.let { message ->
        EditorCommunityMessageDialog(
            message = message,
            onDismiss = { communityShareMessage = null }
        )
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun EditorCommunityMessageDialog(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = RoundedCornerShape(30.dp),
                color = DoseviaCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
                shadowElevation = 18.dp
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(42.dp).background(DoseviaPinkPrimary, CircleShape), contentAlignment = Alignment.Center) {
                            Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Community", color = DoseviaText, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            Text("Your artwork is ready to show.", color = DoseviaMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = DoseviaSoftPanel, modifier = Modifier.fillMaxWidth()) {
                        Text(message.cleanCommunityMessage(), modifier = Modifier.padding(16.dp), color = DoseviaText, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)) {
                        Text("Got it", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

private fun String.cleanCommunityMessage(): String {
    return replace("Firebase", "DoPalette")
        .replace("firebase", "DoPalette")
        .replace("cache", "saved copy")
        .replace("sync", "update")
        .replace("recipe", "artwork")
        .replace("Success", "Done")
}

@Composable
private fun SavingProgressOverlay(message: String = "SAVING") {
    EditorOperationWaveOverlay(word = operationWordFor(message), modifier = Modifier.fillMaxSize())
}

private fun operationWordFor(message: String): String {
    val upper = message.uppercase()
    return when {
        "SIGN" in upper -> "SIGNING OUT"
        "DELETE" in upper || "DELET" in upper -> "DELETING"
        "SYNC" in upper -> "SYNCING"
        "DOWNLOAD" in upper -> "DOWNLOADING"
        "LOAD" in upper -> "LOADING"
        else -> "SAVING"
    }
}

@Composable
private fun EditorOperationWaveOverlay(word: String, modifier: Modifier = Modifier) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        EditorOperationWaveOverlayContent(
            word = word,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun EditorOperationWaveOverlayContent(word: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "editor-operation-wave")
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE605070D)),
        contentAlignment = Alignment.Center
    ) {
        val panelWidth = (maxWidth * 0.78f).coerceAtMost(330.dp)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.width(panelWidth),
                shape = RoundedCornerShape(26.dp),
                color = DoseviaCard.copy(alpha = 0.98f),
                tonalElevation = 12.dp
            ) {
                Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                word.forEachIndexed { index, char ->
                    val y by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = -9f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 520, delayMillis = index * 45),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "editor-operation-wave-char-$index"
                    )
                    Text(
                        text = char.toString(),
                        color = DoseviaPinkPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.graphicsLayer { translationY = y }
                    )
                    if (char != ' ') Spacer(Modifier.width(3.dp))
                    else Spacer(Modifier.width(8.dp))
                }
                }
            }
        }
    }
}

@Composable
private fun StudioPanel(
    activeColor: Color,
    activeHex: String,
    colors: List<Color>,
    recentColors: List<Color>,
    finishedCount: Int,
    activeBrushStyle: BrushStyle,
    brushSize: Float,
    isEraserMode: Boolean,
    currentMode: AppMode,
    canUndo: Boolean,
    canRedo: Boolean,
    onHelp: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onSaveCustomColor: (Color) -> Unit,
    onBrushSelected: (BrushStyle) -> Unit,
    onBrushSizeChanged: (Float) -> Unit,
    onEraserChanged: (Boolean) -> Unit,
    onModeChanged: (AppMode) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(activeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = activeHex,
                color = Color(0xFFEFF4FA),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorActionButton(
                text = "Undo",
                enabled = canUndo,
                containerColor = DoseviaPinkPrimary.copy(alpha = 0.20f),
                contentColor = DoseviaText,
                modifier = Modifier.weight(1f),
                onClick = onUndo
            )
            EditorActionButton(
                text = "Redo",
                enabled = canRedo,
                containerColor = DoseviaPinkPrimary.copy(alpha = 0.20f),
                contentColor = DoseviaText,
                modifier = Modifier.weight(1f),
                onClick = onRedo
            )
            EditorActionButton(
                text = "Clear",
                enabled = true,
                containerColor = DoseviaRed.copy(alpha = 0.20f),
                contentColor = DoseviaText,
                modifier = Modifier.weight(1f),
                onClick = onClear
            )
        }
        Button(
            onClick = onHelp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel)
        ) {
            Text(
                text = "Help & Guide",
                color = DoseviaText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        if (finishedCount > 0) {
            Text(
                text = "Finished Layer: $finishedCount saved",
                color = DoseviaGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
        Text(
            text = "BRUSH SELECTOR",
            color = DoseviaMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BrushButton("Marker", BrushStyle.MARKER, activeBrushStyle, onBrushSelected)
            BrushButton("Fill Bucket", BrushStyle.REGION_FILL, activeBrushStyle, onBrushSelected)
            BrushButton("Neon Glow", BrushStyle.NEON_GLOW, activeBrushStyle, onBrushSelected)
            BrushButton("Airbrush", BrushStyle.AIRBRUSH, activeBrushStyle, onBrushSelected)
            BrushButton("Watercolor", BrushStyle.WATERCOLOR, activeBrushStyle, onBrushSelected)
            BrushButton("Chisel", BrushStyle.CHISEL, activeBrushStyle, onBrushSelected)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SIZE", color = DoseviaMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = brushSize,
                onValueChange = onBrushSizeChanged,
                valueRange = 4f..80f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = DoseviaPinkPrimary,
                    activeTrackColor = DoseviaPinkPrimary
                )
            )
            Text(brushSize.roundToInt().toString(), color = DoseviaText, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEraserChanged(!isEraserMode) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Eraser Mode", color = DoseviaText, fontWeight = FontWeight.SemiBold)
            Checkbox(
                checked = isEraserMode,
                onCheckedChange = onEraserChanged,
                colors = CheckboxDefaults.colors(checkedColor = DoseviaPinkPrimary)
            )
        }
        if (recentColors.isNotEmpty()) {
            Text(
                text = "SAVED COLORS",
                color = DoseviaMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recentColors.distinct().forEach { color ->
                    ColorChip(
                        color = color,
                        selected = color == activeColor && !isEraserMode,
                        onClick = { onColorSelected(color) },
                        size = 38
                    )
                }
            }
        }


        ColorWheelPanel(
            activeColor = activeColor,
            onLiveColorChanged = { mixed -> onColorSelected(mixed) },
            onSaveColor = { mixed -> onSaveCustomColor(mixed) }
        )

        Text(
            text = "COMPLETE COLOR PALETTE",
            color = DoseviaMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val originalRows = remember { createColorFamilyRows() }
            val maxFamilyCount = originalRows.maxOfOrNull { it.size } ?: 10
            val gap = when {
                maxWidth >= 700.dp -> 12.dp
                maxWidth >= 430.dp -> 8.dp
                else -> 4.dp
            }
            val rawChipSize = (maxWidth.value - (gap.value * (maxFamilyCount - 1))) / maxFamilyCount.toFloat()
            val chipSize = rawChipSize
                .coerceIn(20f, if (maxWidth >= 700.dp) 46f else 40f)
                .roundToInt()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (maxWidth >= 460.dp) 12.dp else 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                originalRows.forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowColors.forEach { color ->
                            ColorChip(
                                color = color,
                                selected = color == activeColor && !isEraserMode,
                                onClick = { onColorSelected(color) },
                                size = chipSize
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ColorWheelPanel(
    activeColor: Color,
    onLiveColorChanged: (Color) -> Unit,
    onSaveColor: (Color) -> Unit
) {
    var hue by remember(activeColor) { mutableFloatStateOf(colorHue(activeColor)) }
    var saturation by remember(activeColor) { mutableFloatStateOf(colorSaturation(activeColor)) }
    var value by remember(activeColor) { mutableFloatStateOf(colorValue(activeColor)) }
    val picked = Color.hsv(hue, saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))

    fun updateFromWheel(offset: Offset, size: Float) {
        val center = Offset(size / 2f, size / 2f)
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        val radius = size / 2f
        val distance = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
        saturation = (distance / radius).coerceIn(0f, 1f)
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        hue = ((angle + 360f) % 360f)
        onLiveColorChanged(Color.hsv(hue, saturation, value))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = DoseviaSoftPanel,
        border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(picked, RoundedCornerShape(16.dp))
                        .border(1.dp, DoseviaBorder, RoundedCornerShape(16.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("COLOR WHEEL", color = DoseviaMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(picked.toHexString(), color = DoseviaText, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("Drag to preview live. Add only when you want it saved.", color = DoseviaMuted, fontSize = 11.sp)
                }
                Button(
                    onClick = { onSaveColor(picked) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)
                ) { Text("Add to Palette", fontWeight = FontWeight.Bold) }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val wheelSize = when {
                    maxWidth >= 700.dp -> 220.dp
                    maxWidth >= 420.dp -> 190.dp
                    else -> 168.dp
                }
                Box(modifier = Modifier.size(wheelSize), contentAlignment = Alignment.Center) {
                    val wheelColors = remember {
                        listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue,
                            Color.Magenta, Color.Red
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .pointerInput(value) {
                                detectDragGestures(
                                    onDragStart = { offset -> updateFromWheel(offset, size.width.toFloat()) },
                                    onDrag = { change, _ -> updateFromWheel(change.position, size.width.toFloat()) }
                                )
                            }
                    ) {
                        val r = size.minDimension / 2f
                        drawCircle(brush = Brush.sweepGradient(wheelColors), radius = r)
                        drawCircle(brush = Brush.radialGradient(listOf(Color.White, Color.Transparent), radius = r), radius = r)
                        drawCircle(color = DoseviaBorder, radius = r, style = Stroke(width = 2f))
                    }
                    val r = with(androidx.compose.ui.platform.LocalDensity.current) { wheelSize.toPx() / 2f }
                    val markerX = (cos(Math.toRadians(hue.toDouble())).toFloat() * saturation * r)
                    val markerY = (sin(Math.toRadians(hue.toDouble())).toFloat() * saturation * r)
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { translationX = markerX; translationY = markerY }
                            .background(Color.White, CircleShape)
                            .border(3.dp, picked, CircleShape)
                    )
                }
            }

            Text("BRIGHTNESS", color = DoseviaMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = value,
                onValueChange = {
                    value = it
                    onLiveColorChanged(Color.hsv(hue, saturation, value))
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = picked, activeTrackColor = picked)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.horizontalGradient(listOf(Color.Black, Color.hsv(hue, saturation, 1f))))
                    .border(1.dp, DoseviaBorder, RoundedCornerShape(999.dp))
            )
        }
    }
}

private fun colorHue(color: Color): Float {
    val max = maxOf(color.red, color.green, color.blue)
    val min = minOf(color.red, color.green, color.blue)
    val delta = max - min
    if (delta == 0f) return 0f
    val raw = when (max) {
        color.red -> ((color.green - color.blue) / delta) % 6f
        color.green -> ((color.blue - color.red) / delta) + 2f
        else -> ((color.red - color.green) / delta) + 4f
    }
    return ((60f * raw) + 360f) % 360f
}

private fun colorSaturation(color: Color): Float {
    val max = maxOf(color.red, color.green, color.blue)
    val min = minOf(color.red, color.green, color.blue)
    return if (max == 0f) 0f else ((max - min) / max).coerceIn(0f, 1f)
}

private fun colorValue(color: Color): Float = maxOf(color.red, color.green, color.blue).coerceIn(0f, 1f)

@Composable
private fun ColorChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    size: Int
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color, CircleShape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Color.White else DoseviaBorder,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}


@Composable
private fun EditorActionButton(
    text: String,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 46.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = DoseviaBorder.copy(alpha = 0.25f),
            contentColor = contentColor,
            disabledContentColor = DoseviaMuted.copy(alpha = 0.55f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HelpGuideDialog(
    onClose: () -> Unit
) {
    val guideImages = remember {
        listOf(
            R.drawable.help_color_with_one_finger,
            R.drawable.help_zoom_and_move_with_two_fingers,
            R.drawable.help_pick_a_color,
            R.drawable.help_undo_and_redo,
            R.drawable.help_finish_and_download,
            R.drawable.help_finished_layer_limit,
            R.drawable.help_draft_auto_save,
            R.drawable.help_share_to_community,
            R.drawable.help_browse_community,
            R.drawable.help_view_artwork_details
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 980.dp)
                .heightIn(max = 820.dp),
            shape = RoundedCornerShape(28.dp),
            color = DoseviaCard,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Help & Guide",
                            color = DoseviaText,
                            fontSize = 27.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Visual guide for coloring, zooming, saving, layers, and drafts.",
                            color = DoseviaMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Surface(
                        onClick = onClose,
                        modifier = Modifier.heightIn(min = 46.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "‹",
                                color = DoseviaPinkPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Back",
                                color = DoseviaText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                guideImages.forEach { imageRes ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White)
                            .border(1.dp, DoseviaBorder, RoundedCornerShape(22.dp))
                    ) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.5f)
                                .graphicsLayer {
                                    scaleX = 1.08f
                                    scaleY = 1.08f
                                },
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementReveal(
    artwork: FinishedArtwork?,
    isSaving: Boolean,
    canMakeNew: Boolean,
    onDismiss: () -> Unit,
    onShareCommunity: () -> Unit,
    onDownload: (Bitmap?) -> Unit,
    onMakeNew: () -> Unit
) {
    val context = LocalContext.current
    var downloaded by remember { mutableStateOf(false) }
    var showMakeNewConfirm by remember { mutableStateOf(false) }
    var showShareConfirm by remember { mutableStateOf(false) }
    var shineSweepVisible by remember(artwork?.id) { mutableStateOf(false) }
    val revealSpin = remember(artwork?.id) { Animatable(0f) }
    val revealShine = remember(artwork?.id) { Animatable(0f) }

    LaunchedEffect(artwork?.id) {
        revealSpin.snapTo(0f)
        revealShine.snapTo(0f)
        shineSweepVisible = false
        revealSpin.animateTo(
            targetValue = 360f,
            animationSpec = tween(durationMillis = 760, easing = LinearEasing)
        )
        shineSweepVisible = true
        revealShine.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 820, easing = LinearEasing)
        )
        shineSweepVisible = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            color = DoseviaCard,
            tonalElevation = 12.dp
        ) {
            Box(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEAF1F8))
                        .border(1.dp, DoseviaBorder, CircleShape)
                        .clickable(enabled = !isSaving) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = DoseviaText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                Text("Artwork Finished", color = DoseviaText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    artwork?.layerName ?: "Finished Layer",
                    color = DoseviaGreen,
                    fontWeight = FontWeight.Bold
                )
                val previewAspectRatio = remember(artwork?.title) {
                    artwork?.let { finished ->
                        val (baseWidth, baseHeight) = SelectionArtworkAssets.baseSize(context, finished.title)
                        baseWidth.toFloat() / baseHeight.toFloat().coerceAtLeast(1f)
                    } ?: 1f
                }
                val finishedPreviewSignature = remember(artwork?.id, artwork?.timestamp, artwork?.strokes?.size) {
                    artwork?.strokes?.sumOf { stroke -> stroke.serializableActions.size } ?: 0
                }
                val finishedPreviewBitmap = remember(artwork?.id, artwork?.timestamp, artwork?.strokes?.size, finishedPreviewSignature) {
                    artwork?.let { finished ->
                        val (baseWidth, baseHeight) = SelectionArtworkAssets.baseSize(context, finished.title)
                        val previewWidth = 1080
                        val previewHeight = ((previewWidth.toFloat() * baseHeight.toFloat() / baseWidth.toFloat()).roundToInt()).coerceAtLeast(1)
                        ArtworkRenderer.renderArtworkBitmap(context, finished.strokes, previewWidth, previewHeight, finished.title)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(previewAspectRatio)
                        .graphicsLayer {
                            rotationY = revealSpin.value
                            cameraDistance = 24f * density
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, DoseviaPinkPrimary, RoundedCornerShape(18.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (finishedPreviewBitmap != null) {
                            Image(
                                bitmap = finishedPreviewBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds,
                            )
                        } else {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(Color.White)
                            }
                        }
                        if (shineSweepVisible) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val shineProgress = revealShine.value
                                val centerX = size.width * (1.25f - shineProgress * 1.65f)
                                val centerY = size.height * (1.25f - shineProgress * 1.65f)
                                val bandLength = (size.width + size.height) * 0.72f
                                drawLine(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.00f),
                                            Color.White.copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.00f)
                                        ),
                                        start = Offset(centerX + bandLength * 0.5f, centerY - bandLength * 0.5f),
                                        end = Offset(centerX - bandLength * 0.5f, centerY + bandLength * 0.5f)
                                    ),
                                    start = Offset(centerX + bandLength * 0.5f, centerY - bandLength * 0.5f),
                                    end = Offset(centerX - bandLength * 0.5f, centerY + bandLength * 0.5f),
                                    strokeWidth = size.minDimension * 0.16f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
                if (downloaded) {
                    Text("Saved to Gallery", color = DoseviaGreen, fontWeight = FontWeight.Bold)
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showShareConfirm = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoseviaGreen, contentColor = Color(0xFF001233)),
                        enabled = !isSaving && artwork != null
                    ) {
                        Text("Share to Community", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            downloaded = true
                            val exactPreviewBitmap = finishedPreviewBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                            onDownload(exactPreviewBitmap)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary),
                        enabled = !isSaving
                    ) {
                        Text(if (isSaving) "Saving" else "Download", color = Color(0xFFEFF4FA), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { showMakeNewConfirm = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoseviaBorder),
                        enabled = !isSaving && canMakeNew
                    ) {
                        Text("Make New", color = DoseviaText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
    }

    if (showShareConfirm) {
        Dialog(onDismissRequest = { showShareConfirm = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
                shape = RoundedCornerShape(28.dp),
                color = DoseviaCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Share to Community?", color = DoseviaText, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text("Other artists can see and like this finished artwork.", color = DoseviaMuted, fontSize = 14.sp, lineHeight = 21.sp)
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showShareConfirm = false; onShareCommunity() },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)
                        ) { Text("Share", fontWeight = FontWeight.ExtraBold) }
                        Button(
                            onClick = { showShareConfirm = false },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)
                        ) { Text("Not Now", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }


    if (showMakeNewConfirm) {
        MakeNewDraftDialog(
            onCancel = { showMakeNewConfirm = false },
            onMakeNew = {
                showMakeNewConfirm = false
                onMakeNew()
            }
        )
    }
}

@Composable
private fun EditorLoadingWaveIndicator(modifier: Modifier = Modifier) {
    EditorOperationWaveOverlay(word = "LOADING", modifier = modifier)
}

@Composable
private fun DockButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = text, color = DoseviaText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50.dp),
        color = if (selected) DoseviaPinkPrimary.copy(alpha = 0.18f) else DoseviaBorder.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) DoseviaPinkPrimary else DoseviaBorder
        )
    ) {
        Text(
            text = text,
            color = if (selected) DoseviaPinkPrimary else DoseviaText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun BrushButton(
    label: String,
    style: BrushStyle,
    activeBrushStyle: BrushStyle,
    onBrushSelected: (BrushStyle) -> Unit
) {
    val selected = style == activeBrushStyle
    Surface(
        modifier = Modifier
            .width(116.dp)
            .clickable { onBrushSelected(style) },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) DoseviaPinkPrimary.copy(alpha = 0.15f) else DoseviaBorder.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) DoseviaPinkPrimary else DoseviaBorder
        )
    ) {
        Text(
            text = label,
            color = if (selected) DoseviaPinkPrimary else DoseviaMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(stroke: StrokeData) {
    val isClearMode = stroke.color == Color.Transparent
    val activeBlend = if (isClearMode) BlendMode.Clear else BlendMode.SrcOver
    val drawColor = if (isClearMode) Color.Black else stroke.color

    when (stroke.style) {
        BrushStyle.REGION_FILL -> {
            drawPath(
                path = stroke.path,
                color = drawColor,
                blendMode = activeBlend
            )
        }

        BrushStyle.MARKER -> {
            drawPath(
                path = stroke.path,
                color = drawColor,
                style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                blendMode = activeBlend
            )
        }

        BrushStyle.NEON_GLOW -> {
            drawIntoCanvas { canvas ->
                val outerGlowPaint = Paint().apply {
                    color = if (isClearMode) Color.Black else stroke.color.copy(alpha = 0.30f)
                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    strokeWidth = stroke.width * 1.75f
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    blendMode = activeBlend
                }.asFrameworkPaint()
                outerGlowPaint.maskFilter = BlurMaskFilter(stroke.width * 0.52f, BlurMaskFilter.Blur.NORMAL)
                canvas.nativeCanvas.drawPath(stroke.path.asAndroidPath(), outerGlowPaint)

                val colorTubePaint = Paint().apply {
                    color = if (isClearMode) Color.Black else stroke.color.copy(alpha = 0.95f)
                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    strokeWidth = stroke.width * 0.68f
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    blendMode = activeBlend
                }.asFrameworkPaint()
                colorTubePaint.maskFilter = BlurMaskFilter(stroke.width * 0.06f, BlurMaskFilter.Blur.NORMAL)
                canvas.nativeCanvas.drawPath(stroke.path.asAndroidPath(), colorTubePaint)

                val corePaint = Paint().apply {
                    color = if (isClearMode) Color.Black else Color.White.copy(alpha = 0.92f)
                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    strokeWidth = (stroke.width * 0.12f).coerceAtLeast(1.0f)
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    blendMode = activeBlend
                }.asFrameworkPaint()
                canvas.nativeCanvas.drawPath(stroke.path.asAndroidPath(), corePaint)
            }
        }

        BrushStyle.AIRBRUSH -> {
            drawIntoCanvas { canvas ->
                val airbrushPaint = Paint().apply {
                    color = if (isClearMode) Color.Black else stroke.color.copy(alpha = 0.6f)
                    style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                    strokeWidth = stroke.width * 1.5f
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    blendMode = activeBlend
                }.asFrameworkPaint()
                airbrushPaint.maskFilter = BlurMaskFilter(stroke.width * 0.65f, BlurMaskFilter.Blur.NORMAL)
                canvas.nativeCanvas.drawPath(stroke.path.asAndroidPath(), airbrushPaint)
            }
        }

        BrushStyle.WATERCOLOR -> {
            drawPath(
                path = stroke.path,
                color = if (isClearMode) Color.Black else stroke.color.copy(alpha = 0.22f),
                style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                blendMode = activeBlend
            )
        }

        BrushStyle.CHISEL -> {
            drawPath(
                path = stroke.path,
                color = drawColor,
                style = Stroke(
                    width = stroke.width,
                    cap = StrokeCap.Square,
                    join = StrokeJoin.Miter,
                    miter = 3.0f
                ),
                blendMode = activeBlend
            )
        }
    }
}

private fun Color.toHexString(): String = String.format("#%06X", 0xFFFFFF and toArgb())

private fun saveArtworkBitmapToGallery(
    context: Context,
    title: String,
    layerName: String,
    bitmap: Bitmap
) {
    try {
        val fileName = "DoPalette_${title}_${layerName.replace(" ", "_")}_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DoPalette")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        ArtworkStore.touch()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}

private fun saveArtworkToGallery(
    context: Context,
    title: String,
    layerName: String,
    strokes: List<StrokeData>
) {
    try {
        val output = ArtworkRenderer.renderArtworkBitmap(context, strokes, title = title) ?: return

        val fileName = "DoPalette_${title}_${layerName.replace(" ", "_")}_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DoPalette")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
        resolver.openOutputStream(uri)?.use { stream ->
            output.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        ArtworkStore.touch()
        output.recycle()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


private fun buildExportPath(
    stroke: StrokeData,
    scaleX: Float,
    scaleY: Float
): android.graphics.Path {
    val actions = stroke.serializableActions
    if (actions.isNotEmpty()) {
        return android.graphics.Path().apply {
            actions.forEach { action ->
                when (action.type) {
                    PathActionType.MOVE_TO -> moveTo(action.x * scaleX, action.y * scaleY)
                    PathActionType.LINE_TO -> lineTo(action.x * scaleX, action.y * scaleY)
                    PathActionType.QUAD_TO -> quadTo(
                        action.x * scaleX,
                        action.y * scaleY,
                        action.x2 * scaleX,
                        action.y2 * scaleY
                    )
                }
            }
        }
    }

    return android.graphics.Path(stroke.path.asAndroidPath()).apply {
        transform(Matrix().apply { postScale(scaleX, scaleY) })
    }
}


private fun loadSavedEditorColors(context: android.content.Context): List<Color> {
    val raw = context.getSharedPreferences("dopalette_editor_colors", android.content.Context.MODE_PRIVATE)
        .getString("saved_colors", "")
        .orEmpty()
    return raw.split(",")
        .mapNotNull { token ->
            runCatching {
                val clean = token.trim().removePrefix("#")
                if (clean.length == 8) Color(android.graphics.Color.parseColor("#${clean}")) else null
            }.getOrNull()
        }
        .distinct()
        .take(12)
}

private fun saveEditorColors(context: android.content.Context, colors: List<Color>) {
    val raw = colors.distinct().take(12).joinToString(",") { color ->
        "%08X".format(color.toArgb())
    }
    context.getSharedPreferences("dopalette_editor_colors", android.content.Context.MODE_PRIVATE)
        .edit()
        .putString("saved_colors", raw)
        .apply()
}

private fun createColorFamilyRows(): List<List<Color>> = listOf(
    listOf(
        Color(0xFFFFEBEE), Color(0xFFFFCDD2), Color(0xFFEF9A9A), Color(0xFFE57373),
        Color(0xFFEF5350), Color(0xFFF44336), Color(0xFFE53935), Color(0xFFD32F2F),
        Color(0xFFC62828), Color(0xFFB71C1C)
    ),
    listOf(
        Color(0xFFFCE4EC), Color(0xFFF8BBD0), Color(0xFFF48FB1), Color(0xFFF06292),
        Color(0xFFEC407A), Color(0xFFE91E63), Color(0xFFD81B60), Color(0xFFC2185B),
        Color(0xFFAD1457), Color(0xFF880E4F)
    ),
    listOf(
        Color(0xFFF3E5F5), Color(0xFFE1BEE7), Color(0xFFCE93D8), Color(0xFFBA68C8),
        Color(0xFFAB47BC), Color(0xFF9C27B0), Color(0xFF8E24AA), Color(0xFF7B1FA2),
        Color(0xFF6A1B9A), Color(0xFF4A148C)
    ),
    listOf(
        Color(0xFFE3F2FD), Color(0xFFBBDEFB), Color(0xFF90CAF9), Color(0xFF64B5F6),
        Color(0xFF42A5F5), Color(0xFF2196F3), Color(0xFF1E88E5), Color(0xFF1976D2),
        Color(0xFF1565C0), Color(0xFF0D47A1)
    ),
    listOf(
        Color(0xFFE0F7FA), Color(0xFFB2EBF2), Color(0xFF80DEEA), Color(0xFF4DD0E1),
        Color(0xFF26C6DA), Color(0xFF00BCD4), Color(0xFF00ACC1), Color(0xFF0097A7),
        Color(0xFF00838F), Color(0xFF006064)
    ),
    listOf(
        Color(0xFFE8F5E9), Color(0xFFC8E6C9), Color(0xFFA5D6A7), Color(0xFF81C784),
        Color(0xFF66BB6A), Color(0xFF4CAF50), Color(0xFF43A047), Color(0xFF388E3C),
        Color(0xFF2E7D32), Color(0xFF1B5E20)
    ),
    listOf(
        Color(0xFFF9FBE7), Color(0xFFF0F4C3), Color(0xFFE6EE9C), Color(0xFFDCE775),
        Color(0xFFD4E157), Color(0xFFCDDC39), Color(0xFFC0CA33), Color(0xFFAFB42B),
        Color(0xFF9E9D24), Color(0xFF827717)
    ),
    listOf(
        Color(0xFFFFFDE7), Color(0xFFFFF9C4), Color(0xFFFFF59D), Color(0xFFFFF176),
        Color(0xFFFFEE58), Color(0xFFFFEB3B), Color(0xFFFDD835), Color(0xFFFBC02D),
        Color(0xFFF9A825), Color(0xFFF57F17)
    ),
    listOf(
        Color(0xFFFFF3E0), Color(0xFFFFE0B2), Color(0xFFFFCC80), Color(0xFFFFB74D),
        Color(0xFFFFA726), Color(0xFFFF9800), Color(0xFFFB8C00), Color(0xFFF57C00),
        Color(0xFFEF6C00), Color(0xFFE65100)
    ),
    listOf(
        Color(0xFFEFEBE9), Color(0xFFD7CCC8), Color(0xFFBCAAA4), Color(0xFFA1887F),
        Color(0xFF8D6E63), Color(0xFF795548), Color(0xFF6D4C41), Color(0xFF5D4037),
        Color(0xFF4E342E), Color(0xFF3E2723)
    ),
    listOf(
        Color(0xFFFFFFFF), Color(0xFFF5F5F5), Color(0xFFE0E0E0), Color(0xFFBDBDBD),
        Color(0xFF9E9E9E), Color(0xFF757575), Color(0xFF616161), Color(0xFF424242),
        Color(0xFF212121), Color(0xFF000000)
    )
)

private fun createCompletePalette(): List<Color> {
    val primaryColors = listOf(
        Color(0xFFFF1744), // Primary Red
        Color(0xFF2979FF), // Primary Blue
        Color(0xFFFFEA00)  // Primary Yellow
    )
    val secondaryColors = listOf(
        Color(0xFFFF9100), // Orange
        Color(0xFF00E676), // Green
        Color(0xFFD500F9)  // Purple
    )
    val tertiaryColors = listOf(
        Color(0xFFFF3D00), Color(0xFFFFC400), Color(0xFFC6FF00), Color(0xFF00BFA5),
        Color(0xFF00B0FF), Color(0xFF304FFE), Color(0xFFAA00FF), Color(0xFFFF4081)
    )
    val naturalColors = listOf(
        Color(0xFFE53935), Color(0xFFEF5350), Color(0xFFFF7043), Color(0xFFFFB300),
        Color(0xFFFFEB3B), Color(0xFFCDDC39), Color(0xFF8BC34A), Color(0xFF4CAF50),
        Color(0xFF009688), Color(0xFF00BCD4), Color(0xFF03A9F4), Color(0xFF2196F3),
        Color(0xFF3F51B5), Color(0xFF673AB7), Color(0xFF9C27B0), Color(0xFFE91E63),
        Color(0xFF795548), Color(0xFF607D8B)
    )

    fun addVariationRamp(target: MutableList<Color>, base: Color) {
        // Ordered from light tint → pure color → deep shade so users can easily find related colors.
        target += lerpColor(Color.White, base, 0.16f)
        target += lerpColor(Color.White, base, 0.32f)
        target += lerpColor(Color.White, base, 0.50f)
        target += lerpColor(Color.White, base, 0.68f)
        target += base
        target += lerpColor(base, Color.Black, 0.16f)
        target += lerpColor(base, Color.Black, 0.32f)
        target += lerpColor(base, Color.Black, 0.50f)
    }

    val palette = mutableListOf<Color>()

    // Always start with the three primary colors and their variations.
    primaryColors.forEach { addVariationRamp(palette, it) }
    secondaryColors.forEach { addVariationRamp(palette, it) }
    tertiaryColors.forEach { addVariationRamp(palette, it) }
    naturalColors.forEach { addVariationRamp(palette, it) }

    val skinTones = listOf(
        Color(0xFFFFE0BD), Color(0xFFF1C27D), Color(0xFFE0AC69), Color(0xFFC68642),
        Color(0xFF8D5524), Color(0xFF6D3F1F), Color(0xFF4B2A14), Color(0xFF2E1A0F)
    )
    val neons = listOf(
        Color(0xFFFF005D), Color(0xFFFF3D00), Color(0xFFFFEA00), Color(0xFF39FF14),
        Color(0xFF00FFD5), Color(0xFF00A3FF), Color(0xFF7A00FF), Color(0xFFFF00E6)
    )
    val neutrals = listOf(
        Color(0xFFEFF4FA), Color(0xFFF5F5F5), Color(0xFFE0E0E0), Color(0xFFBDBDBD),
        Color(0xFF9E9E9E), Color(0xFF757575), Color(0xFF424242), Color(0xFF000000)
    )
    palette += skinTones
    palette += neons
    palette += neutrals
    return palette.distinct()
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val safeFraction = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * safeFraction,
        green = start.green + (end.green - start.green) * safeFraction,
        blue = start.blue + (end.blue - start.blue) * safeFraction,
        alpha = 1f
    )
}

@Composable
private fun DoneInterstitialPlaceholderDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0x99081225)).padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
                shape = RoundedCornerShape(30.dp),
                color = DoseviaCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
                shadowElevation = 22.dp
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(58.dp).background(Color(0xFFEAF1FF), CircleShape), contentAlignment = Alignment.Center) {
                        Text("AD", color = DoseviaPinkPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                    Text("Interstitial Ad Placeholder", color = DoseviaText, fontWeight = FontWeight.Black, fontSize = 23.sp, textAlign = TextAlign.Center)
                    Text("This appears only after the artwork is saved and the DONE result is shown. Premium users will never see this.", color = DoseviaMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
                    Surface(shape = RoundedCornerShape(20.dp), color = DoseviaSoftPanel, modifier = Modifier.fillMaxWidth()) {
                        Text("AdMob interstitial will be connected later.", modifier = Modifier.padding(16.dp), color = DoseviaText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)) {
                        Text("Continue", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
