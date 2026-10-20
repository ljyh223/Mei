package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.PlaylistCoverStyle
import com.ljyh.mei.constants.PlaylistCoverStyleKey
import com.ljyh.mei.ui.component.MeiCollectionHeader
import com.ljyh.mei.ui.component.dialog.FullScreenImageViewer
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.utils.image.largeImage
import com.ljyh.mei.utils.preferences.rememberEnumPreference

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
    Column(Modifier.fillMaxWidth()) {
        MeiCollectionHeader(
            title = if (isLoading) "正在加载的歌单标题" else title,
            titleModifier = Modifier.skeleton(isLoading),
            cover = {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .combinedClickable(
                            enabled = !isLoading && cover.isNotBlank(),
                            onClickLabel = "预览歌单封面",
                            onLongClickLabel = "预览歌单封面",
                            onClick = { showCoverPreview = true },
                            onLongClick = { showCoverPreview = true },
                        ).skeleton(isLoading, MaterialTheme.shapes.large),
                ) {
                    if (!isLoading) when (coverStyle) {
                        PlaylistCoverStyle.Cover -> AsyncImage(
                            model = cover.largeImage(), contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        )
                        PlaylistCoverStyle.FirstSongImage -> AsyncImage(
                            model = (coverList.firstOrNull() ?: cover).largeImage(), contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        )
                        PlaylistCoverStyle.Combination -> {
                            if (coverList.size < 5) AsyncImage(
                                model = cover.largeImage(), contentDescription = null,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                            ) else FinalPerfectCollage(imageUrls = coverList, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            },
            metadata = {
                if (creator.isNotBlank() || isLoading) Text(
                    if (isLoading) "歌单创建者" else creator,
                    modifier = Modifier.skeleton(isLoading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (isLoading) "正在加载歌曲信息" else "$count 首 · 播放 $playCount · 收藏 $subscribeCount",
                    modifier = Modifier.skeleton(isLoading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onPlayAll, enabled = !isLoading,
                modifier = Modifier.widthIn(min = 144.dp).heightIn(min = 48.dp)
                    .skeleton(isLoading, MaterialTheme.shapes.extraLarge),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("播放全部")
            }
            FilledTonalButton(
                onClick = { onSubscribed(isSubscribed) }, enabled = !isLoading,
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Icon(actionIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(actionLabel)
            }
            FilledTonalIconButton(onClick = onDownload, enabled = !isLoading, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Download, contentDescription = "下载歌单", modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (showCoverPreview && cover.isNotBlank()) FullScreenImageViewer(cover) { showCoverPreview = false }
}
