package com.dopalette.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlin.math.max

object SelectionArtworkAssets {
    private const val ROOT = "selections"
    private const val PREVIEW_MAX_EDGE = 768

    private val bitmapCache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 10).toInt().coerceAtLeast(8 * 1024)) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    @Volatile private var catalogCache: List<CatalogEntry>? = null
    @Volatile private var categoriesCache: List<String>? = null

    data class CatalogEntry(
        val title: String,
        val category: String
    )

    fun clearCaches() {
        bitmapCache.evictAll()
        catalogCache = null
        categoriesCache = null
    }

    fun folderFor(title: String): String = "$ROOT/Fruits/${title.trim()}"
    fun folderFor(category: String, title: String): String = "$ROOT/${category.trim()}/${title.trim()}"
    fun basePath(title: String): String = "${folderFor(title)}/base.png"
    fun maskPath(title: String): String = "${folderFor(title)}/mask.png"
    fun basePath(category: String, title: String): String = "${folderFor(category, title)}/base.png"
    fun maskPath(category: String, title: String): String = "${folderFor(category, title)}/mask.png"

    fun loadBaseBitmap(context: Context, title: String): Bitmap? {
        val category = findCategoryForTitle(context, title.trim()) ?: "Fruits"
        return loadBitmap(context, basePath(category, title), maxEdge = null)
    }

    fun loadBasePreviewBitmap(context: Context, title: String, maxEdge: Int = PREVIEW_MAX_EDGE): Bitmap? {
        val category = findCategoryForTitle(context, title.trim()) ?: "Fruits"
        return loadBitmap(context, basePath(category, title), maxEdge = maxEdge.coerceAtLeast(384))
    }

    fun baseSize(context: Context, title: String): Pair<Int, Int> {
        val category = findCategoryForTitle(context, title.trim()) ?: "Fruits"
        val path = basePath(category, title)
        return try {
            context.assets.open(path).use { input ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, options)
                val width = options.outWidth.takeIf { it > 0 } ?: 1
                val height = options.outHeight.takeIf { it > 0 } ?: 1
                width to height
            }
        } catch (e: Exception) {
            1 to 1
        }
    }

    fun hasBaseArtworkForTitle(context: Context, title: String): Boolean {
        val category = findCategoryForTitle(context, title.trim()) ?: return false
        return hasBaseArtwork(context, category, title.trim())
    }

    /**
     * Kept for compatibility, but intentionally lightweight now.
     * DoPalette has many large assets; decoding all of them on startup causes lag/crashes
     * on lower-spec phones. Images are loaded lazily when visible or opened in the editor.
     */
    fun preloadBaseBitmaps(context: Context, entries: List<CatalogEntry>) = Unit

    fun loadMaskBitmap(context: Context, title: String): Bitmap? {
        val category = findCategoryForTitle(context, title.trim()) ?: "Fruits"
        return loadBitmap(context, maskPath(category, title), maxEdge = null)
    }

    fun loadCatalog(context: Context): List<CatalogEntry> {
        catalogCache?.let { return it }
        return try {
            val categories = context.assets.list(ROOT).orEmpty()
                .filter { it.isNotBlank() }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })

            categories.flatMap { category ->
                context.assets.list("$ROOT/$category").orEmpty()
                    .filter { item -> hasBaseArtwork(context, category, item) }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                    .map { item -> CatalogEntry(title = item, category = category) }
            }.also { loaded ->
                catalogCache = loaded
                categoriesCache = loaded.map { it.category }.distinct()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadCategories(context: Context): List<String> {
        categoriesCache?.let { return it }
        val fromCatalog = loadCatalog(context).map { it.category }.distinct()
        if (fromCatalog.isNotEmpty()) return fromCatalog
        return try {
            context.assets.list(ROOT).orEmpty()
                .filter { category ->
                    context.assets.list("$ROOT/$category").orEmpty()
                        .any { item -> hasBaseArtwork(context, category, item) }
                }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                .also { categoriesCache = it }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun findCategoryForTitle(context: Context?, title: String): String? {
        if (context == null) return null
        return try {
            context.assets.list(ROOT).orEmpty().firstOrNull { category ->
                hasBaseArtwork(context, category, title)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun hasBaseArtwork(context: Context, category: String, title: String): Boolean {
        val folder = folderFor(category, title)
        return assetExists(context, "$folder/base.png")
    }

    private fun assetExists(context: Context, assetPath: String): Boolean {
        return try {
            context.assets.open(assetPath).use { true }
        } catch (e: Exception) {
            false
        }
    }

    private fun loadBitmap(context: Context, assetPath: String, maxEdge: Int?): Bitmap? {
        val cacheKey = if (maxEdge == null) "full:$assetPath" else "preview:$maxEdge:$assetPath"
        bitmapCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                if (maxEdge != null) {
                    inJustDecodeBounds = true
                    context.assets.open(assetPath).use { input -> BitmapFactory.decodeStream(input, null, this) }
                    inSampleSize = calculateInSampleSize(outWidth, outHeight, maxEdge)
                    inJustDecodeBounds = false
                }
            }

            context.assets.open(assetPath).use { input ->
                BitmapFactory.decodeStream(input, null, options)?.let { decoded ->
                    val ready = if (maxEdge != null && max(decoded.width, decoded.height) > maxEdge) {
                        val ratio = maxEdge.toFloat() / max(decoded.width, decoded.height).toFloat()
                        val targetWidth = (decoded.width * ratio).toInt().coerceAtLeast(1)
                        val targetHeight = (decoded.height * ratio).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true).also {
                            if (it !== decoded && !decoded.isRecycled) decoded.recycle()
                        }
                    } else {
                        decoded
                    }
                    bitmapCache.put(cacheKey, ready)
                    ready
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var halfWidth = width / 2
        var halfHeight = height / 2
        while (halfWidth / sample >= maxEdge && halfHeight / sample >= maxEdge) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }
}
