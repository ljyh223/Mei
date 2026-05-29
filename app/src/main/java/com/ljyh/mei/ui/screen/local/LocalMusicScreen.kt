package com.ljyh.mei.ui.screen.local

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.screen.local.component.AlbumRow
import com.ljyh.mei.ui.screen.local.component.ArtistRow
import com.ljyh.mei.ui.screen.local.component.EmptyLocalMusic
import com.ljyh.mei.ui.screen.local.component.FolderItem
import com.ljyh.mei.ui.screen.local.component.LibraryStats
import com.ljyh.mei.ui.screen.local.component.ManagementCard
import com.ljyh.mei.ui.screen.local.component.ManagementCards
import com.ljyh.mei.ui.screen.local.component.ScanProgressCard
import com.ljyh.mei.ui.screen.local.component.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMusicScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LocalMusicViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)

    val localSongs by db.songDao().getLocalSongs().collectAsState(initial = emptyList())
    val artists by db.songDao().getLocalArtists().collectAsState(initial = emptyList())
    val albums by db.songDao().getLocalAlbums().collectAsState(initial = emptyList())
    val scanFolders by db.scanFolderDao().getAll().collectAsState(initial = emptyList())
    val scanState by viewModel.scanState.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.scanFolderUri(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("本地音乐") },
                navigationIcon = {
                    com.ljyh.mei.ui.component.IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
        ) {
            if (scanState.isScanning) {
                ScanProgressCard(scanState)
            }

            if (localSongs.isEmpty() && !scanState.isScanning) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyLocalMusic(onAddFolder = { folderPickerLauncher.launch(null) })
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item { LibraryStats(localSongs) }

                    item { SectionHeader("歌曲", "${localSongs.size} 首") }
                    item {
                        ManagementCard(
                            "全部歌曲",
                            "浏览所有本地歌曲",
                            Icons.Rounded.MusicNote,
                            onClick = {
                                Screen.LocalSongList.navigate(navController) {
                                    addPath("all"); addPath("all")
                                }
                            }
                        )
                    }

                    if (artists.isNotEmpty()) {
                        item { SectionHeader("艺术家", "${artists.size} 位艺术家") }
                        item {
                            ArtistRow(
                                artists = artists,
                                songs = localSongs,
                                onArtistClick = { name ->
                                    Screen.LocalSongList.navigate(navController) {
                                        addPath("artist"); addPath(name)
                                    }
                                }
                            )
                        }
                    }

                    if (albums.isNotEmpty()) {
                        item { SectionHeader("专辑", "${albums.size} 张专辑") }
                        item {
                            AlbumRow(
                                albums = albums,
                                songs = localSongs,
                                onAlbumClick = { name ->
                                    Screen.LocalSongList.navigate(navController) {
                                        addPath("album"); addPath(name)
                                    }
                                }
                            )
                        }
                    }

                    if (scanFolders.isNotEmpty()) {
                        item { SectionHeader("文件夹", "${scanFolders.filter { it.enabled }.size} 个文件夹") }
                        items(scanFolders, key = { it.id }) { folder ->
                            FolderItem(
                                folder = folder,
                                onClick = {
                                    Screen.LocalSongList.navigate(navController) {
                                        addPath("folder"); addPath(folder.path)
                                    }
                                }
                            )
                        }
                    }

                    item { SectionHeader("管理", null) }
                    item {
                        ManagementCards(
                            onAddFolder = { folderPickerLauncher.launch(null) }
                        )
                    }
                }
            }
        }
    }
}
