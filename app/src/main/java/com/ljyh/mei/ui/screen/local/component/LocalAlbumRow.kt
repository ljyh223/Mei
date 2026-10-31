package com.ljyh.mei.ui.screen.local.component

import com.ljyh.mei.ui.component.card.VinylAlbumCard
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
    VinylAlbumCard(
        title = title, coverUrl = coverUrl, subtitle = count,
        modifier = Modifier.width(120.dp * VinylAlbumArtworkRatio), textSpacing = 6.dp,
        titleStyle = MaterialTheme.typography.labelMedium, subtitleStyle = MaterialTheme.typography.labelSmall,
        onClick = onClick,
    )
}
