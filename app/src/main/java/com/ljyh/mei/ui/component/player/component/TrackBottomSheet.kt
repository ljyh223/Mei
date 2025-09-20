package com.ljyh.mei.ui.component.player.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.PlaylistDetail
import com.ljyh.mei.data.model.api.GetLyric
import com.ljyh.mei.data.model.parseString
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.GridMenu
import com.ljyh.mei.ui.component.GridMenuItem
import com.ljyh.mei.ui.component.ListDialog
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.utils.DownloadManager
import com.ljyh.mei.utils.SongMate
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.get
import com.ljyh.mei.utils.smallImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackBottomSheet (
    showBottomSheet: Boolean,
    viewModel: PlaylistViewModel,
    mediaMetadata: MediaMetadata,
    onDismiss: () -> Unit
){
    val userId = LocalContext.current.dataStore[UserIdKey] ?: ""
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val playlistDetail by viewModel.playlistDetail.collectAsState()
    val allMePlaylist by viewModel.playlist.collectAsState()
    var showDialog by remember {
        mutableStateOf(false)
    }
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
                            onDismiss()
                            showDialog = false
                            Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                            Log.d(
                                "Playlist ADD",
                                "添加的歌曲id:${mediaMetadata.id} 名字:${mediaMetadata.title} playlistId:${it.id} playlistName:${it.title}"
                            )
                            viewModel.addSongToPlaylist(
                                pid = it.id,
                                trackIds = mediaMetadata.id.toString()
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
                        Text(
                            text = it.title,
                            maxLines = 1,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${it.count}首歌曲",
                            maxLines = 1,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                            )

                    }
                }
            }

        }
    }
    if (showBottomSheet) {
        ModalBottomSheet(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            onDismissRequest = onDismiss,
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
                    (playlistDetail as Resource.Success<PlaylistDetail>).data.playlist.creator.userId.toString() == userId
                ) {
                    GridMenuItem(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "删除此歌曲",
                        onClick = {
                            onDismiss()
                            Log.d("Playlist DEL", "删除的歌曲id:${mediaMetadata.id} 名字:${mediaMetadata.title}")
                            viewModel.deleteSongFromPlaylist(
                                pid = (playlistDetail as Resource.Success<PlaylistDetail>).data.playlist.Id.toString(),
                                trackIds = mediaMetadata.id.toString()
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
                                    mediaMetadata.id.toString()
                                )
                                if (path != "") {
                                    scope.launch {
                                        val lyric =
                                            viewModel.apiService.getLyric(
                                                GetLyric(
                                                    id = mediaMetadata.id.toString()
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
                        onDismiss()
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("id", mediaMetadata.id.toString())
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    }
                )

                GridMenuItem(
                    icon = Icons.Rounded.ContentCopy,
                    title = "复制歌名",
                    onClick = {
                        onDismiss()
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("name", mediaMetadata.title)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }



}