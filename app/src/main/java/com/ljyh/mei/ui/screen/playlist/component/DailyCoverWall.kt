package com.ljyh.mei.ui.screen.playlist.component

import android.graphics.Bitmap
import android.graphics.Paint as AndroidPaint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@Composable
internal fun DailyCoverWall(
    covers: List<String>,
    today: LocalDate,
) {
    val context = LocalContext.current
    val artworks = produceState<List<CoverArtwork>>(emptyList(), covers) {
        if (covers.isEmpty()) return@produceState
        val limiter = Semaphore(6)
        value = coroutineScope {
            covers.mapIndexed { index, url ->
                async {
                    if (url.isBlank()) return@async CoverArtwork(index, null, 400f + index)
                    limiter.withPermit {
                        try {
                            val request = ImageRequest.Builder(context)
                                .data(url)
                                .size(192)
                                .allowHardware(false)
                                .build()
                            val result = context.imageLoader.execute(request) as? SuccessResult
                                ?: return@withPermit CoverArtwork(index, null, 400f + index)
                            withContext(Dispatchers.Default) {
                                val bitmap = squareCoverBitmap(result.image.toBitmap(), 192)
                                CoverArtwork(index, bitmap, artworkHue(bitmap))
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            CoverArtwork(index, null, 400f + index)
                        }
                    }
                }
            }.awaitAll().sortedWith(compareBy<CoverArtwork> { it.hue }.thenBy { it.index })
        }
    }.value
    DisposableEffect(artworks) {
        onDispose { artworks.forEach { it.bitmap?.recycle() } }
    }

    DailyCoverScatterWall(artworks, covers, today)
}

@Composable
private fun DailyCoverScatterWall(artworks: List<CoverArtwork>, covers: List<String>, today: LocalDate) {
    val density = LocalDensity.current
    val surface = MaterialTheme.colorScheme.surface
    val bitmapPaint = remember { AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG) }
    val emptyPaint = remember { AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(75, 75, 88)
    } }

    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        val stageWidth = maxWidth
        val stageHeight = if (covers.isEmpty()) 220.dp else stageWidth * maxOf(0.86f, covers.size / 36f)
        val widthPx = with(density) { stageWidth.toPx() }
        val heightPx = with(density) { stageHeight.toPx() }
        val seed = remember(covers, today) {
            covers.fold(today.toEpochDay().toInt()) { hash, url -> hash * 31 + url.hashCode() }
        }
        val placements = remember(covers.size, seed, widthPx, heightPx) {
            scatterPlacements(covers.size, widthPx, heightPx, seed)
        }

        Box(Modifier.fillMaxWidth().height(stageHeight)) {
            Canvas(Modifier.fillMaxSize()) {
                if (artworks.isEmpty()) return@Canvas
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    placements.forEachIndexed { index, placement ->
                        val artwork = artworks.getOrNull(index) ?: return@forEachIndexed
                        val half = placement.side / 2f
                        val bounds = RectF(-half, -half, half, half)
                        val save = native.save()
                        native.translate(placement.x, placement.y)
                        native.rotate(placement.rotation)
                        val bitmap = artwork.bitmap
                        if (bitmap != null && !bitmap.isRecycled) {
                            native.drawBitmap(bitmap, null, bounds, bitmapPaint)
                        } else {
                            native.drawRoundRect(bounds, half * 0.10f, half * 0.10f, emptyPaint)
                        }
                        native.restoreToCount(save)
                    }
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(stageHeight * 0.36f)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, surface))),
            )
            DailyCoverDate(today, Modifier.align(Alignment.TopEnd), MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun DailyCoverDate(today: LocalDate, modifier: Modifier = Modifier, color: Color = Color.White) {
    Column(modifier.padding(top = 18.dp, end = 20.dp), horizontalAlignment = Alignment.End) {
        Text(
            text = "%02d.%02d".format(today.monthValue, today.dayOfMonth),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA),
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = 0.76f),
        )
    }
}

private data class CoverArtwork(val index: Int, val bitmap: Bitmap?, val hue: Float)

private data class CoverPlacement(
    val x: Float,
    val y: Float,
    val side: Float,
    val rotation: Float,
)

