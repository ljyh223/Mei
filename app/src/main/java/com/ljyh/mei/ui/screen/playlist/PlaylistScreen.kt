package com.ljyh.mei.ui.screen.playlist

import android.app.Activity
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.PlayCircle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.google.gson.Gson
import com.ljyh.mei.AppContext
import com.ljyh.mei.R
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.MiniPlaylistDetail
import com.ljyh.mei.data.model.PlaylistDetail
import com.ljyh.mei.data.model.SimplePlaylist
import com.ljyh.mei.data.model.api.GetLyric
import com.ljyh.mei.data.model.api.GetSongDetails
import com.ljyh.mei.data.model.api.GetSongUrl
import com.ljyh.mei.data.model.parseString
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.model.toMiniPlaylistDetail
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.extensions.mediaItems
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.ConfirmationDialog
import com.ljyh.mei.ui.component.FinalPerfectCollage
import com.ljyh.mei.ui.component.Track
import com.ljyh.mei.ui.component.shimmer.ButtonPlaceholder
import com.ljyh.mei.ui.component.shimmer.ListItemPlaceHolder
import com.ljyh.mei.ui.component.shimmer.ShimmerHost
import com.ljyh.mei.ui.component.shimmer.TextPlaceholder
import com.ljyh.mei.ui.component.utils.fadingEdge
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.utils.DownloadManager
import com.ljyh.mei.utils.NotificationHelper
import com.ljyh.mei.utils.PermissionsUtils.checkAndRequestFilesPermissions
import com.ljyh.mei.utils.rearrangeArray
import com.ljyh.mei.utils.rememberPreference
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
                        PlaylistInfo(result.data.toMiniPlaylistDetail(), viewModel) {
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
                            Track(viewModel, track.toMediaMetadata()) {
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
                                            index,
                                            lazyPagingItems.itemSnapshotList.items.map { it.id.toString() })
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
fun Official() {
    Box(
        modifier = Modifier
    ) {
        AsyncImage(
            model = R.drawable.bg1,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .aspectRatio(1f)
                .fadingEdge(
                    top = WindowInsets.systemBars
                        .asPaddingValues()
                        .calculateTopPadding(),
                    bottom = 64.dp
                ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .wrapContentHeight()
//                .aspectRatio(4/3f)
//                .align(Alignment.Center)
//                .background(Color.Black.copy(alpha = 0.3f))
//        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "时光雷达",
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.W900,
                    color = Color.White,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(0f, 0f),
                            blurRadius = 4f
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))

                Text(
                    text = "每日更新",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.95f),
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(0f, 0f),
                            blurRadius = 4f
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()

                )
                Spacer(Modifier.height(10.dp))

                Text(
                    text = "你曾经只爱的那些歌曲，现在你还记得吗",
                    maxLines = 2,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(0f, 0f),
                            blurRadius = 4f
                        )
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()

                )
            }
            Spacer(Modifier.height(16.dp))
            Row {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(0.5f),
                    onClick = {}
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        tint = Color.White,
                        contentDescription = null
                    )
                }

                Spacer(Modifier.widthIn(16.dp))

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(0.5f),
                    onClick = {}
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddTask,
                        tint = Color.White,
                        contentDescription = null
                    )
                }

                Spacer(Modifier.widthIn(16.dp))

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(0.5f),
                    onClick = {}
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Message,
                        tint = Color.White,
                        contentDescription = null
                    )
                }
            }
        }
    }
}


@Composable
fun PlaylistInfo(
    playlistDetail: MiniPlaylistDetail,
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

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (playlistDetail.cover.size < 5) {
            AsyncImage(
                model = playlistDetail.cover[0],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            FinalPerfectCollage(
                imageUrls = playlistDetail.cover,
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }


        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlistDetail.name,
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Spacer(Modifier.height(8.dp))

        Text(
            text = "${playlistDetail.count} 首歌曲 | 创建者: ${playlistDetail.createUserName}",
            color = MaterialTheme.colorScheme.secondary,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            maxLines = 2
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "播放量: ${playlistDetail.playCount}",
                color = MaterialTheme.colorScheme.secondary,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                maxLines = 1
            )
            Text(
                text = "收藏量: ${playlistDetail.subscribedCount}",
                color = MaterialTheme.colorScheme.secondary,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { play() }) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            }


            Button(onClick = {

            }) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            }

            if (userId == playlistDetail.creatorUserId.toString()
            ) {
                Button(onClick = {

                    if (playlistDetail.count > 500) {
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
                        ?.find { it.isFile && it.name == "${playlistDetail.id}.json" }
                    if (file != null) {
                        file.readText().let { json ->
                            val playlist =
                                Gson().fromJson(json, SimplePlaylist::class.java)
                            if (playlist.songs.size < playlistDetail.count) {
                                val difference =
                                    playlistDetail.trackIds.filterNot { a ->
                                        playlist.songs.any { s -> s.id == a.toString() }
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
                                    difference.joinToString(",")
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

                        count.intValue = playlistDetail.count
                        ids.value =
                            playlistDetail.trackIds.joinToString(",")
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


fun prepare(
    ids: String,
    scope: CoroutineScope,
    playlistDetail: MiniPlaylistDetail,
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
                id = playlistDetail.id.toString(),
                name = playlistDetail.name,
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
