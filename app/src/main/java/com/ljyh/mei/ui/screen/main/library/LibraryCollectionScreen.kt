package com.ljyh.mei.ui.screen.main.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ljyh.mei.ui.component.card.ArtworkCard
import com.ljyh.mei.ui.component.collection.MeiCollectionPage
import com.ljyh.mei.ui.component.glass.MeiGlassIconButton
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen

@Composable
fun LibraryCollectionScreen(collection: LibraryCollection, viewModel: LibraryViewModel) {
    val nav = LocalNavController.current
    val signedIn = bindLibraryAccount(viewModel)
    val state by viewModel.libraryUiState.collectAsStateWithLifecycle()
    val data = (state as? LibraryUiState.Content)?.data?.takeIf { signedIn }
    var query by rememberSaveable(collection) { mutableStateOf("") }
    var searchActive by rememberSaveable(collection) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    fun closeSearch() { focusManager.clearFocus(); query = ""; searchActive = false }
    BackHandler(enabled = searchActive, onBack = ::closeSearch)
    val terms = remember(query) { query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() } }
    fun matches(vararg fields: String): Boolean = terms.all { term ->
        fields.any { it.contains(term, ignoreCase = true) }
    }

    val playlists = remember(data, collection, terms) {
        data?.playlistsIn(collection).orEmpty().filter { matches(it.title, it.authorName) }
    }
    val albums = remember(data, terms) {
        data?.albums.orEmpty().filter { album ->
            matches(album.title, album.artist.joinToString(" ") { it.name })
        }
    }
    val isAlbums = collection == LibraryCollection.Albums
    val itemCount = if (isAlbums) albums.size else playlists.size
    val loading = signedIn && (state is LibraryUiState.Loading ||
            if (isAlbums) data?.albumsLoading == true else data?.playlistsLoading == true)
    val failed = signedIn && (state is LibraryUiState.Error ||
            if (isAlbums) data?.albumsError != null else data?.playlistsError != null)
    MeiCollectionPage(
        title = collection.title,
        itemCount = itemCount,
        itemKey = { if (isAlbums) "album_${albums[it].id}" else "playlist_${playlists[it].id}" },
        onNavigateBack = { if (searchActive) closeSearch() else nav.navigateUp() },
        onRetry = {
            if (signedIn) viewModel.getUserAccount(force = true)
            else Screen.ContentSettings.navigate(nav)
        },
        columns = if (rememberDeviceInfo().isTablet) GridCells.Adaptive(168.dp) else GridCells.Fixed(
            2
        ),
        showLargeTitle = !searchActive,
        titleContent = if (searchActive) ({
            LibraryCollectionSearchField(query, { query = it },
                if (isAlbums) "搜索专辑或歌手" else "搜索歌单或创建者")
        }) else null,
        actions = {
            if (signedIn) MeiGlassIconButton(onClick = {
                if (searchActive) closeSearch() else searchActive = true
            }) {
                Icon(if (searchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = if (searchActive) "关闭搜索" else "搜索")
            }
        },
        loading = loading,
        failed = failed,
        emptyText = if (!signedIn) "登录查看歌单与收藏" else if (terms.isNotEmpty()) "没有找到匹配的${if (isAlbums) "专辑" else "歌单"}" else when (collection) {
            LibraryCollection.Created -> "还没有创建歌单"
            LibraryCollection.Collected -> "还没有收藏歌单"
            LibraryCollection.Albums -> "还没有收藏专辑"
        },
        emptyActionLabel = if (signedIn) null else "登录账号",
        footer = if (failed) ({
            TextButton(onClick = { viewModel.getUserAccount(force = true) }) {
                Text(
                    "同步失败，重试"
                )
            }
        }) else null,
    ) { index ->
        if (isAlbums) {
            val album = albums[index]
            ArtworkCard(
                album.title,
                album.cover,
                Modifier.fillMaxWidth(),
                subtitle = album.artist.joinToString(" / ") { it.name }) {
                Screen.Album.navigate(nav) { addPath(album.id.toString()) }
            }
        } else {
            val playlist = playlists[index]
            ArtworkCard(
                playlist.title,
                playlist.cover,
                Modifier.fillMaxWidth(),
                subtitle = "${playlist.count} 首"
            ) {
                Screen.PlayList.navigate(nav) { addPath(playlist.id) }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryCollectionSearchField(query: String, onQueryChange: (String) -> Unit, hint: String) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    TextField(
        value = query, onValueChange = onQueryChange,
        placeholder = { Text(hint) }, singleLine = true,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
