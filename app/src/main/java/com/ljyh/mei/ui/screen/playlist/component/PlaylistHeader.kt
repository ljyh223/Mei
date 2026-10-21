package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.PlaylistCoverStyle
import com.ljyh.mei.constants.PlaylistCoverStyleKey
import com.ljyh.mei.ui.component.dialog.FullScreenImageViewer
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.utils.image.largeImage
import com.ljyh.mei.utils.preferences.rememberEnumPreference

/**
 * Centered playlist hero. The page deliberately keeps its action buttons on neutral
 * surfaces: Haze samples the browsing layer, so a glass surface inside that layer
 * would end up sampling itself.
 */
@Composable
fun PlaylistHeader(
    title: String,
    count: Int,
    playCount: Long,
    subscribeCount: Long,
    cover: String,
    coverList: List<String>,
    creator: String,
    isSubscribed: Boolean,
    onPlayAll: () -> Unit,
    onSubscribed: (Boolean) -> Unit,
    onDownload: () -> Unit = {},
    actionIcon: ImageVector,
    actionLabel: String,
    isLoading: Boolean = false,
) {
    val coverStyle by rememberEnumPreference(PlaylistCoverStyleKey, defaultValue = PlaylistCoverStyle.Cover)
    var showCoverPreview by remember(cover) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val artworkSize = if (maxWidth >= 560.dp) 200.dp
            else (maxWidth * 0.62f).coerceIn(160.dp, 248.dp)
            Box(
                modifier = Modifier
                    .size(artworkSize)
                    .align(Alignment.Center)
                    .shadow(8.dp, MaterialTheme.shapes.large)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .combinedClickable(
                        enabled = !isLoading && cover.isNotBlank(),
                        onClickLabel = "预览歌单封面",
                        onLongClickLabel = "预览歌单封面",
                        onClick = { showCoverPreview = true },
                        onLongClick = { showCoverPreview = true },
                    )
                    .skeleton(isLoading, MaterialTheme.shapes.large),
            ) {
                if (!isLoading) when (coverStyle) {
                    PlaylistCoverStyle.Cover -> AsyncImage(
                        model = cover.largeImage(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    PlaylistCoverStyle.FirstSongImage -> AsyncImage(
                        model = (coverList.firstOrNull() ?: cover).largeImage(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    PlaylistCoverStyle.Combination -> {
                        if (coverList.size < 5) AsyncImage(
                            model = cover.largeImage(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        ) else FinalPerfectCollage(imageUrls = coverList, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = if (isLoading) "正在加载的歌单标题" else title,
            modifier = Modifier.skeleton(isLoading),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        if (creator.isNotBlank() || isLoading) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isLoading) "歌单创建者" else creator,
                modifier = Modifier.skeleton(isLoading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isLoading) "正在加载歌曲信息"
            else "$count 首 · 播放 $playCount · 收藏 $subscribeCount",
            modifier = Modifier.skeleton(isLoading),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeutralIconButton(
                onClick = { onSubscribed(isSubscribed) },
                enabled = !isLoading,
                contentDescription = actionLabel,
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (isSubscribed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(16.dp))

            Button(
                onClick = onPlayAll,
                enabled = !isLoading,
                modifier = Modifier.widthIn(min = 148.dp).heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("播放全部")
            }

            Spacer(Modifier.width(16.dp))

            NeutralIconButton(
                onClick = onDownload,
                enabled = !isLoading,
                contentDescription = "下载歌单",
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (showCoverPreview && cover.isNotBlank()) FullScreenImageViewer(cover) { showCoverPreview = false }
}

@Composable
private fun NeutralIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .semantics { this.contentDescription = contentDescription },
        content = content,
    )
}
