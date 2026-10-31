package com.ljyh.mei.ui.screen.artist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntRect
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.model.Album

internal data class ArtistDetailUiState(
    val id: String,
    val artist: ArtistDetail.Data.Artist?,
    val expertIdentities: List<ArtistDetail.Data.SecondaryExpertIdentiy>,
    val detailLoading: Boolean,
    val detailError: String?,
    val songs: List<MediaMetadata>,
    val songsLoading: Boolean,
    val songsError: String?,
    val albums: List<Album>,
    val albumsLoading: Boolean,
    val albumsError: String?,
    val coverColor: Color?,
    val canPlay: Boolean,
)
internal data class ArtistDetailActions(
    val onBack: () -> Unit,
    val onBrowseSongs: () -> Unit,
    val onBrowseAlbums: () -> Unit,
    val onPlayHotSongs: () -> Unit,
    val onTrackClick: (MediaMetadata, Int) -> Unit,
    val onTrackMore: (MediaMetadata, IntRect) -> Unit,
    val onAlbumClick: (Long) -> Unit,
    val onRetryDetail: () -> Unit,
    val onRetrySongs: () -> Unit,
    val onRetryAlbums: () -> Unit,
)

internal fun artistDisplayAliases(artist: ArtistDetail.Data.Artist): List<String> =
    (artist.transNames + artist.alias.mapNotNull { value ->
        (value as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content
    }).filter(String::isNotBlank).distinct()
