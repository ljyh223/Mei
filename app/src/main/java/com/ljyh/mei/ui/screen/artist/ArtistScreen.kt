package com.ljyh.mei.ui.screen.artist

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.data.model.domain.toMediaMetadata
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.ui.component.track.BasicTrackActionMenu
import com.ljyh.mei.ui.component.track.TrackMenuTarget
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.screen.Screen
import java.util.UUID
import kotlinx.coroutines.CancellationException

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun ArtistScreen(
    id: String,
    viewModel: ArtistViewModel = hiltViewModel(),
    content: @Composable (ArtistDetailUiState, ArtistDetailActions) -> Unit = { state, actions -> ArtistDetailContent(state, actions) },
) {
    val nav = LocalNavController.current
    val player = LocalPlayerConnection.current
    val details by viewModel.artistDetail.collectAsState()
    val songResult by viewModel.artistSongs.collectAsState()
    val albumResult by viewModel.artistAlbums.collectAsState()
    val artist = (details as? Resource.Success)?.data?.data?.artist
    val artwork = artist?.cover?.takeIf(String::isNotBlank) ?: artist?.avatar?.takeIf(String::isNotBlank)
    var color by remember(artwork) { mutableStateOf(artwork?.let(viewModel::cachedArtistColor)) }
    var menu by remember(id) { mutableStateOf<TrackMenuTarget?>(null) }
    LaunchedEffect(id) {
        viewModel.getArtistDetail(id); viewModel.getArtistAlbums(id); viewModel.getArtistSongs(id)
    }
    LaunchedEffect(artwork) {
        if (artwork != null) try { color = viewModel.artistColor(artwork) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { }
    }
    val songs = remember(songResult) { (songResult as? Resource.Success)?.data?.hotSongs.orEmpty().map { it.toMediaMetadata() } }
    val albums = remember(albumResult) { (albumResult as? Resource.Success)?.data?.hotAlbums.orEmpty().map { album ->
        Album(album.id, album.name, album.picUrl, album.size,
            album.artists.map { Album.Artist(it.id.toLong(), it.name) })
    } }
    val source = QueueSource(QueueSource.Type.ARTIST, id, artist?.name)
    fun play(track: MediaMetadata) {
        player?.onTrackClicked(track.id.toString(), buildQueue = {
            ListQueue(UUID.randomUUID().toString(), "热门单曲",
                songs.map { it.id.toString() to it.toMediaItem() },
                songs.indexOfFirst { it.id == track.id }.coerceAtLeast(0), source = source)
        }, fallbackSource = source)
    }
    content(
        ArtistDetailUiState(
            id, artist, (details as? Resource.Success)?.data?.data?.secondaryExpertIdentiy.orEmpty(),
            details is Resource.Loading, (details as? Resource.Error)?.message,
            songs, songResult is Resource.Loading, (songResult as? Resource.Error)?.message,
            albums, albumResult is Resource.Loading, (albumResult as? Resource.Error)?.message, color, player != null,
        ),
        ArtistDetailActions(
            onBack = { nav.popBackStack() },
            onBrowseSongs = { Screen.ArtistSongs.navigate(nav) { addPath(id) } },
            onBrowseAlbums = { Screen.ArtistAlbums.navigate(nav) { addPath(id) } },
            onPlayHotSongs = { songs.firstOrNull()?.let(::play) },
            onTrackClick = { song, _ -> play(song) },
            onTrackMore = { song, anchor -> menu = TrackMenuTarget(song, anchor, source) },
            onAlbumClick = { album -> Screen.Album.navigate(nav) { addPath(album.toString()) } },
            onRetryDetail = { viewModel.getArtistDetail(id) },
            onRetrySongs = { viewModel.getArtistSongs(id) },
            onRetryAlbums = { viewModel.getArtistAlbums(id) },
        ),
    )
    BasicTrackActionMenu(menu) { menu = null }
}
