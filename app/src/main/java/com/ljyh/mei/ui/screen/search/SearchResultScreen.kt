package com.ljyh.mei.ui.screen.search

import android.R.attr.onClick
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.api.SearchResult
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.utils.encrypt.getResourceLink
import com.ljyh.mei.utils.smallImage


@Composable
fun SearchResultScreen(
    query: String,
    type: Int,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchState by viewModel.searchResult.collectAsState()
    val pagerState = rememberPagerState(pageCount = { SearchType.entries.size - 1 }) // 去掉搜索历史
    val scope = rememberCoroutineScope()
    val types = SearchType.entries.toTypedArray()
    // query 变化时自动触发搜索
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            viewModel.search(query, type, limit = 20)
        }
    }

    HorizontalPager(
        state = pagerState,
    ) { index ->

        val type = types[index]

        // 当页面变更时更换类型并搜索
        LaunchedEffect(index) {
            viewModel.onTabChange(type)
        }

        when(val result=searchState){
            is Resource.Error -> {

            }
            Resource.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Success -> {
                Log.d("SearchResultScreen", "result: $result")
                SearchResultList(type = type, data = result.data)
            }
        }


    }
}

@Composable
fun SearchResultList(type: SearchType, data: SearchResult) {

    when (type) {
        SearchType.Song -> data.result.songs?.let {
            SongList(songs = it, onSongClick = { song ->
                // 跳转到歌曲详情页
            })
        }

        SearchType.Artist -> data.result.artists?.let {
            ArtistList(
                artists = it,
                onClick = {

                }
            )
        }

        SearchType.Album -> data.result.albums?.let {
            AlbumList(
                albums = it,
                onClick = {}
            )
        }

        SearchType.Playlist -> data.result.playlists?.let {
            PlaylistList(
                playlists = it,
                onClick = {}
            )
        }

        SearchType.History -> {

        }
    }
}


@Composable
fun SongList(
    songs: List<SearchResult.Result.Song>,
    onSongClick: (SearchResult.Result.Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(songs) { song ->
            SongItem(song = song, onClick = { onSongClick(song) })
        }
    }
}


@Composable
fun AlbumList(
    albums: List<SearchResult.Result.Album>,
    onClick: (Long) -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums) { album ->
            AlbumItem(album = album, onClick = { onClick(album.id) })
        }
    }
}

@Composable
fun ArtistList(
    artists: List<SearchResult.Result.Artist>,
    onClick: (Long) -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(artists) { artist ->
            ArtistItem(artist = artist, onClick = { onClick(artist.id) })
        }
    }
}

@Composable
fun PlaylistList(
    playlists: List<SearchResult.Result.Playlist>,
    onClick: (Long) -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(playlists) { playlist ->
            PlaylistItem(playlist = playlist, onClick = { onClick(playlist.id) })
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
            model = artist.picUrl.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

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
        // 封面
        AsyncImage(
            model = getResourceLink(song.album.picId.toString(), "jpg"),
            contentDescription = song.name,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {

            // 歌名 + 翻译名
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (song.transNames?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${song.transNames.joinToString(", ")})",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 艺人
            Text(
                text = song.artists.joinToString(" / ") { it.name },
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 别名 alias
            if (song.alias.isNotEmpty()) {
                Text(
                    text = song.alias.joinToString(" / "),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
