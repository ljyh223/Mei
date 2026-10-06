package com.ljyh.mei.ui.screen.search

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.SearchAlbum
import com.ljyh.mei.data.model.SearchPlaylist
import com.ljyh.mei.data.model.SearchResults
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.item.AlbumItem
import com.ljyh.mei.ui.component.item.ArtistItem
import com.ljyh.mei.ui.component.item.PlaylistItem
import com.ljyh.mei.ui.component.item.Track
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Screen

@OptIn(UnstableApi::class)
@Composable
fun SearchResultScreen(
    query: String,
    type: Int,
    viewModel: SearchViewModel = hiltViewModel()
) {
    // 状态收集
    val searchState by viewModel.searchResult.collectAsState()
    val selectedType by viewModel.currentTab.collectAsState()

    // 初始化逻辑：只需在进入界面（或 query 改变）时调用一次
    LaunchedEffect(query, type) {
        viewModel.onSearchInit(query, type)
    }

    val playerConnection = LocalPlayerConnection.current
    val navController = LocalNavController.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = LocalPlayerAwareWindowInsets.current
            .add(WindowInsets(top = 4.dp))
            .asPaddingValues()
    ) {
        // Tab 过滤器
        item {
            SearchTypeFilterRow(
                selected = selectedType,
                onSelect = { viewModel.onTabChange(it) }
            )
        }

        // 结果内容区
        when (val result = searchState) {
            is Resource.Loading -> {
                item {
                    LoadingView()
                }
            }
            is Resource.Error -> {
                item {
                    ErrorView(message = result.message)
                }
            }
            is Resource.Success -> {
                SearchResultList(
                    data = result.data,
                    type = selectedType,
                    navController = navController,
                    onSongClick = { songs, index ->
                        playerConnection?.playQueue(
                            ListQueue(
                                id = "SearchQueue-$query", // 加上 query 避免 ID 重复
                                title = "搜索: $query",
                                items = songs.map { song -> song.id.toString() to song.toMediaItem() },
                                startIndex = index,
                                position = 0
                            )
                        )
                    }
                )
            }
        }
    }
}

// --- 抽离出来的子组件，使主代码更整洁 ---

@Composable
fun SearchTypeFilterRow(
    selected: SearchType,
    onSelect: (SearchType) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchType.entries
            .filter { it != SearchType.History }
            .forEach { type ->
                FilterChip(
                    selected = selected == type,
                    onClick = { onSelect(type) },
                    label = { Text(type.displayName) }
                )
            }
    }
}

@Composable
fun LoadingView() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(top = 50.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(message: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(top = 50.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("加载失败：$message")
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.SearchResultList(
    data: SearchResults,
    type: SearchType,
    navController: NavController,
    onSongClick: (List<MediaMetadata>, Int) -> Unit
) {
    when (type) {
        SearchType.Song -> {
            val songs = data.songs
            if (songs.isEmpty()) item { EmptyView() }
            items(songs) { song ->
                Track(
                    track = song,
                    onClick = {
                        // 找到当前点击歌曲的 index
                        val index = songs.indexOfFirst { it.id == song.id }
                        onSongClick(songs, maxOf(0, index))
                    },
                    onMoreClick = { }
                )
            }
        }
        SearchType.Artist -> {
            val artists = data.artists
            if (artists.isEmpty()) item { EmptyView() }
            items(artists) { artist ->
                ArtistItem(artist = artist, onClick = {
                    Screen.Artist.navigate(navController) {
                        addPath(artist.id.toString())
                    }
                })
            }
        }
        SearchType.Album -> {
            val albums = data.albums
            if (albums.isEmpty()) item { EmptyView() }
            items(albums) { album ->
                AlbumItem(album = album.toUiAlbum(), onClick = {
                    Screen.Album.navigate(navController) {
                        addPath(album.id.toString())
                    }
                })
            }
        }
        SearchType.Playlist -> {
            val playlists = data.playlists
            if (playlists.isEmpty()) item { EmptyView() }
            items(playlists) { playlist ->
                PlaylistItem(
                    playlist = playlist.toRoomPlaylist(),
                    onClick = {
                        Screen.PlayList.navigate(navController) {
                            addPath(playlist.id.toString())
                        }
                    }
                )
            }
        }
        else -> {}
    }
}

private fun SearchAlbum.toUiAlbum() = com.ljyh.mei.ui.model.Album(
    id = id,
    title = title,
    cover = coverUrl,
    size = size,
    artist = artists.map { com.ljyh.mei.ui.model.Album.Artist(id = it.id, name = it.name) }
)

private fun SearchPlaylist.toRoomPlaylist() = Playlist(
    id = id.toString(),
    title = title,
    cover = coverUrl,
    author = creatorId.toString(),
    authorName = creatorName,
    authorAvatar = creatorAvatarUrl,
    count = trackCount
)

@Composable
fun EmptyView() {
    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
        Text("暂无搜索结果", color = androidx.compose.ui.graphics.Color.Gray)
    }
}
