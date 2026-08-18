package com.ljyh.mei.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.ripple
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val CapsuleCornerRadius = 24.dp
private val CapsuleExtraSlide = 16.dp

@Composable
fun FloatingCapsuleNavigationBar(
    shouldShow: Boolean,
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleProgress by animateFloatAsState(
        targetValue = if (shouldShow) 1f else 0f,
        animationSpec = NavigationBarAnimationFloatSpec,
        label = "navCapsule"
    )

    if (visibleProgress <= 0f) return

    val slideOffset = (FloatingCapsuleNavHeight + CapsuleExtraSlide) * (1f - visibleProgress)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FloatingCapsuleHorizontalPadding)
            .offset { IntOffset(0, slideOffset.roundToPx()) }
            .graphicsLayer {
                alpha = visibleProgress.coerceIn(0f, 1f)
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
                        .height(FloatingCapsuleNavHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 24.dp),
                        ) { onTabSelect(screen) },
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
fun FloatingCapsulePlayerBarContent(
    title: String?,
    artist: String?,
    coverUrl: String?,
    isPlaying: Boolean,
    canSkipNext: Boolean,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(FloatingCapsuleMiniPlayerHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp)
    ) {
        MiniPlayerSongContent(
            title = title.orEmpty(),
            artist = artist.orEmpty(),
            coverUrl = coverUrl,
            modifier = Modifier.weight(1f),
        )

        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
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
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FloatingCapsuleMiniPlayer(
    shouldShow: Boolean,
    hideProgress: Float = 0f,
    isPlaying: Boolean,
    canSkipNext: Boolean,
    title: String?,
    artist: String?,
    coverUrl: String?,
    nextTitle: String?,
    nextArtist: String?,
    nextCoverUrl: String?,
    prevTitle: String?,
    prevArtist: String?,
    prevCoverUrl: String?,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
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

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }
    val contentWidthPx = with(density) { 360.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    val currentInfo = remember(title, artist, coverUrl) { Triple(title ?: "", artist ?: "", coverUrl) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FloatingCapsuleHorizontalPadding)
            .offset { IntOffset(0, slideOffset.roundToPx()) }
            .graphicsLayer {
                val hideAlpha = if (hideProgress < 0.3f) 1f else ((1f - hideProgress) / 0.7f).coerceIn(0f, 1f)
                alpha = (hideAlpha * visibleProgress).coerceIn(0f, 1f)
            },
        shape = RoundedCornerShape(CapsuleCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FloatingCapsuleMiniPlayerHeight)
                .clip(RoundedCornerShape(CapsuleCornerRadius))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            scope.launch { offsetX.stop() }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                offsetX.snapTo(
                                    (offsetX.value + dragAmount).coerceIn(-contentWidthPx, contentWidthPx)
                                )
                            }
                        },
                        onDragEnd = {
                            val current = offsetX.value
                            scope.launch {
                                if (current <= -swipeThresholdPx) {
                                    offsetX.animateTo(-contentWidthPx, spring(stiffness = Spring.StiffnessMedium))
                                    onNext()
                                    offsetX.snapTo(0f)
                                } else if (current >= swipeThresholdPx) {
                                    offsetX.animateTo(contentWidthPx, spring(stiffness = Spring.StiffnessMedium))
                                    onPrevious()
                                    offsetX.snapTo(0f)
                                } else {
                                    offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                }
                            }
                        }
                    )
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FloatingCapsuleMiniPlayerHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                    .padding(horizontal = 12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    MiniPlayerSongContent(
                        title = currentInfo.first,
                        artist = currentInfo.second,
                        coverUrl = currentInfo.third,
                        modifier = Modifier.graphicsLayer {
                            translationX = offsetX.value
                            alpha = (1f - kotlin.math.abs(offsetX.value) / contentWidthPx).coerceIn(0f, 1f)
                        }
                    )

                    if (offsetX.value < 0f) {
                        MiniPlayerSongContent(
                            title = nextTitle ?: "",
                            artist = nextArtist ?: "",
                            coverUrl = nextCoverUrl,
                            modifier = Modifier.graphicsLayer {
                                translationX = offsetX.value + contentWidthPx
                                alpha = (-offsetX.value / contentWidthPx).coerceIn(0f, 1f)
                            }
                        )
                    }

                    if (offsetX.value > 0f) {
                        MiniPlayerSongContent(
                            title = prevTitle ?: "",
                            artist = prevArtist ?: "",
                            coverUrl = prevCoverUrl,
                            modifier = Modifier.graphicsLayer {
                                translationX = offsetX.value - contentWidthPx
                                alpha = (offsetX.value / contentWidthPx).coerceIn(0f, 1f)
                            }
                        )
                    }
                }

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
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
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerSongContent(
    title: String,
    artist: String,
    coverUrl: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
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
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
            Text(
                text = artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
