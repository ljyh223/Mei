package com.ljyh.mei.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
fun FloatingCapsuleMiniPlayer(
    shouldShow: Boolean,
    hideProgress: Float = 0f,
    isPlaying: Boolean,
    canSkipNext: Boolean,
    title: String?,
    artist: String?,
    coverUrl: String?,
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

    val offsetX = remember { Animatable(0f) }
    var swipeDirection by remember { mutableStateOf(0) }

    val songInfo = remember(title, artist, coverUrl) {
        Triple(title ?: "", artist ?: "", coverUrl)
    }

    LaunchedEffect(songInfo) {
        if (offsetX.value != 0f) {
            offsetX.snapTo(0f)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FloatingCapsuleHorizontalPadding)
            .offset { IntOffset(0, slideOffset.roundToPx()) }
            .graphicsLayer {
                val hideAlpha = if (hideProgress < 0.5f) 1f else ((1f - hideProgress) * 2f).coerceIn(0f, 1f)
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
                            swipeDirection = if (dragAmount < 0) -1 else 1
                            scope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount)
                            }
                        },
                        onDragEnd = {
                            val current = offsetX.value
                            if (current <= -swipeThresholdPx) {
                                swipeDirection = -1
                                onNext()
                                scope.launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                            } else if (current >= swipeThresholdPx) {
                                swipeDirection = 1
                                onPrevious()
                                scope.launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                            } else {
                                scope.launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                            }
                        }
                    )
                }
        ) {
            AnimatedContent(
                targetState = songInfo,
                transitionSpec = {
                    val direction = if (swipeDirection < 0) -1 else 1
                    if (swipeDirection != 0) {
                        val exit = slideOutHorizontally(tween(250)) { w -> w * direction } + fadeOut(tween(150))
                        val enter = slideInHorizontally(tween(250)) { w -> -w * direction } + fadeIn(tween(150))
                        enter togetherWith exit
                    } else {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FloatingCapsuleMiniPlayerHeight)
                    .graphicsLayer {
                        translationX = offsetX.value
                    },
                label = "songTransition"
            ) { (currentTitle, currentArtist, currentCover) ->
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
                    currentCover?.let {
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
                            text = currentTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = currentArtist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
}
