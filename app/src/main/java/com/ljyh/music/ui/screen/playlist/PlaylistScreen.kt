package com.ljyh.music.ui.screen.playlist

import android.app.Activity
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.google.gson.Gson
import com.ljyh.music.AppContext
import com.ljyh.music.constants.UserIdKey
import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.data.model.SimplePlaylist
import com.ljyh.music.data.model.api.GetLyric
import com.ljyh.music.data.model.api.GetSongDetails
import com.ljyh.music.data.model.api.GetSongUrl
import com.ljyh.music.data.model.parseString
import com.ljyh.music.data.model.room.Like
import com.ljyh.music.data.network.Resource
import com.ljyh.music.di.PlaylistRepository
import com.ljyh.music.extensions.mediaItems
import com.ljyh.music.playback.queue.ListQueue
import com.ljyh.music.ui.component.ConfirmationDialog
import com.ljyh.music.ui.component.Track
import com.ljyh.music.ui.component.shimmer.ButtonPlaceholder
import com.ljyh.music.ui.component.shimmer.ListItemPlaceHolder
import com.ljyh.music.ui.component.shimmer.ShimmerHost
import com.ljyh.music.ui.component.shimmer.TextPlaceholder
import com.ljyh.music.ui.local.LocalNavController
import com.ljyh.music.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.music.ui.local.LocalPlayerConnection
import com.ljyh.music.utils.DownloadManager
import com.ljyh.music.utils.NotificationHelper
import com.ljyh.music.utils.checkAndRequestFilesPermissions
import com.ljyh.music.utils.largeImage
import com.ljyh.music.utils.rearrangeArray
import com.ljyh.music.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    id: Long,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    LaunchedEffect(key1 = id) {
        viewModel.getPlaylistDetail(id.toString())
    }
    val userId by rememberPreference(UserIdKey, "")
    val playlistDetail by viewModel.playlistDetail.collectAsState()
    val lazyPagingItems = remember(id, playlistDetail) {
        if (playlistDetail is Resource.Success) {
            viewModel.getPlaylistTracks(id.toString(), userId, playlistDetail)
        } else {
            flowOf(PagingData.empty()) // 详情未加载完成时返回空数据
        }
    }.collectAsLazyPagingItems()


    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val lazyListState = rememberLazyListState()
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }

    val title = remember { mutableStateOf("") }
    val ids = remember { mutableStateOf(listOf<String>()) }
    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }
    Box(
        modifier = Modifier.fillMaxSize()

    ) {

        LazyColumn(
            Modifier.padding(horizontal = 16.dp),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            state = lazyListState,
        ) {
            when (val result = playlistDetail) {
                is Resource.Error -> {
                    item { Text(text = "Error: ${result.message}") }
                    Log.d("PlaylistScreen", result.toString())
                }

                Resource.Loading -> {
                    item {
                        PlaylistShimmer()
                    }
                    Log.d("PlaylistScreen", result.toString())
                }

                is Resource.Success -> {
                    title.value = result.data.playlist.name
                    ids.value = result.data.playlist.trackIds.map { it.id.toString() }
                    if (result.data.playlist.name.endsWith("喜欢的音乐")) {
                        viewModel.updateAllLike(result.data.playlist.trackIds.map { Like(it.id.toString()) }
                            .toList())
                    }
                    item {
                        PlaylistInfo(result.data, viewModel) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = result.data.playlist.name,
                                    items = ids.value
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(lazyPagingItems.itemCount) { index ->
                        val track = lazyPagingItems[index]
                        if (track != null) {
                            Track(viewModel, track) {
                                playerConnection.player.mediaItems.forEachIndexed { i, mediaItem ->
                                    Log.d(
                                        "PlaylistScreen",
                                        "index: $i, mediaId: ${mediaItem.mediaId}"
                                    )
                                    if (mediaItem.mediaId == track.id.toString()) {
                                        playerConnection.player.seekToDefaultPosition(i)
                                        playerConnection.player.play()
                                        return@forEachIndexed
                                    }
                                }
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = result.data.playlist.name,
                                        items = rearrangeArray(
                                            lazyPagingItems.itemSnapshotList.items.map { it.id.toString() },
                                            index
                                        )
                                    )
                                )
                            }
                        }
                    }


                }

            }

        }
        TopAppBar(
            title = { if (showTopBarTitle) Text("歌单详情") },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = null
                    )
                }
            },
            scrollBehavior = scrollBehavior
        )

    }

}


