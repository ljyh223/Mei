package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.screen.main.library.LibraryDashboardSelection

/** Fixed-height artwork group with a wide upper cover and two smaller covers below. */
@Composable
internal fun LibraryDashboard(
    selection: LibraryDashboardSelection,
    likedArtwork: String?,
    preparingPlayback: Boolean,
    canPlay: Boolean,
    loadingRecent: Boolean,
    recentSyncFailed: Boolean,
    onOpenPlaylist: (String) -> Unit,
    onPlayLiked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stacked = maxWidth < 300.dp || fontScale > 1.3f
        val height = (if (maxWidth >= 600.dp) 260.dp else 220.dp) * fontScale.coerceIn(1f, 1.6f)
        val likedImage: @Composable (Modifier) -> Unit = { imageModifier ->
            LibraryLikedPlaylist(selection.liked, likedArtwork, preparingPlayback, canPlay, imageModifier,
                onOpen = { selection.liked?.let { onOpenPlaylist(it.id) } },
                onPlay = { selection.liked?.let { onPlayLiked(it.id) } })
        }
        val tiles: @Composable (Modifier) -> Unit = { tilesModifier ->
            Column(tilesModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlaylistCoverTile(selection.topCreated,
                    Modifier.fillMaxWidth().weight(1f), compact = false,
                    emptyText = "暂无自建歌单", onOpenPlaylist = onOpenPlaylist)
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(2) { index ->
                        PlaylistCoverTile(selection.recentCollected.getOrNull(index),
                            Modifier.weight(1f).fillMaxHeight(), compact = true,
                            emptyText = when { loadingRecent -> "同步中"; recentSyncFailed -> "未能同步"; else -> "暂无收藏歌单" },
                            onOpenPlaylist = onOpenPlaylist)
                    }
                }
            }
        }
        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            likedImage(Modifier.fillMaxWidth().height(height))
            tiles(Modifier.fillMaxWidth().height(height))
        } else Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            likedImage(Modifier.weight(1.5f).fillMaxHeight())
            tiles(Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun PlaylistCoverTile(
    playlist: Playlist?,
    modifier: Modifier,
    compact: Boolean,
    emptyText: String,
    onOpenPlaylist: (String) -> Unit,
) {
    Box(modifier.clip(RoundedCornerShape(if (compact) 12.dp else 20.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .semantics { contentDescription = playlist?.title ?: emptyText }
        .clickable(enabled = playlist != null, role = Role.Button) { playlist?.let { onOpenPlaylist(it.id) } }) {
        Icon(Icons.Rounded.MusicNote, null, Modifier.align(Alignment.Center).size(if (compact) 24.dp else 32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!playlist?.cover.isNullOrBlank()) AsyncImage(
            model = playlist.cover, contentDescription = null,
            modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop,
        )
    }
}
