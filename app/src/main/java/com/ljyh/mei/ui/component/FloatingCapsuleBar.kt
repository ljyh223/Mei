package com.ljyh.mei.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.MiniPlayerHeight
import com.ljyh.mei.constants.NavigationBarAnimationSpec
import com.ljyh.mei.constants.ThumbnailCornerRadius
import com.ljyh.mei.extensions.togglePlayPause
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Index
import com.ljyh.mei.utils.smallImage
import kotlin.math.roundToInt

private val FloatingCapsuleHorizontalPadding = 24.dp
private val FloatingCapsuleBottomPadding = 12.dp
private val FloatingCapsuleCornerRadius = 28.dp
private val FloatingCapsuleMiniPlayerHeight = 52.dp
private val FloatingCapsuleNavHeight = 56.dp
private val FloatingCapsuleTotalHeight = FloatingCapsuleNavHeight + FloatingCapsuleMiniPlayerHeight

@Composable
fun FloatingCapsuleBar(
    showMiniPlayer: Boolean,
    shouldShow: Boolean,
    bottomInset: Dp,
    playerProgress: Float,
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
    onMiniPlayerClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    isPlaying: Boolean,
    canSkipNext: Boolean,
    songTitle: String?,
    songArtist: String?,
    songCoverUrl: String?,
    modifier: Modifier = Modifier,
) {
    val visibleHeight by animateDpAsState(
        targetValue = if (shouldShow) {
            if (showMiniPlayer) FloatingCapsuleTotalHeight else FloatingCapsuleNavHeight
        } else 0.dp,
        animationSpec = NavigationBarAnimationSpec,
        label = "floatingCapsuleHeight"
    )

    if (visibleHeight <= 0.dp) return

    val totalOffset = bottomInset + FloatingCapsuleBottomPadding + FloatingCapsuleTotalHeight
    val hideOffset = totalOffset * (1f - visibleHeight / (if (showMiniPlayer) FloatingCapsuleTotalHeight else FloatingCapsuleNavHeight))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FloatingCapsuleHorizontalPadding)
            .offset { IntOffset(0, hideOffset.roundToPx()) }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(FloatingCapsuleCornerRadius),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (showMiniPlayer && visibleHeight > FloatingCapsuleNavHeight) {
                    FloatingCapsuleMiniPlayer(
                        progress = playerProgress,
                        isPlaying = isPlaying,
                        canSkipNext = canSkipNext,
                        title = songTitle,
                        artist = songArtist,
                        coverUrl = songCoverUrl,
                        onClick = onMiniPlayerClick,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                    )
                }

                FloatingCapsuleNavigation(
                    selectedRoute = selectedRoute,
                    onTabSelect = onTabSelect,
                )
            }
        }
    }
}

@Composable
private fun FloatingCapsuleMiniPlayer(
    progress: Float,
    isPlaying: Boolean,
    canSkipNext: Boolean,
    title: String?,
    artist: String?,
    coverUrl: String?,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(FloatingCapsuleMiniPlayerHeight)
            .clickable(onClick = onClick)
    ) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(FloatingCapsuleMiniPlayerHeight)
                .padding(horizontal = 12.dp)
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = artist ?: "",
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

@Composable
private fun FloatingCapsuleNavigation(
    selectedRoute: String?,
    onTabSelect: (Index) -> Unit,
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
            val targetColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

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
                    tint = targetColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = screen.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = targetColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
