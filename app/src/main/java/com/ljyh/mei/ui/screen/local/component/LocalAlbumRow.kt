package com.ljyh.mei.ui.screen.local.component

import com.ljyh.mei.ui.component.card.ArtworkCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.ui.component.layout.meiPageGutter

@Composable
internal fun AlbumRow(albums: List<String>, songs: List<Song>, onAlbumClick: (String) -> Unit = {}) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = meiPageGutter()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums) { album ->
            val albumSongs = songs.filter { it.album == album }
            val cover = albumSongs.firstOrNull { it.cover.isNotEmpty() }?.cover
            AlbumCard(album, "${albumSongs.size} 首", cover, onClick = { onAlbumClick(album) })
        }
    }
}

@Composable
internal fun AlbumCard(title: String, count: String, coverUrl: String?, onClick: () -> Unit = {}) {
    ArtworkCard(
        title = title, coverUrl = coverUrl, subtitle = count, modifier = Modifier.width(120.dp),
        resizeImage = false, cornerRadius = 10.dp, textSpacing = 6.dp, titleMaxLines = 1,
        titleStyle = MaterialTheme.typography.labelMedium, subtitleStyle = MaterialTheme.typography.labelSmall,
        placeholder = {
            Icon(Icons.Rounded.Album, null, modifier = Modifier.align(Alignment.Center).size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        },
        onClick = onClick,
    )
}
