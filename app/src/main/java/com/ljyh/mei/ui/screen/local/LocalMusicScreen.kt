package com.ljyh.mei.ui.screen.local

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.PlaylistType
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.screen.local.component.AlbumRow
import com.ljyh.mei.ui.screen.local.component.ArtistRow
import com.ljyh.mei.ui.screen.local.component.EmptyLocalMusic
import com.ljyh.mei.ui.screen.local.component.FolderItem
import com.ljyh.mei.ui.screen.local.component.ManagementCard
import com.ljyh.mei.ui.screen.local.component.ManagementCards
import com.ljyh.mei.ui.screen.local.component.ScanProgressCard
import com.ljyh.mei.ui.screen.local.component.SectionHeader
import com.ljyh.mei.utils.PermissionsUtils
import kotlinx.coroutines.launch

private val ARTIST_SEPARATORS = Regex("[、/;|&]")

private fun splitArtists(artist: String): List<String> {
    if (artist.isBlank()) return emptyList()
    return artist.split(ARTIST_SEPARATORS)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .ifEmpty { listOf(artist.trim()) }
}

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
    val albums by db.songDao().getLocalAlbums().collectAsState(initial = emptyList())
    val scanFolders by db.scanFolderDao().getAll().collectAsState(initial = emptyList())
    val scanState by viewModel.scanState.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()

    val artists = remember(localSongs) {
        localSongs.flatMap { song ->
            splitArtists(song.artist)
        }.distinct().sorted()
    }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.scanAllMusic()
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
                actions = {
                    IconButton(
                        onClick = { viewModel.scanAllMusic() },
                        enabled = !scanState.isScanning && hasPermission
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = "重新扫描"
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

            if (!hasPermission) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyLocalMusic(
                        onAddFolder = {
                            val activity = context as? Activity
                            if (activity != null) {
                                PermissionsUtils.checkAndRequestFilesPermissions(activity)
                                viewModel.checkPermission()
                            }
                        }
                    )
                }
            } else if (localSongs.isEmpty() && !scanState.isScanning) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyLocalMusic(onAddFolder = { viewModel.scanAllMusic() })
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
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
                            val folderSongs = localSongs.filter { it.folderPath == folder.path }
                            val coverUrl = folderSongs.firstOrNull { it.cover.isNotEmpty() }?.cover
                            FolderItem(
                                folder = folder,
                                coverUrl = coverUrl,
                                onClick = {
                                    Screen.LocalSongList.navigate(navController) {
                                        addPath("folder_id"); addPath(folder.id.toString())
                                    }
                                }
                            )
                        }
                    }

                    item { SectionHeader("管理", null) }
                    item {
                        ManagementCards(
                            onAddFolder = { viewModel.scanAllMusic() },
                            onCreatePlaylist = { showCreatePlaylistDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("创建歌单") },
            text = {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("歌单名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            val playlist = Playlist(
                                id = "user_${System.currentTimeMillis()}",
                                title = newPlaylistName,
                                cover = "",
                                author = "local",
                                authorName = "本地音乐",
                                authorAvatar = "",
                                count = 0,
                                type = PlaylistType.USER,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            scope.launch {
                                db.playlistDao().insertPlaylist(playlist)
                            }
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newPlaylistName = ""
                    showCreatePlaylistDialog = false
                }) {
                    Text("取消")
                }
            }
        )
    }
}
