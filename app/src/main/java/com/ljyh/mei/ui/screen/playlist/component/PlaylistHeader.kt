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
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.model.PlaylistHeaderStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    headerStyle: PlaylistHeaderStyle = PlaylistHeaderStyle.Standard,
    description: String? = null,
    updateTime: Long = 0,
    titleModifier: Modifier = Modifier,
) {
    val coverStyle by rememberEnumPreference(PlaylistCoverStyleKey, defaultValue = PlaylistCoverStyle.Cover)
    var showCoverPreview by remember(cover) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = meiPageGutter(), vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val artworkSize = playlistArtworkSize(maxWidth)
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
                if (!isLoading) when (
                    if (headerStyle == PlaylistHeaderStyle.Radar) PlaylistCoverStyle.Combination else coverStyle
                ) {
                    PlaylistCoverStyle.Cover -> AsyncImage(
                        model = cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    PlaylistCoverStyle.FirstSongImage -> AsyncImage(
                        model = (coverList.firstOrNull() ?: cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    PlaylistCoverStyle.Combination -> {
                        if (coverList.size < 5) AsyncImage(
                            model = cover,
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
            modifier = titleModifier.skeleton(isLoading),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        if (creator.isNotBlank() || isLoading || headerStyle == PlaylistHeaderStyle.Radar) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isLoading) "歌单创建者"
                    else if (headerStyle == PlaylistHeaderStyle.Radar) playlistUpdateLabel(updateTime)
                    else creator,
                modifier = Modifier.skeleton(isLoading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        if (headerStyle == PlaylistHeaderStyle.Radar && !description.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isLoading) "正在加载歌曲信息"
            else "$count 首 · 播放 ${compactPlaylistCount(playCount)} · 收藏 ${compactPlaylistCount(subscribeCount)}",
            modifier = Modifier.skeleton(isLoading),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        PlaylistHeaderActions(
            isSubscribed = isSubscribed,
            onPlayAll = onPlayAll,
            onSubscribed = { onSubscribed(isSubscribed) },
            onDownload = onDownload,
            actionIcon = actionIcon,
            actionLabel = actionLabel,
            isLoading = isLoading,
        )
    }
    if (showCoverPreview && cover.isNotBlank()) FullScreenImageViewer(cover) { showCoverPreview = false }
}

@Composable
internal fun PlaylistHeaderActions(
    isSubscribed: Boolean,
    onPlayAll: () -> Unit,
    onSubscribed: () -> Unit,
    onDownload: () -> Unit,
    actionIcon: ImageVector,
    actionLabel: String,
    isLoading: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeutralIconButton(
            onClick = onSubscribed,
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

internal fun playlistUpdateLabel(updateTime: Long): String =
    if (updateTime > 0) "最近更新 " + Instant.ofEpochMilli(updateTime)
        .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM月dd日"))
    else "每日更新"

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

internal fun playlistArtworkSize(availableWidth: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp =
    if (availableWidth >= 560.dp) 200.dp else (availableWidth * 0.62f).coerceIn(160.dp, 248.dp)
