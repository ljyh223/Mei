package com.ljyh.mei.ui.screen.main.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ljyh.mei.ui.component.card.ArtworkCard
import com.ljyh.mei.ui.component.collection.MeiCollectionPage
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen

@Composable
fun LibraryCollectionScreen(collection: LibraryCollection, viewModel: LibraryViewModel) {
    val nav = LocalNavController.current
    val signedIn = bindLibraryAccount(viewModel)
    val state by viewModel.libraryUiState.collectAsStateWithLifecycle()
    val data = (state as? LibraryUiState.Content)?.data?.takeIf { signedIn }
    val playlists = remember(data, collection) { data?.playlistsIn(collection).orEmpty() }
    val albums = data?.albums.orEmpty()
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
        onNavigateBack = { nav.navigateUp() },
        onRetry = {
            if (signedIn) viewModel.getUserAccount(force = true)
            else Screen.ContentSettings.navigate(nav)
        },
        columns = if (rememberDeviceInfo().isTablet) GridCells.Adaptive(168.dp) else GridCells.Fixed(2),
        loading = loading,
        failed = failed,
        emptyText = if (!signedIn) "登录查看歌单与收藏" else when (collection) {
            LibraryCollection.Created -> "还没有创建歌单"
            LibraryCollection.Collected -> "还没有收藏歌单"
            LibraryCollection.Albums -> "还没有收藏专辑"
        },
        emptyActionLabel = if (signedIn) null else "登录账号",
        footer = if (failed) ({ TextButton(onClick = { viewModel.getUserAccount(force = true) }) { Text("同步失败，重试") } }) else null,
    ) { index ->
        if (isAlbums) {
            val album = albums[index]
            ArtworkCard(album.title, album.cover, Modifier.fillMaxWidth(), subtitle = album.artist.joinToString(" / ") { it.name }) {
                Screen.Album.navigate(nav) { addPath(album.id.toString()) }
            }
        } else {
            val playlist = playlists[index]
            ArtworkCard(playlist.title, playlist.cover, Modifier.fillMaxWidth(), subtitle = "${playlist.count} 首") {
                Screen.PlayList.navigate(nav) { addPath(playlist.id) }
            }
        }
    }
}
