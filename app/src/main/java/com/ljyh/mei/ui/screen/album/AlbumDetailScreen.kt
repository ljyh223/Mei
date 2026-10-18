package com.ljyh.mei.ui.screen.album

import com.ljyh.mei.playback.queue.QueueSource

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.DownloadPathKey
import com.ljyh.mei.constants.DownloadQuality
import com.ljyh.mei.constants.DownloadQualityKey
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.download.SongDownloadInfo
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.DownloadConfirmDialog
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.playlist.matchesPlaylistSearch
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.ui.screen.playlist.component.PlaylistActionOverlay
import com.ljyh.mei.download.DownloadManager
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun AlbumDetailScreen(
    id: Long,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
) {
    // 1. 请求初始数据
    LaunchedEffect(id) {
        viewModel.getAlbumDetail(id.toString())
        viewModel.isSubscribe(id)
    }

    val context = LocalContext.current
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current

    val albumDetail by viewModel.albumDetail.collectAsState()
    val subscribeState by viewModel.subscribeAlbum.collectAsState()
    val unSubscribeState by viewModel.unSubscribeAlbum.collectAsState()
    val isSubscribeState by viewModel.isSubscribe.collectAsState()
    val allMePlaylist by playlistViewModel.playlist.collectAsState()
    var currentOverlay by remember { mutableStateOf<OverlayState>(OverlayState.None) }


    // 3. 本地收藏状态 (乐观更新核心)
    var isSubscribed by remember(id) { mutableStateOf(false) }

    LaunchedEffect(subscribeState) {
        when (val result = subscribeState) {
            is Resource.Success -> if (result.data.code != 200) {
                isSubscribed = false
                Toast.makeText(context, "收藏失败，请稍后重试", Toast.LENGTH_SHORT).show()
            }
            is Resource.Error -> {
                isSubscribed = false
                Toast.makeText(context, "收藏失败: ${result.message}", Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }
    }

    LaunchedEffect(unSubscribeState) {
        when (val result = unSubscribeState) {
            is Resource.Success -> {
                if (result.data.code == 200) {
                    isSubscribed = false
                    Toast.makeText(context, "取消收藏成功", Toast.LENGTH_SHORT).show()
                } else {
                    isSubscribed = true
                    Toast.makeText(context, "取消收藏失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
            }
            is Resource.Error -> {
                isSubscribed = true
                Toast.makeText(context, "取消收藏失败: ${result.message}", Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }
    }

    LaunchedEffect(isSubscribeState) {
        isSubscribed=isSubscribeState
    }

    val detail = (albumDetail as? Resource.Success)?.data

    val scope = rememberCoroutineScope()
    var isAlbumSearchActive by remember { mutableStateOf(false) }
    var albumSearchQuery by remember { mutableStateOf("") }
    val displayedSongs = remember(detail, albumSearchQuery) {
        val songs = detail?.songs.orEmpty()
        if (albumSearchQuery.isBlank()) songs else songs.filter { it.matchesPlaylistSearch(albumSearchQuery) }
    }

    // Download dialog state
    var showDownloadDialog by remember { mutableStateOf(false) }
    var pendingDownloadTracks by remember { mutableStateOf<List<MediaMetadata>>(emptyList()) }

    val (downloadPath) = rememberPreference(DownloadPathKey, DownloadManager.getDefaultDownloadPath())
    val (downloadQuality) = rememberEnumPreference(DownloadQualityKey, DownloadQuality.EXHIGH)

    // 5. 下载处理逻辑
    fun doDownload(tracks: List<MediaMetadata>, saveSeparateLyrics: Boolean) {
        scope.launch {
            val songIds = tracks.map { it.id.toString() }
            val localIds = DownloadManager.existingLocalSongIds(context, songIds)
            val remoteIds = songIds.filterNot { it in localIds }
            val result = if (remoteIds.isNotEmpty()) {
                viewModel.resolveSongUrls(remoteIds, downloadQuality.toMusicQuality())
            } else null
            val urlMap = if (result is Resource.Success) {
                result.data.data.associate { it.id.toString() to (it.url to it.encodeType.orEmpty()) }
            } else {
                emptyMap()
            }

            val downloadInfos = tracks.mapNotNull { track ->
                val songId = track.id.toString()
                val (url, encodeType) = if (songId in localIds) {
                    null to ""
                } else {
                    urlMap[songId] ?: return@mapNotNull null
                }
                if (url != null || songId in localIds) {
                    SongDownloadInfo(
                        songId = songId,
                        url = url,
                        songTitle = track.title,
                        songArtist = track.artists.map { it.name },
                        songAlbum = track.album.title,
                        songCover = track.coverUrl,
                        duration = track.duration,
                        fileType = encodeType
                    )
                } else null
            }

            if (downloadInfos.isEmpty()) {
                Toast.makeText(context, "无法获取歌曲链接，请稍后重试", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val detail = albumDetail
            val playlistName = if (detail is Resource.Success) {
                detail.data.album.name
            } else {
                "未分类"
            }

            DownloadManager.enqueue(
                context = context,
                songs = downloadInfos,
                playlistName = playlistName,
                playlistId = id.toString(),
                downloadPath = downloadPath,
                saveSeparateLyrics = saveSeparateLyrics,
            )
            Toast.makeText(context, "已添加 ${downloadInfos.size} 首到下载队列", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleDownload() {
        pendingDownloadTracks = detail?.songs.orEmpty()
        showDownloadDialog = true
    }

    fun handleTrackDownload(track: MediaMetadata) {
        pendingDownloadTracks = listOf(track)
        showDownloadDialog = true
    }

    if (showDownloadDialog) {
        DownloadConfirmDialog(
            currentQuality = downloadQuality,
            downloadPath = downloadPath,
            onDismiss = { showDownloadDialog = false },
            onConfirm = { saveSeparateLyrics ->
                showDownloadDialog = false
                doDownload(pendingDownloadTracks, saveSeparateLyrics)
            },
            onGoToSettings = {
                showDownloadDialog = false
                Screen.DownloadSettings.navigate(navController)
            },
            onGoToDownloadManage = {
                showDownloadDialog = false
                Screen.DownloadManage.navigate(navController)
            }
        )
    }

    // 6. 提取播放队列构建逻辑 (避免重复)
    fun buildListQueue(startIndex: Int = 0): ListQueue? {
        val detail = albumDetail
        if (detail is Resource.Success) {
            val items = detail.data.songs.map { song ->
                Pair(
                    song.id.toString(),
                    song.toMediaItem()
                )
            }
            return ListQueue(
                id = "album_${detail.data.album.id}",
                title = detail.data.album.name,
                source = QueueSource(QueueSource.Type.ALBUM, detail.data.album.id.toString(), detail.data.album.name),
                items = items,
                startIndex = startIndex
            )
        }
        return null
    }

    when (val result = albumDetail) {
        is Resource.Loading -> AlbumLoadState(
            onBack = { navController.popBackStack() },
            message = null,
            onRetry = null,
        )
        is Resource.Error -> AlbumLoadState(
            onBack = { navController.popBackStack() },
            message = "专辑加载失败，请检查网络后重试",
            onRetry = { viewModel.getAlbumDetail(id.toString()) },
        )
        is Resource.Success -> {
            val coverUrl = result.data.album.picUrl
            var coverColor by remember(coverUrl) {
                mutableStateOf<Color?>(viewModel.cachedCoverColor(coverUrl))
            }
            LaunchedEffect(coverUrl) {
                coverColor = try {
                    viewModel.coverColor(coverUrl)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
            AlbumDetailContent(
                album = result.data.album,
                coverColor = coverColor,
                songs = result.data.songs,
                displayedSongs = displayedSongs,
                isSubscribed = isSubscribed,
                canPlay = playerConnection != null,
                searchQuery = albumSearchQuery,
                isSearchActive = isAlbumSearchActive,
                onSearchQueryChange = { albumSearchQuery = it },
                onSearchActiveChange = { active ->
                    isAlbumSearchActive = active
                    if (!active) albumSearchQuery = ""
                },
                onBack = { navController.popBackStack() },
                onArtistClick = { artistId ->
                    Screen.Artist.navigate(navController) { addPath(artistId.toString()) }
                },
                onPlayAll = {
                    buildListQueue(0)?.let { playerConnection?.playQueue(it) }
                },
                onSubscribe = {
                    val newState = !isSubscribed
                    isSubscribed = newState
                    if (newState) viewModel.subscribeAlbum(result.data.album)
                    else viewModel.unSubscribeAlbum(id.toString())
                },
                onDownload = { handleDownload() },
                onTrackClick = { song, index ->
                    playerConnection?.onTrackClicked(
                        trackId = song.id.toString(),
                        fallbackSource = QueueSource(QueueSource.Type.ALBUM, id.toString(), result.data.album.name),
                        buildQueue = {
                            val originalIndex = result.data.songs.indexOfFirst { it.id == song.id }
                                .takeIf { it >= 0 } ?: index
                            buildListQueue(originalIndex)
                        }
                    )
                },
                onTrackMore = { track, anchor -> currentOverlay = OverlayState.TrackActionMenu(track, anchor) },
            )
        }
    }

    PlaylistActionOverlay(
        overlay = currentOverlay,
        isCreator = false,
        playlistId = id,
        queueSource = QueueSource(QueueSource.Type.ALBUM, id.toString(),
            (albumDetail as? Resource.Success)?.data?.album?.name),
        allMePlaylist = allMePlaylist,
        onDismiss = { currentOverlay = OverlayState.None },
        onUpdateOverlay = { currentOverlay = it },
        onDownloadTrack = { handleTrackDownload(it) },
        viewModel = playlistViewModel,
    )
}

@Composable
private fun AlbumLoadState(onBack: () -> Unit, message: String?, onRetry: (() -> Unit)?) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (message == null) {
                CircularProgressIndicator()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(message, modifier = Modifier.padding(horizontal = 24.dp))
                    TextButton(onClick = { onRetry?.invoke() }) { Text("重试") }
                }
            }
        }
    }
}
