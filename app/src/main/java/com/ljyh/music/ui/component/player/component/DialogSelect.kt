package com.ljyh.music.ui.component.player.component

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ljyh.music.data.model.qq.u.SearchResult
import com.ljyh.music.data.model.room.QQSong
import com.ljyh.music.data.network.Resource
import com.ljyh.music.ui.component.player.PlayerViewModel
import com.ljyh.music.utils.TimeUtils.formatMilliseconds
import com.ljyh.music.utils.TimeUtils.formatSeconds
import kotlin.math.abs

@Composable
fun DialogSelect(
    id: String,
    showDialog: Boolean,
    searchNew: Resource<SearchResult>,
    viewmodel: PlayerViewModel,
    duration: Long,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val maxHeight = screenHeight / 2

            Surface(
                modifier = Modifier.padding(24.dp),
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    when (searchNew) {
                        is Resource.Error -> {
                            ErrorState(message = searchNew.message)
                        }
                        is Resource.Loading -> {
                            LoadingState()
                        }
                        is Resource.Success -> {
                            SuccessState(
                                songs = searchNew.data.req0.data.body.song.list,
                                duration = duration,
                                maxHeight = maxHeight,
                                onSongClick = { song ->
                                    onDismiss()
                                    viewmodel.insertSong(
                                        QQSong(
                                            id = id,
                                            qid = song.id.toString(),
                                            title = song.title,
                                            artist = song.singer.firstOrNull()?.name ?: "",
                                            album = song.album.name,
                                            duration = song.interval,
                                        )
                                    )
                                    Log.d("DialogSelect", "insert song info")
                                    viewmodel.getLyricNew(
                                        id = song.id,
                                        title = song.title,
                                        album = song.album.name,
                                        artist = song.singer.firstOrNull()?.name ?: "",
                                        duration = song.interval,
                                    )
                                    Log.d("DialogSelect", "get lyric")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Text(text = message, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun LoadingState() {
    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
}

@Composable
private fun SuccessState(
    songs: List<SearchResult.Req0.Data.Body.Song.S>,
    duration: Long,
    maxHeight: Dp,
    onSongClick: (SearchResult.Req0.Data.Body.Song.S) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
    ) {
        itemsIndexed(songs) { index, song ->
            SongItem(
                song = song,
                duration = duration,
                onClick = { onSongClick(song) }
            )

        }
    }
}

@Composable
private fun SongItem(
    song: SearchResult.Req0.Data.Body.Song.S,
    duration: Long,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
//            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Text(
            text = "${song.name}・${formatSeconds(song.interval)}",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${song.singer.joinToString { it.name }} - ${song.album.name}",
            color = if (abs(formatMilliseconds(duration) - song.interval) < 15) {
                Color.Gray.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
        )
    }
}