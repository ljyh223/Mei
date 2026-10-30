package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.artwork.PlaylistCoverStage
import com.ljyh.mei.ui.component.dialog.FullScreenImageViewer
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.ui.model.PlaylistHeaderStyle
import com.ljyh.mei.ui.model.UiPlaylist

/** Shared dispatch for both the phone list header and the tablet detail column. */
@Composable
fun PlaylistDetailHeader(
    playlist: UiPlaylist,
    isLoading: Boolean,
    onPlayAll: () -> Unit,
    onHeaderAction: () -> Unit,
    onDownload: () -> Unit,
    actionIcon: ImageVector,
    actionLabel: String,
    compact: Boolean = false,
    loadingHeaderStyle: PlaylistHeaderStyle? = null,
    titleModifier: Modifier = Modifier,
    maxArtworkHeight: Dp? = null,
) {
    if (isLoading && loadingHeaderStyle == null) {
        UnknownPlaylistHeaderSkeleton()
        return
    }
    val style = if (isLoading) loadingHeaderStyle ?: playlist.headerStyle else playlist.headerStyle
    if (style == PlaylistHeaderStyle.Standard || style == PlaylistHeaderStyle.Radar) {
        PlaylistHeader(
            title = playlist.title,
            cover = playlist.cover,
            coverList = playlist.coverList,
            creator = playlist.creatorName,
            count = playlist.count,
            playCount = playlist.playCount ?: 0L,
            subscribeCount = playlist.subscriberCount,
            isSubscribed = playlist.isSubscribed,
            isLoading = isLoading,
            headerStyle = playlist.headerStyle,
            description = playlist.description,
            updateTime = playlist.updateTime,
            titleModifier = titleModifier,
            onPlayAll = onPlayAll,
            onSubscribed = { onHeaderAction() },
            onDownload = onDownload,
            actionIcon = actionIcon,
            actionLabel = actionLabel,
        )
        return
    }

    val liked = style == PlaylistHeaderStyle.Liked
    val widthLimit = if (compact) { if (liked) 360.dp else 280.dp } else 420.dp
    val artworkWidth = maxArtworkHeight?.let { minOf(widthLimit, it / if (liked) 1.02f else 0.65f) } ?: widthLimit
    var preview by remember(playlist.id) { mutableStateOf<String?>(null) }
    val covers = if (isLoading) emptyList() else playlist.coverList.filter(String::isNotBlank).take(3)
        .ifEmpty { listOfNotNull(playlist.cover.takeIf(String::isNotBlank)) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = meiPageGutter()),
            contentAlignment = Alignment.Center,
        ) {
            key(playlist.id) {
                PlaylistCoverStage(
                    covers = covers,
                    liked = liked,
                    onCoverClick = { preview = it },
                    modifier = Modifier.widthIn(max = artworkWidth).fillMaxWidth(),
                    isLoading = isLoading,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.padding(horizontal = meiPageGutter()).then(titleModifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (liked) {
                Icon(Icons.Rounded.Favorite, null, Modifier.size(26.dp).skeleton(isLoading, RoundedCornerShape(50)), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = if (liked && (playlist.isCreator || isLoading)) "我喜欢的音乐"
                    else if (isLoading) "正在加载歌单标题" else playlist.title,
                modifier = Modifier.skeleton(isLoading),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isLoading) "正在加载歌曲信息" else listOfNotNull(
                "${playlist.count} 首",
                playlist.creatorName.takeIf(String::isNotBlank),
                playlist.playCount?.let { "播放 ${compactPlaylistCount(it)} 次" },
            ).joinToString(" · "),
            modifier = Modifier.padding(horizontal = meiPageGutter()).skeleton(isLoading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!liked && (playlist.updateTime > 0 || isLoading)) {
            Spacer(Modifier.height(6.dp))
            Text(
                if (isLoading) "最近更新" else playlistUpdateLabel(playlist.updateTime),
                modifier = Modifier.skeleton(isLoading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = meiPageGutter())) {
            PlaylistHeaderActions(
                isSubscribed = playlist.isSubscribed,
                onPlayAll = onPlayAll,
                onSubscribed = onHeaderAction,
                onDownload = onDownload,
                actionIcon = actionIcon,
                actionLabel = actionLabel,
                isLoading = isLoading,
            )
        }
    }
    preview?.let { FullScreenImageViewer(it) { preview = null } }
}

/**
 * Known special types use their actual cover-stage proportions. Unknown playlists use a neutral
 * placeholder until the detail response identifies their layout.
 */
@Composable
private fun UnknownPlaylistHeaderSkeleton() {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = meiPageGutter()), contentAlignment = Alignment.Center) {
            Spacer(Modifier.size(playlistArtworkSize(maxWidth)).skeleton(true, MaterialTheme.shapes.large))
        }
        Spacer(Modifier.height(20.dp))
        Spacer(Modifier.width(176.dp).height(28.dp).skeleton(true))
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.width(136.dp).height(20.dp).skeleton(true))
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = meiPageGutter())) {
            PlaylistHeaderActions(false, {}, {}, {}, Icons.Rounded.MusicNote, "", isLoading = true)
        }
    }
}


internal fun compactPlaylistCount(count: Long): String = when {
    count >= 100_000_000 -> String.format(java.util.Locale.CHINA, "%.1f亿", count / 100_000_000.0)
    count >= 10_000 -> String.format(java.util.Locale.CHINA, "%.1f万", count / 10_000.0)
    else -> count.toString()
}
