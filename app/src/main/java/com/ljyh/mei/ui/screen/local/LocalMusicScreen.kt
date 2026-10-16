package com.ljyh.mei.ui.screen.local

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import java.net.URLEncoder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.screen.local.component.AlbumRow
import com.ljyh.mei.ui.screen.local.component.ArtistRow
import com.ljyh.mei.ui.screen.local.component.EmptyLocalMusic
import com.ljyh.mei.ui.screen.local.component.FolderItem
import com.ljyh.mei.ui.screen.local.component.ManagementCard
import com.ljyh.mei.ui.screen.local.component.SectionHeader
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMusicScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LocalMusicViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scanState by viewModel.scanState.collectAsState()
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasAudioPermission by remember(permission) {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPermission = granted
    }
    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) viewModel.scan()
    }
    val requestScan = {
        if (hasAudioPermission) viewModel.scan() else permissionLauncher.launch(permission)
    }

    val localSongs by db.songDao().getLocalSongs().collectAsState(initial = emptyList())
    val albums by db.songDao().getLocalAlbums().collectAsState(initial = emptyList())

    val artists = remember(localSongs) {
        localSongs.flatMap { it.artist }.distinct().sorted()
    }

    val folders = remember(localSongs) {
        localSongs.groupBy { it.folderPath ?: "" }
            .filter { it.key.isNotBlank() }
            .map { (path, songs) ->
                val name = path.substringAfterLast('/').ifEmpty { path }
                path to FoldInfo(name, songs.size, songs.firstOrNull { it.cover.isNotEmpty() }?.cover)
            }
            .sortedBy { it.first }
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
                    IconButton(onClick = requestScan, enabled = scanState !is LocalScanState.Scanning) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "扫描本地音乐")
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
            if (scanState is LocalScanState.Scanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (scanState is LocalScanState.Failed) {
                Text(
                    (scanState as LocalScanState.Failed).message,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (localSongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyLocalMusic(
                        hasPermission = hasAudioPermission,
                        isScanning = scanState is LocalScanState.Scanning,
                        onScan = requestScan,
                    )
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

                    if (folders.isNotEmpty()) {
                        item { SectionHeader("文件夹", "${folders.size} 个文件夹") }
                        items(folders, key = { it.first }) { (path, info) ->
                            FolderItem(
                                folderPath = path,
                                folderName = info.name,
                                songCount = info.count,
                                coverUrl = info.coverUrl,
                                onClick = {
                                    Screen.LocalSongList.navigate(navController) {
                                        addPath("folder"); addPath(URLEncoder.encode(path, "UTF-8"))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class FoldInfo(
    val name: String,
    val count: Int,
    val coverUrl: String?
)
