package com.dopalette.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb

object ArtworkRenderer {
    fun renderArtworkBitmap(
        context: Context,
        strokes: List<StrokeData>,
        width: Int = 0,
        height: Int = 0,
        title: String = "APPLE"
    ): Bitmap? {
        val base = SelectionArtworkAssets.loadBaseBitmap(context, title) ?: return null

        // Render at the artwork's native resolution first, then scale only the final
        // composed bitmap for thumbnails/previews. Rendering directly at small preview
        // sizes can lose thin regions (for example stems) because the region mask is
        // rebuilt on a downscaled line-art image. Native-first rendering keeps Editor,
        // Finished, Home, Category, and Download outputs identical for every artwork.
        val renderWidth = base.width.coerceAtLeast(1)
        val renderHeight = base.height.coerceAtLeast(1)
        val requestedWidth = if (width > 0) width else renderWidth
        val requestedHeight = if (height > 0) height else renderHeight

        val nativeOutput = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(nativeOutput)
        val dst = Rect(0, 0, renderWidth, renderHeight)

        canvas.drawColor(android.graphics.Color.WHITE)
        val artworkMask = SelectionArtworkAssets.loadMaskBitmap(context, title)
        drawStrokes(canvas, strokes, base, renderWidth, renderHeight, artworkMask)

        // Match the proven Fruits pipeline: keep every category on the same layer order.
        // User colors live below the original smooth artwork, then the base PNG is drawn
        // with MULTIPLY so white paper stays transparent over colors while black/gray
        // anti-aliased outlines sit on top. Do not use the hard binary outline overlay here;
        // it causes the jagged/white rim artifacts seen on non-fruit pages.
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }
        canvas.drawBitmap(base, null, dst, linePaint)
        linePaint.xfermode = null

        if (requestedWidth == renderWidth && requestedHeight == renderHeight) {
            return nativeOutput
        }

