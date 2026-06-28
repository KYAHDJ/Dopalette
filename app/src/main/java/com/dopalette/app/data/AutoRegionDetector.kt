package com.dopalette.app.data

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.roundToInt

object AutoRegionDetector {
    private val preparedCache = java.util.concurrent.ConcurrentHashMap<String, PreparedLineArt>()

    // Treat every real outline as a hard wall, including thin seed outlines,
    // shine outlines, veins, and anti-aliased gray edge pixels.
    private const val LINE_BLOCK_LIMIT = 235
    private const val LINE_PAINT_UNDER_LIMIT = 254
    private const val MIN_REGION_PIXELS = 8
    private const val BARRIER_DILATE_RADIUS = 0
    private const val PAINT_UNDER_LINE_RADIUS = 6
    private const val REGION_EDGE_EXPAND_RADIUS = 4
    private const val MICRO_WHITE_ISLAND_MAX_PIXELS = 180
    private const val MICRO_WHITE_ISLAND_NEAR_RADIUS = 3

    data class PreparedLineArt(
        val width: Int,
        val height: Int,
        val hardBarrier: BooleanArray,
        val paintUnderLine: BooleanArray
    )

    data class RegionMask(
        val bitmap: Bitmap,
        val pixelCount: Int,
        val touchesEdge: Boolean
    )

    fun clearCaches() {
        preparedCache.clear()
    }

    fun prepareLineArt(lineArt: Bitmap?, canvasWidth: Int, canvasHeight: Int): PreparedLineArt? {
        if (lineArt == null || lineArt.isRecycled || canvasWidth <= 1 || canvasHeight <= 1) return null
        val key = "${lineArt.hashCode()}_${lineArt.width}x${lineArt.height}_${canvasWidth}x${canvasHeight}"
        preparedCache[key]?.let { return it }

        val scaled = Bitmap.createScaledBitmap(lineArt, canvasWidth, canvasHeight, false)
        val total = canvasWidth * canvasHeight
        val rawPixels = IntArray(total)
        scaled.getPixels(rawPixels, 0, canvasWidth, 0, 0, canvasWidth, canvasHeight)
        if (scaled !== lineArt && !scaled.isRecycled) scaled.recycle()

        val originalBlock = BooleanArray(total)
        val paintUnderLine = BooleanArray(total)
        for (i in 0 until total) {
            originalBlock[i] = isLinePixel(rawPixels[i], LINE_BLOCK_LIMIT)
            paintUnderLine[i] = isLinePixel(rawPixels[i], LINE_PAINT_UNDER_LIMIT)
        }

        val hardBarrier = dilate(originalBlock, canvasWidth, canvasHeight, BARRIER_DILATE_RADIUS)
        val prepared = PreparedLineArt(canvasWidth, canvasHeight, hardBarrier, paintUnderLine)
        if (preparedCache.size > 12) preparedCache.clear()
        preparedCache[key] = prepared
        return prepared
    }


    fun createSolidOutlineOverlay(lineArt: Bitmap?, canvasWidth: Int, canvasHeight: Int): Bitmap? {
        if (lineArt == null || lineArt.isRecycled || canvasWidth <= 1 || canvasHeight <= 1) return null
        val scaled = Bitmap.createScaledBitmap(lineArt, canvasWidth, canvasHeight, false)
        val total = canvasWidth * canvasHeight
        val rawPixels = IntArray(total)
        scaled.getPixels(rawPixels, 0, canvasWidth, 0, 0, canvasWidth, canvasHeight)
        if (scaled !== lineArt && !scaled.isRecycled) scaled.recycle()

        val linePixels = BooleanArray(total)
        for (i in 0 until total) {
            linePixels[i] = isLinePixel(rawPixels[i], 248)
        }
        val outputPixels = IntArray(total)
        for (i in 0 until total) {
            outputPixels[i] = if (linePixels[i]) Color.BLACK else Color.TRANSPARENT
        }
        val output = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        output.setPixels(outputPixels, 0, canvasWidth, 0, 0, canvasWidth, canvasHeight)
        return output
    }

