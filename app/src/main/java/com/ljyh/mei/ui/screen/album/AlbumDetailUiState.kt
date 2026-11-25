package com.ljyh.mei.ui.screen.album

import com.ljyh.mei.constants.AlbumArtworkMode
import com.ljyh.mei.constants.AlbumModelMode
import com.ljyh.mei.data.repository.DynamicCover
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntRect
import com.ljyh.mei.data.model.domain.AlbumOverview
import com.ljyh.mei.data.model.domain.MediaMetadata

internal data class AlbumDetailUiState(
    val album: AlbumOverview,
    val coverColor: Color?,
    val songs: List<MediaMetadata>,
    val displayedSongs: List<MediaMetadata>,
    val isSubscribed: Boolean?,
    val isSubscriptionLoading: Boolean,
    val canSubscribe: Boolean,
    val canPlay: Boolean,
    val searchQuery: String,
    val isSearchActive: Boolean,
    val artworkMode: AlbumArtworkMode = AlbumArtworkMode.Auto,
    val motionArtwork: DynamicCover? = null,
    val modelMode: AlbumModelMode = AlbumModelMode.Auto,
)
internal data class AlbumDetailActions(
    val onSearchQueryChange: (String) -> Unit,
    val onSearchActiveChange: (Boolean) -> Unit,
    val onBack: () -> Unit,
    val onArtistClick: (Long) -> Unit,
    val onPlayAll: () -> Unit,
    val onShuffle: () -> Unit,
    val onSubscribe: () -> Unit,
    val onDownload: () -> Unit,
    val onTrackClick: (MediaMetadata, Int) -> Unit,
    val onTrackMore: (MediaMetadata, IntRect) -> Unit,
    val onArtworkModeChange: (AlbumArtworkMode) -> Unit = {},
    val onArtworkError: () -> Unit = {},
    val onModelModeChange: (AlbumModelMode) -> Unit = {},
    val onStaticArtworkChanged: suspend (String, coil3.Image?) -> Color? = { _, _ -> null },
)

internal fun albumDisplayArtists(album: AlbumOverview, songs: List<MediaMetadata>): List<MediaMetadata.Artist> {
    val albumIds = album.artistIds.toSet()
    val guests = songs.flatMap { it.artists }.filter { it.name.isNotBlank() && it.id !in albumIds }
        .distinctBy { if (it.id > 0) it.id.toString() else it.name }
    return album.artistNames.mapIndexed { index, name -> MediaMetadata.Artist(album.artistIds.getOrNull(index) ?: 0L, name) }
        .filter { it.name.isNotBlank() } + guests
}

internal fun albumPackageTracks(songs: List<MediaMetadata>) = songs.mapIndexed { index, song ->
    com.ljyh.mei.ui.component.artwork.album.AlbumPackageTrack(index + 1, song.title, song.duration)
}
