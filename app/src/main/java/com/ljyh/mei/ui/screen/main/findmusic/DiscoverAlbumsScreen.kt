package com.ljyh.mei.ui.screen.main.findmusic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.collection.MeiCollectionPage
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen

@Composable
fun DiscoverAlbumsScreen(viewModel: DiscoverAlbumsViewModel = hiltViewModel()) {
    val nav = LocalNavController.current
    val result by viewModel.albums.collectAsState()
    val albums = (result as? Resource.Success)?.data.orEmpty()
    MeiCollectionPage(
        title = "新碟上架", itemCount = albums.size, itemKey = { albums[it].id },
        onNavigateBack = { nav.navigateUp() }, onRetry = viewModel::reload,
        loading = result is Resource.Loading, failed = result is Resource.Error,
        columns = if (rememberDeviceInfo().isTablet) GridCells.Adaptive(164.dp) else GridCells.Fixed(2),
    ) { index ->
        val album = albums[index]
        DiscoverAlbumCard(album, Modifier.fillMaxWidth()) {
            Screen.Album.navigate(nav) { addPath(album.id.toString()) }
        }
    }
}