@Composable
fun PlaylistInfo(
    playlistDetail: PlaylistDetail,
    viewModel: PlaylistViewModel,
    play: () -> Unit
) {
    val userId by rememberPreference(UserIdKey, "")
    val context = LocalContext.current
    val scope = CoroutineScope(Dispatchers.IO)
    val count = remember { mutableIntStateOf(0) }
    val showDialog = remember { mutableStateOf(false) }
    val ids = remember { mutableStateOf("") }


    ConfirmationDialog(
        title = "是否继续",
        text = "共计下载${count.intValue}首歌曲",
        onConfirm = { prepare(ids.value, scope, playlistDetail, viewModel) },
        onDismiss = { Toast.makeText(context, "取消了下载", Toast.LENGTH_SHORT).show() },
        openDialog = showDialog
    )
    Column {
        Row {
            AsyncImage(
                modifier = Modifier
                    .size(144.dp)
                    .clip(RoundedCornerShape(6.dp)),
                model = playlistDetail.playlist.coverImgUrl.largeImage(),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = playlistDetail.playlist.name,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "${playlistDetail.playlist.trackCount} 首歌曲\n${playlistDetail.playlist.description}",
                    color = MaterialTheme.colorScheme.secondary,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    maxLines = 3
                )



                Row {
                    Button(onClick = { play() }) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    if (userId == playlistDetail.playlist.creator.userId.toString()
                    ) {
                        Button(onClick = {

                            if (playlistDetail.playlist.trackCount > 500) {
                                Toast.makeText(context, "歌曲数量大于500", Toast.LENGTH_SHORT)
                                    .show()
                                return@Button
                            }

                            if (!checkAndRequestFilesPermissions(context as Activity)) {
                                Toast.makeText(context, "没有权限", Toast.LENGTH_SHORT).show()
                                return@Button
                            }


                            val downloadDir =
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                            val file = downloadDir.listFiles()
                                ?.find { it.isFile && it.name == "${playlistDetail.playlist.Id}.json" }
                            if (file != null) {
                                file.readText().let { json ->
                                    val playlist =
                                        Gson().fromJson(json, SimplePlaylist::class.java)
                                    if (playlist.songs.size < playlistDetail.playlist.trackCount) {
                                        val difference =
                                            playlistDetail.playlist.trackIds.filterNot { a ->
                                                playlist.songs.any { s -> s.id == a.id.toString() }
                                            }
                                        if (difference.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "没有需要下载的",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                            return@Button
                                        }
                                        Log.d("PlaylistScreen", difference.toString())
                                        count.intValue = difference.size
                                        ids.value =
                                            difference.joinToString(",") { it.id.toString() }
                                        showDialog.value = true

                                    } else {
                                        Toast.makeText(
                                            context,
                                            "没有需要下载的",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }

                                }
                            } else {

                                count.intValue = playlistDetail.playlist.trackCount
                                ids.value =
                                    playlistDetail.playlist.trackIds.joinToString(",") { it.id.toString() }
                                showDialog.value = true
                            }


                        }) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        }

                    } else {
                        Button(onClick = {

                        }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        }

                    }


                }
            }
        }
    }
}


fun prepare(
    ids: String,
    scope: CoroutineScope,
    playlistDetail: PlaylistDetail,
    viewModel: PlaylistViewModel,
) {
    val context = AppContext.instance
    val notificationHelper = NotificationHelper(context)
    Log.d("download", ids)
    Toast.makeText(context, "正在准备资源", Toast.LENGTH_SHORT).show()

    scope.launch {
        val result = viewModel.apiService.getSongDetail(
            GetSongDetails(ids)
        )
        val tempSongs = ArrayList<SimplePlaylist.Song>()
        tempSongs.addAll(
            result.songs.map { s ->
                val lyric = viewModel.apiService.getLyric(
                    GetLyric(
                        id = s.id.toString()
                    )
                )
                SimplePlaylist.Song(
                    id = s.id.toString(),
                    name = s.name,
                    artist = s.ar.joinToString(",") { it.name },
                    album = s.al.name,
                    picUrl = s.al.picUrl,
                    lyric = lyric.parseString(),
                    url = ""
                )
            }
        )

        val songUrls = viewModel.apiService.getSongUrl(GetSongUrl(ids))
        tempSongs.forEach { song ->
            songUrls.data.find { it.id.toString() == song.id }?.let {
                song.url = it.url.toString()
            } ?: ""
        }


        DownloadManager.downloadSongs(
            SimplePlaylist(
                id = playlistDetail.playlist.Id.toString(),
                name = playlistDetail.playlist.name,
                songs = tempSongs
            ),
            onProgress = { current, total, lose ->
                notificationHelper.showProgressNotification(
                    current,
                    total,
                    lose
                )
            },
            onComplete = {
                notificationHelper.showCompletionNotification()
            }
        )

    }
}

@Composable
fun PlaylistShimmer() {
    ShimmerHost {
        Row(
            modifier = Modifier.padding(8.dp)
        ) {
            TextPlaceholder(
                Modifier
                    .size(144.dp)
                    .padding(end = 16.dp)
            )
            Column {
                repeat(3) {
                    TextPlaceholder()
                }
                Row {
                    repeat(2) {
                        ButtonPlaceholder()
                    }
                }
            }
        }
        repeat(10) {
            ListItemPlaceHolder()
        }
    }
}
