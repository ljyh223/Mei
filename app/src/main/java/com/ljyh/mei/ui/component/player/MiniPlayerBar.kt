package com.ljyh.mei.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.ljyh.mei.constants.MiniPlayerBarHeight
import com.ljyh.mei.constants.MiniPlayerCoverCornerRadius
import com.ljyh.mei.constants.MiniPlayerCoverSize
import com.ljyh.mei.ui.component.glass.MeiGlassRole
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.component.glass.LocalMeiGlassAppearance
import com.ljyh.mei.utils.image.smallImage
import com.ljyh.mei.ui.component.layout.meiPageGutter

@Composable
fun MiniPlayerBarContent(
    title: String?,
    artist: String?,
    coverUrl: String?,
    isPlaying: Boolean,
    canSkipNext: Boolean,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    drawCover: Boolean = true,
    bottomInset: Dp = 0.dp,
    compactProgress: Float = 0f,
) {
    val compact = compactProgress.coerceIn(0f, 1f)
    val horizontalMargin = miniPlayerHorizontalMargin(meiPageGutter(), compact)
    val nextWidth = lerp(48.dp, 0.dp, compact)
    Column(modifier = modifier.fillMaxWidth()) {
        MeiGlassSurface(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = horizontalMargin)
                .height(MiniPlayerBarHeight),
            shape = RoundedCornerShape(MiniPlayerBarHeight / 2),
            role = MeiGlassRole.MiniPlayer,
        ) {
            // Insets stay outside the floating surface; cover coordinates remain sheet-local.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MiniPlayerBarHeight)
                    .clickable(onClick = onClick)
                    .padding(start = 8.dp, end = 2.dp),
            ) {
                MiniPlayerSongContent(
                    title = title.orEmpty(),
                    artist = artist.orEmpty(),
                    coverUrl = coverUrl,
                    drawCover = drawCover,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Box(
                    modifier = Modifier
                        .width(nextWidth)
                        .height(48.dp)
                        .clipToBounds()
                        .alpha(1f - compact),
                ) {
                    IconButton(
                        onClick = onNext,
                        enabled = canSkipNext && compact < 0.5f,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "下一首",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (canSkipNext) 1f else 0.38f,
                            ),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(bottomInset))
    }
}

@Composable
private fun MiniPlayerSongContent(
    title: String,
    artist: String,
    coverUrl: String?,
    modifier: Modifier = Modifier,
    drawCover: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(MiniPlayerCoverSize)
                .clip(RoundedCornerShape(MiniPlayerCoverCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (drawCover && coverUrl != null) {
                val thumbnailUrl = coverUrl.smallImage()
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl)
                        .memoryCacheKey(thumbnailUrl)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(MiniPlayerCoverSize),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
            if (LocalDensity.current.fontScale <= 1.4f) Text(
                text = artist,
                color = if (LocalMeiGlassAppearance.current.strongContent) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Shared by the mini surface and the artwork that morphs into the full player. */
fun miniPlayerHorizontalMargin(pageGutter: Dp, compactProgress: Float): Dp =
    lerp(pageGutter, 80.dp, compactProgress.coerceIn(0f, 1f))
