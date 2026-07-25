package com.ljyh.mei.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.FloatingCapsuleHorizontalPadding
import com.ljyh.mei.constants.FloatingCapsuleMiniPlayerHeight
import com.ljyh.mei.constants.FloatingCapsuleNavHeight
import com.ljyh.mei.constants.NavigationBarAnimationFloatSpec
import com.ljyh.mei.constants.ThumbnailCornerRadius
import com.ljyh.mei.ui.screen.Index
import com.ljyh.mei.utils.smallImage
import kotlin.math.roundToInt

private val CapsuleCornerRadius = 24.dp
private val CapsuleExtraSlide = 16.dp

@Composable
fun FloatingCapsuleNavigationBar(
    shouldShow: Boolean,
    hideProgress: Float = 0f,
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleProgress by animateFloatAsState(
        targetValue = if (shouldShow) 1f else 0f,
        animationSpec = NavigationBarAnimationFloatSpec,
        label = "navCapsule"
    )

    if (visibleProgress <= 0f && hideProgress >= 1f) return

    val slideOffset = (FloatingCapsuleNavHeight + CapsuleExtraSlide) * (1f - visibleProgress) +
            (FloatingCapsuleNavHeight + 8.dp) * hideProgress

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FloatingCapsuleHorizontalPadding)
            .offset { IntOffset(0, slideOffset.roundToPx()) }
            .graphicsLayer {
                alpha = ((1f - hideProgress) * visibleProgress).coerceIn(0f, 1f)
            },
        shape = RoundedCornerShape(CapsuleCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FloatingCapsuleNavHeight)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Index.entries.forEach { screen ->
                val isSelected = selectedRoute == screen.route
                val tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelect(screen) }
                        .height(FloatingCapsuleNavHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = screen.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingCapsuleMiniPlayer(
    shouldShow: Boolean,
    hideProgress: Float = 0f,
    progress: Float,
    isPlaying: Boolean,
    canSkipNext: Boolean,
    title: String?,
    artist: String?,
    coverUrl: String?,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleProgress by animateFloatAsState(
        targetValue = if (shouldShow) 1f else 0f,
        animationSpec = NavigationBarAnimationFloatSpec,
        label = "miniPlayerCapsule"
    )

    if (visibleProgress <= 0f && hideProgress >= 1f) return

    val slideOffset = (FloatingCapsuleMiniPlayerHeight + CapsuleExtraSlide) * (1f - visibleProgress) +
            (FloatingCapsuleMiniPlayerHeight + 8.dp) * hideProgress

    val colorScheme = MaterialTheme.colorScheme
    val progressFraction = progress.coerceIn(0f, 1f)
    val strokeWidth = 2.5.dp
    val innerPadding = 3.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FloatingCapsuleHorizontalPadding)
            .offset { IntOffset(0, slideOffset.roundToPx()) }
            .graphicsLayer {
                val hideAlpha = if (hideProgress < 0.5f) 1f else ((1f - hideProgress) * 2f).coerceIn(0f, 1f)
                alpha = (hideAlpha * visibleProgress).coerceIn(0f, 1f)
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FloatingCapsuleMiniPlayerHeight)
                .clip(RoundedCornerShape(CapsuleCornerRadius))
                .clickable(onClick = onClick)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(CapsuleCornerRadius),
                color = colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                tonalElevation = 2.dp,
                shadowElevation = 6.dp,
            ) {}

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cornerRadius = CapsuleCornerRadius.toPx()
                val sw = strokeWidth.toPx()
                val halfSw = sw / 2f
                val outlineRect = Rect(
                    offset = Offset(halfSw, halfSw),
                    size = Size(size.width - sw, size.height - sw)
                )
                val outlinePath = Path().apply {
                    addRoundRect(RoundRect(outlineRect, CornerRadius(cornerRadius - halfSw, cornerRadius - halfSw)))
                }

                val pathMeasure = PathMeasure()
                pathMeasure.setPath(outlinePath, false)
                val totalLength = pathMeasure.length

                drawPath(
                    path = outlinePath,
                    color = colorScheme.onSurface.copy(alpha = 0.06f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw)
                )

                if (progressFraction > 0.001f && totalLength > 0f) {
                    val progressPath = Path()
                    pathMeasure.getSegment(0f, totalLength * progressFraction, progressPath, true)
                    drawPath(
                        path = progressPath,
                        color = colorScheme.primary,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw, cap = StrokeCap.Round)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 9.dp)
            ) {
                coverUrl?.let {
                    AsyncImage(
                        model = it.smallImage(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title ?: "",
                        color = colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = artist ?: "",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onNext,
                    enabled = canSkipNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.onSurface
                    )
                }
            }
        }
    }
}