private fun scatterPlacements(count: Int, width: Float, height: Float, seed: Int): List<CoverPlacement> {
    if (width <= 1f || height <= 1f) return emptyList()
    val random = Random(seed)
    val placements = mutableListOf<CoverPlacement>()
    val coveredAreas = mutableListOf<Float>()
    val base = width / 7f
    val dateZone = RectF(width * 0.61f, 0f, width, height * 0.24f)
    val maxCoveredFraction = 2f / 3f

    fun overlapsIfFits(candidate: CoverPlacement): List<Float>? {
        val area = candidate.side * candidate.side
        if (overlapArea(candidate, dateZone) > area * 0.04f) return null
        val overlaps = placements.map { overlapArea(candidate, it) }
        if (overlaps.sum() > area * maxCoveredFraction) return null
        if (overlaps.indices.any { previous ->
                val previousArea = placements[previous].side * placements[previous].side
                overlaps[previous] > minOf(area, previousArea) * maxCoveredFraction ||
                    coveredAreas[previous] + overlaps[previous] > previousArea * maxCoveredFraction
            }
        ) return null
        return overlaps
    }

    repeat(count) {
        val naturalSide = base * (0.78f + random.nextFloat() * 0.66f)
        var chosen: CoverPlacement? = null
        var chosenOverlaps = emptyList<Float>()

        // Reduce only a crowded cover's size; all positions remain stable for this date and song list.
        for (sizeStep in 0..16) {
            val side = naturalSide * 0.86f.pow(sizeStep)
            for (attempt in 0 until 160) {
                val candidate = CoverPlacement(
                    x = side / 2f + random.nextFloat() * (width - side),
                    y = side / 2f + random.nextFloat() * (height - side),
                    side = side,
                    rotation = (random.nextFloat() - 0.5f) * 12f,
                )
                val overlaps = overlapsIfFits(candidate) ?: continue

                chosen = candidate
                chosenOverlaps = overlaps
                break
            }
            if (chosen != null) break
        }

        if (chosen == null) {
            // A tiny last-resort cover still has to pass the same overlap limit.
            fallback@ for (row in 0 until 64) {
                for (column in 0 until 64) {
                    val candidate = CoverPlacement(
                        x = (column + 0.5f) * width / 64f,
                        y = (row + 0.5f) * height / 64f,
                        side = 1f,
                        rotation = 0f,
                    )
                    val overlaps = overlapsIfFits(candidate) ?: continue
                    chosen = candidate
                    chosenOverlaps = overlaps
                    break@fallback
                }
            }
        }
        val placement = chosen ?: return placements
        chosenOverlaps.forEachIndexed { previous, overlap -> coveredAreas[previous] += overlap }
        placements += placement
        coveredAreas += chosenOverlaps.sum()
    }
    return placements
}

private fun overlapArea(a: CoverPlacement, b: CoverPlacement): Float {
    // The bound includes the corners of covers rotated by up to six degrees.
    val aHalf = a.side * 0.56f
    val bHalf = b.side * 0.56f
    val x = (minOf(a.x + aHalf, b.x + bHalf) - maxOf(a.x - aHalf, b.x - bHalf)).coerceAtLeast(0f)
    val y = (minOf(a.y + aHalf, b.y + bHalf) - maxOf(a.y - aHalf, b.y - bHalf)).coerceAtLeast(0f)
    return x * y
}

private fun overlapArea(a: CoverPlacement, b: RectF): Float {
    val half = a.side * 0.56f
    val x = (minOf(a.x + half, b.right) - maxOf(a.x - half, b.left)).coerceAtLeast(0f)
    val y = (minOf(a.y + half, b.bottom) - maxOf(a.y - half, b.top)).coerceAtLeast(0f)
    return x * y
}

private fun squareCoverBitmap(source: Bitmap, side: Int): Bitmap {
    val output = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    val sourceSide = minOf(source.width, source.height)
    val left = (source.width - sourceSide) / 2
    val top = (source.height - sourceSide) / 2
    val canvas = android.graphics.Canvas(output)
    val rounded = android.graphics.Path().apply {
        addRoundRect(RectF(0f, 0f, side.toFloat(), side.toFloat()),
            side * 0.05f, side * 0.05f, android.graphics.Path.Direction.CW)
    }
    canvas.clipPath(rounded)
    canvas.drawBitmap(
        source,
        Rect(left, top, left + sourceSide, top + sourceSide),
        Rect(0, 0, side, side),
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG),
    )
    return output
}

private fun artworkHue(bitmap: Bitmap): Float {
    val hsv = FloatArray(3)
    var xTotal = 0.0
    var yTotal = 0.0
    var saturation = 0f
    var brightness = 0f
    repeat(5) { row ->
        repeat(5) { column ->
            val color = bitmap.getPixel((column * 2 + 1) * bitmap.width / 10,
                (row * 2 + 1) * bitmap.height / 10)
            android.graphics.Color.colorToHSV(color, hsv)
            val weight = hsv[1].coerceAtLeast(0.1f).toDouble()
            xTotal += cos(hsv[0] * PI / 180) * weight
            yTotal += sin(hsv[0] * PI / 180) * weight
            saturation += hsv[1]
            brightness += hsv[2]
        }
    }
    if (saturation / 25f < 0.16f) return 360f + brightness / 25f * 20f
    return ((atan2(yTotal, xTotal) * 180 / PI + 360) % 360).toFloat()
}
