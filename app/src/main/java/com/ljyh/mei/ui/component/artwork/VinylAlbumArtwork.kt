package com.ljyh.mei.ui.component.artwork

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.ui.component.shimmer.skeleton

/** Adds a 20% exposed disc while keeping its entire center label behind the sleeve. */
internal const val VinylAlbumArtworkRatio = 1.20f

/** Shared physical-album silhouette for full cards, compact rows, and their skeletons. */
@Composable
internal fun VinylAlbumArtwork(
    coverUrl: String?,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val labelColor = MaterialTheme.colorScheme.secondaryContainer
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(VinylAlbumArtworkRatio)) {
        val coverSize = maxHeight
        val sleeveShape = RoundedCornerShape((coverSize * .055f).coerceIn(3.dp, 8.dp))
        val sleeveShadow = (coverSize * .02f).coerceIn(1.dp, 3.dp)
        // Paint the complete disc behind the sleeve; the right edge remains inside the item bounds.
        Box(
            Modifier.align(Alignment.CenterEnd).size(coverSize * .94f)
                .shadow(2.dp, CircleShape)
                .skeleton(isLoading, CircleShape),
        ) {
            if (!isLoading) Canvas(Modifier.matchParentSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF141416), Color(0xFF36363B), Color(0xFF161618),
                            Color(0xFF111113), Color(0xFF35353A), Color(0xFF141416),
                        ),
                        center = center,
                    ),
                    radius = radius, center = center,
                )
                drawCircle(Color.White.copy(alpha = .14f), radius - .5.dp.toPx(), center,
                    style = Stroke(.75.dp.toPx()))
                val grooveCount = if (coverSize < 80.dp) 10 else 20
                repeat(grooveCount) { index ->
                    drawCircle(
                        Color.White.copy(alpha = if (index % 3 == 0) .10f else .045f),
                        radius = radius * (.40f + index * .53f / (grooveCount - 1)), center = center,
                        style = Stroke(.5.dp.toPx()),
                    )
                }
                drawCircle(labelColor, radius * .38f, center)
                drawCircle(Color.Black.copy(alpha = .18f), radius * .32f, center,
                    style = Stroke(.65.dp.toPx()))
                drawCircle(Color(0xFF101012), radius * .045f, center)
            }
        }
        Box(
            Modifier.align(Alignment.CenterStart).size(coverSize)
                .shadow(sleeveShadow, sleeveShape)
                .clip(sleeveShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .skeleton(isLoading, sleeveShape),
            contentAlignment = Alignment.Center,
        ) {
            if (!isLoading) {
                Icon(
                    Icons.Rounded.Album, null, Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!coverUrl.isNullOrBlank()) AsyncImage(
                    model = coverUrl, contentDescription = null,
                    modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
