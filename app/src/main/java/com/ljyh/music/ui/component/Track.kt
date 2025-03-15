package com.ljyh.music.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ljyh.music.constants.UserIdKey
import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.data.model.api.GetLyric
import com.ljyh.music.data.model.parseString
import com.ljyh.music.data.network.Resource
import com.ljyh.music.ui.screen.playlist.PlaylistViewModel
import com.ljyh.music.utils.DownloadManager
import com.ljyh.music.utils.SongMate
import com.ljyh.music.utils.dataStore
import com.ljyh.music.utils.formatDuration
import com.ljyh.music.utils.get
import com.ljyh.music.utils.smallImage
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Track(
    viewModel: PlaylistViewModel,
    track: PlaylistDetail.Playlist.Track,
    onclick: () -> Unit
) {
    val playlistDetail by viewModel.playlistDetail.collectAsState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val allMePlaylist by viewModel.playlist.collectAsState()

    val userId= LocalContext.current.dataStore[UserIdKey] ?: ""

    var showBottomSheet by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        ListDialog(
            modifier = Modifier.padding(16.dp),

            onDismiss = {
                showDialog = false
            }
        ) {
            items(allMePlaylist) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            showBottomSheet = false
                            showDialog = false
                            Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                            viewModel.addSongToPlaylist(
                                pid = it.id,
                                trackIds = track.id.toString()
                            )
                        }
                ) {
                    AsyncImage(
                        model = it.cover.smallImage(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(it.title)
                        Text("${it.count}首歌曲")

                    }
                }
            }

        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {


            GridMenu(
                contentPadding = PaddingValues(
                    start = 8.dp,
                    top = 8.dp,
                    end = 8.dp,
                    bottom = 8.dp + WindowInsets.systemBars.asPaddingValues()
                        .calculateBottomPadding()
                )
            ) {
                GridMenuItem(
                    icon = Icons.Rounded.Add,
                    title = "添加到歌单",
                    onClick = {
                        viewModel.getAllMePlaylist()
                        showDialog = true
                    }
                )

                if (playlistDetail is Resource.Success &&
                    (playlistDetail as Resource.Success<PlaylistDetail>).data.playlist.creator.userId.toString() == userId) {
                    GridMenuItem(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "删除此歌曲",
                        onClick = {
                            showBottomSheet= false
                            viewModel.deleteSongFromPlaylist(
                                pid = (playlistDetail as Resource.Success<PlaylistDetail>).data.playlist.Id.toString(),
                                trackIds = track.id.toString()
                            )
                            Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                        }
                    )
                }



                GridMenuItem(
                    icon = Icons.Rounded.Lyrics,
                    title = "更新本地歌词",
                    onClick = {
                        when (val result = playlistDetail) {
                            is Resource.Success -> {
                                val path = DownloadManager.isExist(
                                    result.data.playlist.Id.toString(),
                                    result.data.playlist.name,
                                    track.id.toString()
                                )
                                if (path != "") {
                                    scope.launch {
                                        val lyric =
                                            viewModel.apiService.getLyric(
                                                GetLyric(
                                                    id = track.id.toString()
                                                )
                                            )
                                        val mLyric = lyric.parseString()
                                        if (mLyric.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "找不到可用的歌词",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@launch
                                        }
                                        SongMate.writeLyric(path, mLyric)
                                        Toast.makeText(context, "已更新", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "找不到文件", Toast.LENGTH_SHORT).show()
                                }

                            }

                            else -> {

                            }
                        }
                    }
                )

                GridMenuItem(
                    icon = Icons.Rounded.ContentCopy,
                    title = "复制id",
                    onClick = {
                        showBottomSheet= false
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("id", track.id.toString())
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    }
                )

                GridMenuItem(
                    icon = Icons.Rounded.ContentCopy,
                    title = "复制歌名",
                    onClick = {
                        showBottomSheet= false
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("name", track.name)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .height(48.dp)
            .clickable { onclick() },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,

        ) {
        AsyncImage(
            model = track.al.picUrl.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.ar.joinToString(", ") { it.name }} • ${formatDuration(track.dt)}",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }


        IconButton(onClick = {
            showBottomSheet = true
        }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


enum class Quality {
    HR,
    SQ
}