    fun createMaskForTap(prepared: PreparedLineArt?, tapX: Float, tapY: Float): RegionMask? {
        if (prepared == null) return null
        return try {
            createMaskFromPrepared(prepared, tapX, tapY)
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    fun createMaskForTap(
        lineArt: Bitmap?,
        tapX: Float,
        tapY: Float,
        canvasWidth: Int,
        canvasHeight: Int
    ): RegionMask? {
        val prepared = prepareLineArt(lineArt, canvasWidth, canvasHeight) ?: return null
        return try {
            createMaskFromPrepared(prepared, tapX, tapY)
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    private fun createMaskFromPrepared(prepared: PreparedLineArt, tapX: Float, tapY: Float): RegionMask? {
        val width = prepared.width
        val height = prepared.height
        val total = width * height
        if (width <= 1 || height <= 1 || prepared.hardBarrier.size < total) return null

        val seedX = tapX.roundToInt().coerceIn(0, width - 1)
        val seedY = tapY.roundToInt().coerceIn(0, height - 1)
        val start = nearestOpenIndex(prepared.hardBarrier, seedX, seedY, width, height) ?: return null

        val visited = BooleanArray(total)
        val region = BooleanArray(total)
        val queue = IntArray(total)
        var head = 0
        var tail = 0
        queue[tail++] = start
        visited[start] = true

        var count = 0
        var touchesEdge = false
        while (head < tail) {
            val index = queue[head++]
            if (prepared.hardBarrier[index]) continue
            region[index] = true
            count++

            val x = index % width
            val y = index / width
            if (x == 0 || y == 0 || x == width - 1 || y == height - 1) touchesEdge = true

            if (x > 0) tail = pushIfOpen(prepared.hardBarrier, visited, queue, tail, x - 1, y, width)
            if (x < width - 1) tail = pushIfOpen(prepared.hardBarrier, visited, queue, tail, x + 1, y, width)
            if (y > 0) tail = pushIfOpen(prepared.hardBarrier, visited, queue, tail, x, y - 1, width)
            if (y < height - 1) tail = pushIfOpen(prepared.hardBarrier, visited, queue, tail, x, y + 1, width)
        }

        if (count < MIN_REGION_PIXELS) return null

        val outputPixels = IntArray(total)
        writeStrictRegionMask(region, prepared.hardBarrier, prepared.paintUnderLine, outputPixels, width, height)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return RegionMask(output, count, touchesEdge)
    }

    private fun nearestOpenIndex(barrier: BooleanArray, seedX: Int, seedY: Int, width: Int, height: Int): Int? {
        val direct = seedY * width + seedX
        if (!barrier[direct]) return direct
        for (radius in 1..8) {
            for (dy in -radius..radius) {
                val y = seedY + dy
                if (y !in 0 until height) continue
                for (dx in -radius..radius) {
                    val x = seedX + dx
                    if (x !in 0 until width) continue
                    val index = y * width + x
                    if (!barrier[index]) return index
                }
            }
        }
        return null
    }

    private fun pushIfOpen(
        barrier: BooleanArray,
        visited: BooleanArray,
        queue: IntArray,
        tail: Int,
        x: Int,
        y: Int,
        width: Int
    ): Int {
        val index = y * width + x
        if (visited[index]) return tail
        visited[index] = true
        if (!barrier[index]) {
            if (tail >= queue.size) return tail
            queue[tail] = index
            return tail + 1
        }
        return tail
    }

    private fun writeStrictRegionMask(
        region: BooleanArray,
        hardBarrier: BooleanArray,
        paintUnderLine: BooleanArray,
        outputPixels: IntArray,
        width: Int,
        height: Int
    ) {
        val total = width * height

        // Expand ONLY inside the same open area. This makes every tiny white sliver
        // beside a selected region paintable, but it still cannot cross black outlines.
        val expandedRegion = expandRegionThroughOpenPixels(
            source = region,
            hardBarrier = hardBarrier,
            width = width,
            height = height,
            radius = REGION_EDGE_EXPAND_RADIUS
        )

        val paintRegion = includeTinyNearbyWhiteIslands(
            region = expandedRegion,
            hardBarrier = hardBarrier,
            width = width,
            height = height,
            maxIslandPixels = MICRO_WHITE_ISLAND_MAX_PIXELS,
            nearRadius = MICRO_WHITE_ISLAND_NEAR_RADIUS
        )

        for (i in 0 until total) {
            if (paintRegion[i]) outputPixels[i] = Color.WHITE
        }

        // Also allow paint underneath the solid black/gray outline pixels that touch
        // this region. The original line art is drawn on top afterward, so this removes
        // pinhole white dots and rim halos without exposing color outside the line.
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (!paintUnderLine[index]) continue
                var touchesRegion = false
                loop@ for (dy in -PAINT_UNDER_LINE_RADIUS..PAINT_UNDER_LINE_RADIUS) {
                    val yy = y + dy
                    if (yy !in 0 until height) continue
                    for (dx in -PAINT_UNDER_LINE_RADIUS..PAINT_UNDER_LINE_RADIUS) {
                        val xx = x + dx
                        if (xx !in 0 until width) continue
                        if (paintRegion[yy * width + xx]) {
                            touchesRegion = true
                            break@loop
                        }
                    }
                }
                if (touchesRegion) outputPixels[index] = Color.WHITE
            }
        }
    }


    private fun includeTinyNearbyWhiteIslands(
        region: BooleanArray,
        hardBarrier: BooleanArray,
        width: Int,
        height: Int,
        maxIslandPixels: Int,
        nearRadius: Int
    ): BooleanArray {
        val total = width * height
        val output = region.copyOf()
        val visited = BooleanArray(total)
        val queue = IntArray(total)
        val component = IntArray(maxIslandPixels.coerceAtLeast(1) + 1)

        for (start in 0 until total) {
            if (visited[start] || output[start] || hardBarrier[start]) continue

            var head = 0
            var tail = 0
            var componentCount = 0
            var tooLarge = false
            var isNearPaintedRegion = false

            queue[tail++] = start
            visited[start] = true

            while (head < tail) {
                val index = queue[head++]
                if (!tooLarge && componentCount < component.size) {
                    component[componentCount] = index
                }
                componentCount++
                if (componentCount > maxIslandPixels) tooLarge = true

                val x = index % width
                val y = index / width
                if (!isNearPaintedRegion && touchesPaintedRegion(output, x, y, width, height, nearRadius)) {
                    isNearPaintedRegion = true
                }

                if (x > 0) {
                    val next = index - 1
                    if (!visited[next] && !output[next] && !hardBarrier[next]) {
                        visited[next] = true
                        if (tail < queue.size) queue[tail++] = next
                    }
                }
                if (x < width - 1) {
                    val next = index + 1
                    if (!visited[next] && !output[next] && !hardBarrier[next]) {
                        visited[next] = true
                        if (tail < queue.size) queue[tail++] = next
                    }
                }
                if (y > 0) {
                    val next = index - width
                    if (!visited[next] && !output[next] && !hardBarrier[next]) {
                        visited[next] = true
                        if (tail < queue.size) queue[tail++] = next
                    }
                }
                if (y < height - 1) {
                    val next = index + width
                    if (!visited[next] && !output[next] && !hardBarrier[next]) {
                        visited[next] = true
                        if (tail < queue.size) queue[tail++] = next
                    }
                }
            }

            if (!tooLarge && isNearPaintedRegion) {
                for (i in 0 until componentCount) {
                    output[component[i]] = true
                }
            }
        }
        return output
    }

    private fun touchesPaintedRegion(
        region: BooleanArray,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int
    ): Boolean {
        val safeRadius = radius.coerceAtLeast(1)
        for (dy in -safeRadius..safeRadius) {
            val yy = y + dy
            if (yy !in 0 until height) continue
            for (dx in -safeRadius..safeRadius) {
                val xx = x + dx
                if (xx !in 0 until width) continue
                if (region[yy * width + xx]) return true
            }
        }
        return false
    }

    private fun expandRegionThroughOpenPixels(
        source: BooleanArray,
        hardBarrier: BooleanArray,
        width: Int,
        height: Int,
        radius: Int
    ): BooleanArray {
        var current = source.copyOf()
        repeat(radius.coerceAtLeast(0)) {
            val next = current.copyOf()
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val index = y * width + x
                    if (current[index] || hardBarrier[index]) continue
                    var touches = false
                    if (x > 0 && current[index - 1]) touches = true
                    if (x < width - 1 && current[index + 1]) touches = true
                    if (y > 0 && current[index - width]) touches = true
                    if (y < height - 1 && current[index + width]) touches = true
                    if (touches) next[index] = true
                }
            }
            current = next
        }
        return current
    }

    private fun dilate(source: BooleanArray, width: Int, height: Int, radius: Int): BooleanArray {
        val output = BooleanArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var blocked = false
                loop@ for (dy in -radius..radius) {
                    val yy = y + dy
                    if (yy !in 0 until height) continue
                    for (dx in -radius..radius) {
                        val xx = x + dx
                        if (xx !in 0 until width) continue
                        if (source[yy * width + xx]) {
                            blocked = true
                            break@loop
                        }
                    }
                }
                output[y * width + x] = blocked
            }
        }
        return output
    }

    private fun isLinePixel(pixel: Int, limit: Int): Boolean {
        val alpha = Color.alpha(pixel)
        if (alpha < 30) return false
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        val luminance = (red * 0.299f + green * 0.587f + blue * 0.114f)
        return luminance < limit
    }
}
