package com.ljyh.mei.ui.screen.artist

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.data.model.domain.toMediaMetadata
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.ui.component.card.VinylAlbumCard
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.collection.MeiCollectionPage
import com.ljyh.mei.ui.component.collection.MeiCollectionLoadMore
import com.ljyh.mei.ui.component.track.Track
import com.ljyh.mei.ui.component.track.TrackPlaceholder
import com.ljyh.mei.ui.component.track.BasicTrackActionMenu
import com.ljyh.mei.ui.component.track.TrackMenuTarget
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Screen
import java.util.UUID
import com.ljyh.mei.ui.component.glass.MeiGlassIconButton

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistSongsScreen(id: String, viewModel: ArtistCatalogViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current
    val songs = viewModel.songs.collectAsLazyPagingItems()
    val searchState by viewModel.songSearchState.collectAsStateWithLifecycle()
    val source = remember(id) { QueueSource(QueueSource.Type.ARTIST, id) }
    var menuTrack by remember { mutableStateOf<TrackMenuTarget?>(null) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val normalScroll = rememberLazyGridState()
    val searchScroll = rememberLazyGridState()
    val searching = isSearchActive && searchQuery.isNotBlank()
    val currentSearch = searchState.takeIf { it.query == searchQuery.trim() }
        ?: ArtistCatalogSearchState<ArtistSong.HotSong>(query = searchQuery.trim(), isSearching = true)
    val focusManager = LocalFocusManager.current
    fun closeSearch() { focusManager.clearFocus(); searchQuery = ""; isSearchActive = false }
    BackHandler(enabled = isSearchActive, onBack = ::closeSearch)
    LaunchedEffect(searchQuery) { viewModel.searchSongs(searchQuery) }
    val playSong: (ArtistSong.HotSong, List<ArtistSong.HotSong>) -> Unit = { song, loaded ->
        val startIndex = loaded.indexOfFirst { it.id == song.id }
        if (startIndex >= 0) playerConnection?.onTrackClicked(
            trackId = song.id.toString(),
            fallbackSource = source,
            buildQueue = {
                ListQueue(
                    id = "artist_${id}_${UUID.randomUUID()}",
                    title = "全部歌曲",
                    items = loaded.map { it.id.toString() to it.toMediaMetadata().toMediaItem() },
                    startIndex = startIndex,
                    source = source,
                )
            },
        )
    }

    MeiCollectionPage(
        title = "全部歌曲",
        itemCount = if (searching) currentSearch.matches.size else songs.itemCount,
        itemKey = if (searching) ({ index -> currentSearch.matches[index].id }) else songs.itemKey { it.id },
        onNavigateBack = { if (isSearchActive) closeSearch() else navController.popBackStack() },
        onRetry = { if (searching) viewModel.searchSongs(searchQuery) else songs.retry() },
        state = if (searching) searchScroll else normalScroll,
        loading = if (searching) currentSearch.isSearching else songs.loadState.refresh is LoadState.Loading,
        failed = if (searching) currentSearch.error != null else songs.loadState.refresh is LoadState.Error,
        emptyText = if (searching) "没有找到匹配的歌曲" else "暂无歌曲", emptyActionLabel = null,
        horizontalContentPadding = 0.dp, verticalItemSpacing = 0.dp,
        showLargeTitle = !isSearchActive,
        titleContent = if (isSearchActive) ({ ArtistCatalogSearchField(searchQuery, { searchQuery = it }, "搜索歌手歌曲") }) else null,
        actions = { CatalogSearchAction(isSearchActive) { if (isSearchActive) closeSearch() else isSearchActive = true } },
        footer = {
            if (searching) CatalogSearchStatus(currentSearch) { viewModel.searchSongs(searchQuery) }
            else CatalogAppendState(songs)
        },
    ) { index ->
        val song = if (searching) currentSearch.matches[index] else songs[index]
        if (song == null) TrackPlaceholder(index = index) else ArtistSongItem(
            song = song,
            onClick = { playSong(song, if (searching) currentSearch.matches else songs.itemSnapshotList.items) },
            onMore = { track, anchor -> menuTrack = TrackMenuTarget(track, anchor, source) },
        )
    }
    BasicTrackActionMenu(menuTrack) { menuTrack = null }
}

@Composable
fun ArtistAlbumsScreen(id: String, viewModel: ArtistCatalogViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    val albums = viewModel.albums.collectAsLazyPagingItems()
    val searchState by viewModel.albumSearchState.collectAsStateWithLifecycle()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val normalScroll = rememberLazyGridState()
    val searchScroll = rememberLazyGridState()
    val searching = isSearchActive && searchQuery.isNotBlank()
    val currentSearch = searchState.takeIf { it.query == searchQuery.trim() }
        ?: ArtistCatalogSearchState<ArtistAlbum.HotAlbum>(query = searchQuery.trim(), isSearching = true)
    val focusManager = LocalFocusManager.current
    fun closeSearch() { focusManager.clearFocus(); searchQuery = ""; isSearchActive = false }
    BackHandler(enabled = isSearchActive, onBack = ::closeSearch)
    LaunchedEffect(searchQuery) { viewModel.searchAlbums(searchQuery) }
    MeiCollectionPage(
        title = "全部专辑",
        itemCount = if (searching) currentSearch.matches.size else albums.itemCount,
        itemKey = if (searching) ({ index -> currentSearch.matches[index].id }) else albums.itemKey { it.id },
        onNavigateBack = { if (isSearchActive) closeSearch() else navController.popBackStack() },
        onRetry = { if (searching) viewModel.searchAlbums(searchQuery) else albums.retry() },
        state = if (searching) searchScroll else normalScroll,
        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(148.dp * VinylAlbumArtworkRatio),
        loading = if (searching) currentSearch.isSearching else albums.loadState.refresh is LoadState.Loading,
        failed = if (searching) currentSearch.error != null else albums.loadState.refresh is LoadState.Error,
        emptyText = if (searching) "没有找到匹配的专辑" else "暂无专辑", emptyActionLabel = null,
        showLargeTitle = !isSearchActive,
        titleContent = if (isSearchActive) ({ ArtistCatalogSearchField(searchQuery, { searchQuery = it }, "搜索歌手专辑") }) else null,
        actions = { CatalogSearchAction(isSearchActive) { if (isSearchActive) closeSearch() else isSearchActive = true } },
        footer = {
            if (searching) CatalogSearchStatus(currentSearch) { viewModel.searchAlbums(searchQuery) }
            else CatalogAppendState(albums)
        },
    ) { index ->
        val album = if (searching) currentSearch.matches[index] else albums[index]
        album?.let {
            VinylAlbumCard(title = album.name, subtitle = "${album.size} 首", coverUrl = album.picUrl, modifier = Modifier.fillMaxWidth()) {
                Screen.Album.navigate(navController) { addPath(album.id.toString()) }
            }
        }
    }
}

@Composable
private fun ArtistCatalogSearchField(query: String, onQueryChange: (String) -> Unit, hint: String) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    TextField(
        value = query, onValueChange = onQueryChange, placeholder = { Text(hint) }, singleLine = true,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun CatalogSearchAction(active: Boolean, onToggle: () -> Unit) {
    MeiGlassIconButton(onClick = onToggle) {
        Icon(if (active) Icons.Default.Close else Icons.Default.Search, contentDescription = if (active) "关闭搜索" else "搜索")
    }
}

@Composable
private fun ArtistSongItem(song: ArtistSong.HotSong, onClick: () -> Unit, onMore: (MediaMetadata, IntRect) -> Unit) {
    val track = remember(song) { song.toMediaMetadata() }
    Track(track = track, onClick = onClick, onMoreClick = { anchor -> onMore(track, anchor) })
}

@Composable
private fun <T> CatalogSearchStatus(state: ArtistCatalogSearchState<T>, onRetry: () -> Unit) {
    MeiCollectionLoadMore(false, state.isSearching, state.error != null, onRetry)
}

@Composable
private fun <T : Any> CatalogAppendState(items: LazyPagingItems<T>) {
    MeiCollectionLoadMore(false, items.loadState.append is LoadState.Loading, items.loadState.append is LoadState.Error, items::retry)
}
