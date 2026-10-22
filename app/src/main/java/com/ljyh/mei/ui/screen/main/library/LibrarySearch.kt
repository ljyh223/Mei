package com.ljyh.mei.ui.screen.main.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.component.item.AlbumItem
import com.ljyh.mei.ui.component.item.PlaylistItem
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen

@Stable
class LibrarySearchState {
    var state: LibraryUiState? by mutableStateOf(null)
}

val LocalLibrarySearchState = staticCompositionLocalOf<LibrarySearchState?> { null }

/** Searches cached collection metadata, never expands or fetches every playlist's tracks. */
@Composable
fun LibrarySearchResults(query: String, onOpen: () -> Unit) {
    val state = LocalLibrarySearchState.current?.state
    val content = (state as? LibraryUiState.Content)?.data
    val matches = remember(content, query) { searchLibrary(content, query) }
    val syncing = content?.let { it.albumsLoading || it.playlistsLoading } == true
    val nav = LocalNavController.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(horizontal = meiPageGutter(), vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("搜索音乐库", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("歌单、创建者、专辑与歌手", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            state is LibraryUiState.Loading -> item { Box(Modifier.fillMaxWidth().padding(48.dp), androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() } }
            state is LibraryUiState.Error -> item { MeiStatePanel("收藏暂时不可用", Icons.Rounded.Search, message = "返回音乐库重试同步。") }
            content == null -> item { MeiStatePanel("登录后搜索歌单与收藏", Icons.Rounded.Search) }
            query.isBlank() -> Unit
            matches.playlists.isEmpty() && matches.albums.isEmpty() && !syncing -> item {
                MeiStatePanel("没有找到匹配的收藏", Icons.Rounded.Search, message = "试试其他名称或歌手。")
            }
            else -> {
                if (matches.playlists.isNotEmpty()) {
                    item { SearchHeading("歌单 · ${matches.playlists.size}") }
                    items(matches.playlists, key = { "playlist_${it.id}" }) { playlist ->
                        PlaylistItem(playlist) { id -> onOpen(); Screen.PlayList.navigate(nav) { addPath(id) } }
                    }
                }
                if (matches.albums.isNotEmpty()) {
                    item { SearchHeading("专辑 · ${matches.albums.size}") }
                    items(matches.albums, key = { "album_${it.id}" }) { album ->
                        AlbumItem(album) { onOpen(); Screen.Album.navigate(nav) { addPath(album.id.toString()) } }
                    }
                }
            }
        }
        if (query.isNotBlank() && syncing) item {
            Row(Modifier.fillMaxWidth().padding(horizontal = meiPageGutter(), vertical = 16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("正在同步收藏", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (content != null && (content.albumsError != null || content.playlistsError != null)) item {
            TextButton(onClick = onOpen, modifier = Modifier.padding(horizontal = meiPageGutter())) { Text("部分收藏同步失败，返回重试") }
        }
    }
}

@Composable
private fun SearchHeading(text: String) {
    Text(text, Modifier.padding(horizontal = meiPageGutter(), vertical = 12.dp),
        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}
