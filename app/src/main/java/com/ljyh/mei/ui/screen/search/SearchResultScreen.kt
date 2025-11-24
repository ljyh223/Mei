package com.ljyh.mei.ui.screen.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.api.SearchResult
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.utils.encrypt.getResourceLink


@Composable
fun SearchResultScreen(
    query: String,
    type: Int,
    viewModel: SearchViewModel = hiltViewModel(),
    onSongClick: (SearchResult.Result.Song) -> Unit = {}
) {
    val searchState by viewModel.searchResult.collectAsState()

    // query 变化时自动触发搜索
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            viewModel.search(query, type, limit = 20)
        }
    }

    when (searchState) {
        is Resource.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        is Resource.Error -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("搜索失败：${(searchState as Resource.Error).message}")
        }

        is Resource.Success -> {
            val result = (searchState as Resource.Success<SearchResult>).data.result
            SongListContent(
                songs = result.songs,
                hasMore = result.hasMore,
                onSongClick = onSongClick
            )
        }
    }
}

@Composable
fun SongListContent(
    songs: List<SearchResult.Result.Song>,
    hasMore: Boolean,
    onSongClick: (SearchResult.Result.Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(songs) { song ->
            SongItem(song = song, onClick = { onSongClick(song) })
        }

        if (hasMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("还有更多结果…")
                }
            }
        }
    }
}


@Composable
fun SongItem(
    song: SearchResult.Result.Song,
    onClick: () -> Unit
) {
    val coverUrl = "https://p2.music.126.net/109951170961702500.jpg?param=200y200"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面
        AsyncImage(
            model = getResourceLink(song.album.id.toString(), "jpg"),
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

                if (song.transNames.isNotEmpty()) {
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
