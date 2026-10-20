package com.ljyh.mei.ui.screen.artist

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.data.model.domain.toMediaMetadata
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.ui.component.track.Track
import com.ljyh.mei.ui.component.track.TrackPlaceholder
import com.ljyh.mei.ui.component.track.BasicTrackActionMenu
import com.ljyh.mei.ui.component.track.TrackMenuTarget
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.image.sizedNeteaseImage
import java.util.UUID

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
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val searching = isSearchActive && searchQuery.isNotBlank()
    val currentSearch = searchState.takeIf { it.query == searchQuery.trim() }
        ?: ArtistCatalogSearchState<ArtistSong.HotSong>(query = searchQuery.trim(), isSearching = true)

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

    Scaffold(topBar = {
        ArtistCatalogTopBar(
            title = "全部歌曲",
            searchHint = "搜索歌手歌曲",
            searchQuery = searchQuery,
            isSearchActive = isSearchActive,
            onSearchQueryChange = { searchQuery = it },
            onSearchToggle = {
                if (isSearchActive) searchQuery = ""
                isSearchActive = !isSearchActive
            },
            onBack = {
                if (isSearchActive) {
                    searchQuery = ""
                    isSearchActive = false
                } else navController.popBackStack()
            },
        )
    }) { padding ->
        if (searching) {
            when {
                currentSearch.isSearching && currentSearch.matches.isEmpty() ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                currentSearch.error != null && currentSearch.matches.isEmpty() ->
                    CatalogMessage("搜索失败：${currentSearch.error}", "重试", {
                        viewModel.searchSongs(searchQuery)
                    }, Modifier.fillMaxSize().padding(padding))
                currentSearch.matches.isEmpty() ->
                    CatalogMessage("没有找到匹配的歌曲", null, null, Modifier.fillMaxSize().padding(padding))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = bottomInset + 16.dp),
                ) {
                    items(count = currentSearch.matches.size, key = { currentSearch.matches[it].id }) { index ->
                        val song = currentSearch.matches[index]
                        ArtistSongItem(
                            song = song,
                            onClick = { playSong(song, currentSearch.matches) },
                            onMore = { track, anchor -> menuTrack = TrackMenuTarget(track, anchor, source) },
                        )
                    }
                    if (currentSearch.isSearching || currentSearch.error != null) {
                        item(key = "search_status") {
                            CatalogSearchStatus(currentSearch) { viewModel.searchSongs(searchQuery) }
                        }
                    }
                }
            }
        } else when (songs.loadState.refresh) {
            is LoadState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is LoadState.Error -> CatalogMessage("歌曲加载失败", "重试", { songs.retry() }, Modifier.fillMaxSize().padding(padding))
            is LoadState.NotLoading -> if (songs.itemCount == 0) {
                CatalogMessage("暂无歌曲", null, null, Modifier.fillMaxSize().padding(padding))
            } else LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = bottomInset + 16.dp),
            ) {
                items(count = songs.itemCount, key = songs.itemKey { it.id }) { index ->
                    val song = songs[index]
                    if (song == null) TrackPlaceholder(index = index) else {
                        ArtistSongItem(
                            song = song,
                            onClick = { playSong(song, songs.itemSnapshotList.items) },
                            onMore = { track, anchor -> menuTrack = TrackMenuTarget(track, anchor, source) },
                        )
                    }
                }
                item { CatalogAppendState(songs) }
            }
        }
    }
    BasicTrackActionMenu(menuTrack) { menuTrack = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistAlbumsScreen(id: String, viewModel: ArtistCatalogViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    val albums = viewModel.albums.collectAsLazyPagingItems()
    val searchState by viewModel.albumSearchState.collectAsStateWithLifecycle()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val searching = isSearchActive && searchQuery.isNotBlank()
    val currentSearch = searchState.takeIf { it.query == searchQuery.trim() }
        ?: ArtistCatalogSearchState<ArtistAlbum.HotAlbum>(query = searchQuery.trim(), isSearching = true)

    LaunchedEffect(searchQuery) { viewModel.searchAlbums(searchQuery) }

    Scaffold(topBar = {
        ArtistCatalogTopBar(
            title = "全部专辑",
            searchHint = "搜索歌手专辑",
            searchQuery = searchQuery,
            isSearchActive = isSearchActive,
            onSearchQueryChange = { searchQuery = it },
            onSearchToggle = {
                if (isSearchActive) searchQuery = ""
                isSearchActive = !isSearchActive
            },
            onBack = {
                if (isSearchActive) {
                    searchQuery = ""
                    isSearchActive = false
                } else navController.popBackStack()
            },
        )
    }) { padding ->
        if (searching) {
            when {
                currentSearch.isSearching && currentSearch.matches.isEmpty() ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                currentSearch.error != null && currentSearch.matches.isEmpty() ->
                    CatalogMessage("搜索失败：${currentSearch.error}", "重试", {
                        viewModel.searchAlbums(searchQuery)
                    }, Modifier.fillMaxSize().padding(padding))
                currentSearch.matches.isEmpty() ->
                    CatalogMessage("没有找到匹配的专辑", null, null, Modifier.fillMaxSize().padding(padding))
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(148.dp),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomInset + 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(count = currentSearch.matches.size, key = { currentSearch.matches[it].id }) { index ->
                        val album = currentSearch.matches[index]
                        ArtistAlbumGridItem(album) {
                            Screen.Album.navigate(navController) { addPath(album.id.toString()) }
                        }
                    }
                    if (currentSearch.isSearching || currentSearch.error != null) {
                        item(key = "search_status", span = { GridItemSpan(maxLineSpan) }) {
                            CatalogSearchStatus(currentSearch) { viewModel.searchAlbums(searchQuery) }
                        }
                    }
                }
            }
        } else when (albums.loadState.refresh) {
            is LoadState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is LoadState.Error -> CatalogMessage("专辑加载失败", "重试", { albums.retry() }, Modifier.fillMaxSize().padding(padding))
            is LoadState.NotLoading -> if (albums.itemCount == 0) {
                CatalogMessage("暂无专辑", null, null, Modifier.fillMaxSize().padding(padding))
            } else LazyVerticalGrid(
                columns = GridCells.Adaptive(148.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomInset + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(count = albums.itemCount, key = albums.itemKey { it.id }) { index ->
                    albums[index]?.let { album ->
                        ArtistAlbumGridItem(album) {
                            Screen.Album.navigate(navController) { addPath(album.id.toString()) }
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { CatalogAppendState(albums) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistCatalogTopBar(
    title: String,
    searchHint: String,
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) focusRequester.requestFocus()
    }
    CenterAlignedTopAppBar(
        title = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(searchHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            } else Text(title)
        },
        navigationIcon = {
            IconButton(onClick = {
                focusManager.clearFocus()
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            IconButton(onClick = {
                if (isSearchActive) focusManager.clearFocus()
                onSearchToggle()
            }) {
                Icon(
                    if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearchActive) "关闭搜索" else searchHint,
                )
            }
        },
    )
}

@Composable
private fun ArtistSongItem(
    song: ArtistSong.HotSong,
    onClick: () -> Unit,
    onMore: (MediaMetadata, IntRect) -> Unit,
) {
    val track = remember(song) { song.toMediaMetadata() }
    Track(track = track, onClick = onClick, onMoreClick = { anchor -> onMore(track, anchor) })
}

@Composable
private fun <T> CatalogSearchStatus(state: ArtistCatalogSearchState<T>, onRetry: () -> Unit) {
    when {
        state.isSearching -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.error != null -> CatalogMessage("搜索中断：${state.error}", "继续搜索", onRetry, Modifier.fillMaxWidth().padding(16.dp))
    }
}

@Composable
private fun ArtistAlbumGridItem(album: ArtistAlbum.HotAlbum, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        AsyncImage(
            model = album.picUrl.sizedNeteaseImage(400),
            contentDescription = "专辑《${album.name}》封面",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Text(
            album.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            "${album.size} 首",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun CatalogMessage(message: String, action: String?, onAction: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun <T : Any> CatalogAppendState(items: LazyPagingItems<T>) {
    when (items.loadState.append) {
        is LoadState.Loading -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is LoadState.Error -> CatalogMessage("加载更多失败", "重试", { items.retry() }, Modifier.fillMaxWidth().padding(16.dp))
        is LoadState.NotLoading -> Unit
    }
}