        val scaled = Bitmap.createScaledBitmap(nativeOutput, requestedWidth.coerceAtLeast(1), requestedHeight.coerceAtLeast(1), true)
        if (!nativeOutput.isRecycled && scaled !== nativeOutput) nativeOutput.recycle()
        return scaled
    }

    private fun drawStrokes(canvas: Canvas, strokes: List<StrokeData>, lineArt: Bitmap, targetWidth: Int, targetHeight: Int, artworkMask: Bitmap?) {
        val allStrokesCheckpoint = canvas.saveLayer(null, null)
        val regionCache = mutableMapOf<String, Bitmap>()
        val preparedLineArt = AutoRegionDetector.prepareLineArt(lineArt, targetWidth, targetHeight)
        strokes.forEach { stroke ->
            val clearMode = stroke.color == Color.Transparent
            if (clearMode) {
                // Erase only the accumulated user color layer. The white canvas, base image,
                // outline, and background are drawn outside this layer, so the eraser never
                // becomes white paint and never damages the original artwork.
                if (stroke.style == BrushStyle.REGION_FILL) {
                    val mask = regionMaskForStroke(stroke, lineArt, targetWidth, targetHeight, regionCache, preparedLineArt)
                    if (mask != null) {
                        val clearPaint = Paint().apply {
                            isAntiAlias = false
                            isFilterBitmap = false
                            isDither = false
                            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                        }
                        canvas.drawBitmap(mask, 0f, 0f, clearPaint)
                        clearPaint.xfermode = null
                    }
                } else {
                    drawSingleStroke(canvas, stroke, targetWidth, targetHeight)
                }
            } else {
                val checkpoint = canvas.saveLayer(null, null)
                drawSingleStroke(canvas, stroke, targetWidth, targetHeight)

                val mask = regionMaskForStroke(stroke, lineArt, targetWidth, targetHeight, regionCache, preparedLineArt)
                if (mask != null) {
                    val maskPaint = Paint().apply {
                        isAntiAlias = false
                        isFilterBitmap = false
                        isDither = false
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                    }
                    canvas.drawBitmap(mask, 0f, 0f, maskPaint)
                    maskPaint.xfermode = null
                }
                canvas.restoreToCount(checkpoint)
            }
        }
        artworkMask?.takeIf { !it.isRecycled }?.let { mask ->
            val maskPaint = Paint().apply {
                isAntiAlias = false
                isFilterBitmap = false
                isDither = false
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            canvas.drawBitmap(mask, null, Rect(0, 0, targetWidth, targetHeight), maskPaint)
            maskPaint.xfermode = null
        }
        canvas.restoreToCount(allStrokesCheckpoint)
        regionCache.values.forEach { if (!it.isRecycled) it.recycle() }
    }

    private fun regionMaskForStroke(
        stroke: StrokeData,
        lineArt: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        cache: MutableMap<String, Bitmap>,
        preparedLineArt: AutoRegionDetector.PreparedLineArt? = null
    ): Bitmap? {
        if (stroke.regionSeedX < 0f || stroke.regionSeedY < 0f) return null
        val sourceWidth = stroke.canvasWidth.takeIf { it > 0f } ?: targetWidth.toFloat()
        val sourceHeight = stroke.canvasHeight.takeIf { it > 0f } ?: targetHeight.toFloat()
        val seedX = stroke.regionSeedX * (targetWidth.toFloat() / sourceWidth)
        val seedY = stroke.regionSeedY * (targetHeight.toFloat() / sourceHeight)
        val key = "${seedX.toInt()}_${seedY.toInt()}_${targetWidth}x${targetHeight}"
        return cache.getOrPut(key) {
            AutoRegionDetector.createMaskForTap(preparedLineArt, seedX, seedY)?.bitmap
                ?: AutoRegionDetector.createMaskForTap(lineArt, seedX, seedY, targetWidth, targetHeight)?.bitmap
                ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }.takeIf { it.width == targetWidth && it.height == targetHeight }
    }

    private fun drawSingleStroke(canvas: Canvas, stroke: StrokeData, targetWidth: Int, targetHeight: Int) {
        val sourceWidth = stroke.canvasWidth.takeIf { it > 0f } ?: targetWidth.toFloat()
        val sourceHeight = stroke.canvasHeight.takeIf { it > 0f } ?: targetHeight.toFloat()
        val scaleX = targetWidth.toFloat() / sourceWidth
        val scaleY = targetHeight.toFloat() / sourceHeight

        val path = Path().apply {
            stroke.serializableActions.forEach { action ->
                when (action.type) {
                    PathActionType.MOVE_TO -> moveTo(action.x * scaleX, action.y * scaleY)
                    PathActionType.LINE_TO -> lineTo(action.x * scaleX, action.y * scaleY)
                    PathActionType.QUAD_TO -> quadraticBezierTo(
                        action.x * scaleX,
                        action.y * scaleY,
                        action.x2 * scaleX,
                        action.y2 * scaleY
                    )
                }
            }
        }
        val scaledWidth = stroke.width * ((scaleX + scaleY) / 2f)
        val clearMode = stroke.color == Color.Transparent
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = scaledWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = stroke.color.toArgb()
            if (clearMode) xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        when (stroke.style) {
            BrushStyle.REGION_FILL -> {
                paint.style = Paint.Style.FILL
                canvas.drawPath(path.asAndroidPath(), paint)
            }
            BrushStyle.MARKER, BrushStyle.CHISEL -> {
                if (stroke.style == BrushStyle.CHISEL) {
                    paint.strokeCap = Paint.Cap.SQUARE
                    paint.strokeJoin = Paint.Join.MITER
                }
                canvas.drawPath(path.asAndroidPath(), paint)
            }
            BrushStyle.NEON_GLOW -> {
                val outerGlow = Paint(paint).apply {
                    alpha = 82
                    strokeWidth = scaledWidth * 1.75f
                    maskFilter = BlurMaskFilter(scaledWidth * 0.52f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawPath(path.asAndroidPath(), outerGlow)

                val colorTube = Paint(paint).apply {
                    alpha = 242
                    strokeWidth = scaledWidth * 0.68f
                    maskFilter = BlurMaskFilter((scaledWidth * 0.06f).coerceAtLeast(0.5f), BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawPath(path.asAndroidPath(), colorTube)

                val core = Paint(paint).apply {
                    alpha = 235
                    color = android.graphics.Color.WHITE
                    strokeWidth = (scaledWidth * 0.12f).coerceAtLeast(1.0f)
                    maskFilter = null
                }
                canvas.drawPath(path.asAndroidPath(), core)
            }
            BrushStyle.AIRBRUSH -> {
                paint.alpha = 150
                paint.strokeWidth = scaledWidth * 1.5f
                paint.maskFilter = BlurMaskFilter(scaledWidth * 0.65f, BlurMaskFilter.Blur.NORMAL)
                canvas.drawPath(path.asAndroidPath(), paint)
            }
            BrushStyle.WATERCOLOR -> {
                paint.alpha = 75
                canvas.drawPath(path.asAndroidPath(), paint)
            }
        }
    }
}
