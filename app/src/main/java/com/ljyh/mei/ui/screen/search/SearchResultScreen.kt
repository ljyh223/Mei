package com.ljyh.mei.ui.screen.search

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.api.SearchResult
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.player.Queue
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.encrypt.getResourceLink
import com.ljyh.mei.utils.smallImage


@Composable
fun SearchResultScreen(
    query: String,
    type: Int, // Initial type if needed, but we mostly use ViewModel state
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
        contentPadding = LocalPlayerAwareWindowInsets.current
            .add(WindowInsets(top = 4.dp))
            .asPaddingValues()
    ) {
        item {
            SearchTypeFilterRow(
                selected = selectedType,
                onSelect = {
                    viewModel.onTabChange(it)
                }
            )
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
                            SongItem(song = it, onClick = {
                                playerConnection?.playQueue(
                                    ListQueue(
                                        id = "SearchQueue",
                                        title = "搜索结果",
                                        items = songs.map { s -> s.id.toString() },
                                        startIndex = songs.indexOfFirst { s -> s.id == it.id },
                                        position = 0
                                    )
                                )
                            })
                        }
                    }

                    SearchType.Artist.type -> {
                        val artists = data.result.artists ?: emptyList()
                        items(artists) { ArtistItem(artist = it, onClick = {
                            Toast.makeText(context, "正在建设中: ${it.name}", Toast.LENGTH_SHORT).show()
                        }) }
                    }

                    SearchType.Album.type -> {
                        val albums = data.result.albums ?: emptyList()
                        items(albums) {
                            AlbumItem(album = it, onClick = {
                                Screen.Album.navigate(navController) {
                                    addPath(it.id.toString())
                                }
                            })
                        }
                    }

                    SearchType.Playlist.type -> {
                        val playlists = data.result.playlists ?: emptyList()
                        items(playlists) {
                            PlaylistItem(playlist = it, onClick = {
                                Screen.PlayList.navigate(navController) {
                                    addPath(it.id.toString())
                                }
                            })
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
                androidx.compose.material3.FilterChip(
                    selected = selected == type,
                    onClick = { onSelect(type) },
                    label = { Text(type.displayName) }
                )
            }
    }
}


@Composable
fun PlaylistItem(
    playlist: SearchResult.Result.Playlist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = playlist.coverImgUrl.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${playlist.creator.nickname} · ${playlist.trackCount}首",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ArtistItem(
    artist: SearchResult.Result.Artist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        AsyncImage(
            model = artist.picUrl?.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
    }
}

@Composable
fun AlbumItem(
    album: SearchResult.Result.Album,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = album.picUrl.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${album.artists.joinToString(" / ") { it.name }} · ${album.size}首",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SongItem(
    song: SearchResult.Result.Song,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = getResourceLink(song.album.picId.toString(), "jpg"),
            contentDescription = song.name,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (song.transNames?.isNotEmpty() == true) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "(${song.transNames.joinToString()})",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Text(
                text = song.artists.joinToString(" / ") { it.name },
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 1
            )

            if (song.alias.isNotEmpty()) {
                Text(
                    text = song.alias.joinToString(" / "),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

