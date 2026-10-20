package com.ljyh.mei.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.component.glass.MeiGlassRole
import com.ljyh.mei.constants.MiniPlayerHorizontalMargin
import com.ljyh.mei.constants.MiniPlayerBarHeight
import com.ljyh.mei.constants.MiniPlayerCoverSize
import com.ljyh.mei.constants.ThumbnailCornerRadius
import com.ljyh.mei.utils.image.smallImage

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
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MeiGlassSurface(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = MiniPlayerHorizontalMargin)
                .height(MiniPlayerBarHeight),
            shape = RoundedCornerShape(24.dp),
            role = MeiGlassRole.MiniPlayer,
        ) {
            // Insets stay outside the floating surface; cover coordinates remain sheet-local.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MiniPlayerBarHeight)
                    .clickable(
                        onClick = onClick,
                    )
                    .padding(end = 6.dp),
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
                    modifier = Modifier.size(48.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconButton(
                    onClick = onNext,
                    enabled = canSkipNext,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canSkipNext) 1f else 0.38f),
                    )
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
        modifier = modifier.padding(horizontal = 6.dp),
    ) {
        coverUrl?.let {
            Box(
                modifier = Modifier.padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Spacer(modifier = Modifier.size(48.dp))
                if (drawCover) {
                    val thumbnailUrl = it.smallImage()
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbnailUrl)
                            .memoryCacheKey(thumbnailUrl)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(MiniPlayerCoverSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
            if (LocalDensity.current.fontScale <= 1.4f) Text(
                text = artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
