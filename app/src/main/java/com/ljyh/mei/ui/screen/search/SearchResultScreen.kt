package com.ljyh.mei.ui.screen.search

import android.widget.Toast
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.data.model.api.toAlbum
import com.ljyh.mei.data.model.api.toMediaData
import com.ljyh.mei.data.model.api.toPlaylist
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


@Composable
fun SearchResultScreen(
    query: String, type: Int, // Initial type if needed, but we mostly use ViewModel state
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchState by viewModel.searchResult.collectAsState()
    val selectedType = viewModel.currentTab
    val playerConnection = LocalPlayerConnection.current
    val navController = LocalNavController.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val initialType = SearchType.entries.find { it.type == type } ?: SearchType.Song
        if (viewModel.currentTab != initialType) {
            viewModel.onTabChange(initialType)
        }
    }

    LaunchedEffect(query, selectedType) {
        if (query.isNotBlank()) {
            viewModel.search(query, selectedType.type, limit = 30)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = LocalPlayerAwareWindowInsets.current.add(WindowInsets(top = 4.dp))
            .asPaddingValues()
    ) {
        item {
            SearchTypeFilterRow(
                selected = selectedType, onSelect = {
                    viewModel.onTabChange(it)
                })
        }

        when (val result = searchState) {
            is Resource.Loading -> {
                item {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(top = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is Resource.Error -> {
                item {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(top = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("加载失败：${result.message}")
                    }
                }
            }

            is Resource.Success -> {
                val data = result.data
                when (selectedType.type) {
                    SearchType.Song.type -> {
                        val songs = data.result.songs ?: emptyList()
                        items(songs) {
                            Track(track = it.toMediaData(), onClick = {
                                playerConnection?.playQueue(
                                    ListQueue(
                                        id = "SearchQueue",
                                        title = "搜索结果",
                                        items = songs.map { s -> s.id.toString() },
                                        startIndex = songs.indexOfFirst { s -> s.id == it.id },
                                        position = 0
                                    )
                                )
                            }, onMoreClick = { })
                        }
                    }

                    SearchType.Artist.type -> {
                        val artists = data.result.artists ?: emptyList()
                        items(artists) {
                            ArtistItem(artist = it, onClick = {
                                Screen.Artist.navigate(navController){
                                    addPath(it.id.toString())
                                }
                            })
                        }
                    }

                    SearchType.Album.type -> {
                        val albums = data.result.albums ?: emptyList()
                        items(albums) { album ->
                            AlbumItem(album = album.toAlbum(), onClick = {
                                Screen.Album.navigate(navController) {
                                    addPath(album.id.toString())
                                }
                            })
                        }
                    }

                    SearchType.Playlist.type -> {
                        val playlists = data.result.playlists ?: emptyList()
                        items(playlists) { playlist ->
                            PlaylistItem(
                                playlist = playlist.toPlaylist(),
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
        }
    }
}

@Composable
fun SearchTypeFilterRow(
    selected: SearchType, onSelect: (SearchType) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchType.entries.filter { it != SearchType.History }.forEach { type ->
                androidx.compose.material3.FilterChip(
                    selected = selected == type,
                    onClick = { onSelect(type) },
                    label = { Text(type.displayName) })
            }
    }
}


