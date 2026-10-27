package com.ljyh.mei.ui.screen.playlist.component

import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.utils.image.ArtworkSize
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
internal fun DailyCoverWall(covers: List<String>, today: LocalDate) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val surface = MaterialTheme.colorScheme.surface
    val placeholder = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)

    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = meiPageGutter(), vertical = 8.dp)) {
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
            placements.forEachIndexed { index, placement ->
                val url = covers[index]
                key(index, url) {
                    val side = with(density) { placement.side.toDp() }
                    val left = with(density) { (placement.x - placement.side / 2f).toDp() }
                    val top = with(density) { (placement.y - placement.side / 2f).toDp() }
                    val requestSide = ArtworkSize.forPixels((placement.side * 1.15f).roundToInt()).pixels
                    val model = remember(context, url, requestSide) {
                        ImageRequest.Builder(context)
                            .data(url)
                            .size(requestSide)
                            .crossfade(true)
                            .build()
                    }
                    Box(
                        Modifier.offset(x = left, y = top).size(side)
                            .rotate(placement.rotation)
                            .clip(RoundedCornerShape(4.dp))
                            .background(placeholder),
                    ) {
                        if (url.isNotBlank()) {
                            AsyncImage(
                                model = model,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(stageHeight * 0.36f)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, surface))),
            )
            DailyCoverDate(today, Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun DailyCoverDate(today: LocalDate, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
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
