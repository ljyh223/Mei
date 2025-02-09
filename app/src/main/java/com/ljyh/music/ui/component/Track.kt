package com.ljyh.music.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.data.model.parseString
import com.ljyh.music.data.network.Resource
import com.ljyh.music.ui.screen.playlist.PlaylistViewModel
import com.ljyh.music.utils.DownloadManager
import com.ljyh.music.utils.SongMate
import com.ljyh.music.utils.formatDuration
import com.ljyh.music.utils.smallImage
import kotlinx.coroutines.launch


@Composable
fun Track(
    viewModel: PlaylistViewModel,
    track: PlaylistDetail.Playlist.Track,
    onclick: () -> Unit
) {
    val playlistDetail by viewModel.playlistDetail.collectAsState()
    var isMenuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current


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
            isMenuExpanded = true
        }) {

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("update lyric") },
                    onClick = {
                        isMenuExpanded=false
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
                                            viewModel.apiService.getLyric(track.id.toString())
                                        val mLyric = lyric.parseString()
                                        if(mLyric.isEmpty()){
                                            Toast.makeText(context, "找不到可用的歌词", Toast.LENGTH_SHORT).show()
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
                    },
                )

                DropdownMenuItem(
                    text = { Text("copy id") },
                    onClick = {
                        isMenuExpanded=false

                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("id", track.id.toString())
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    })


                DropdownMenuItem(
                    text = { Text("save cover") },
                    onClick = {
                        Toast.makeText(context, "还未实现", Toast.LENGTH_SHORT).show()
                        isMenuExpanded=false
                    })
            }


            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }

}
