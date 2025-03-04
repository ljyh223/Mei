package com.ljyh.music.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ljyh.music.data.model.qq.u.SearchResult
import com.ljyh.music.data.model.room.QQSong
import com.ljyh.music.data.network.Resource
import com.ljyh.music.ui.component.ListDialog
import com.ljyh.music.utils.formatMilliseconds
import com.ljyh.music.utils.formatSeconds
import kotlin.math.abs

@Composable
fun DialogSelect(
    showDialog: Boolean,
    searchNew: Resource<SearchResult>?,
    viewmodel: PlayerViewModel,
    duration: Long,
    onDismiss: () -> Unit
){
    if (showDialog && searchNew is Resource.Success) {
        ListDialog(
            onDismiss = { onDismiss() }
        ) {
            val songs = searchNew.data.req0.data.body.song.list
            itemsIndexed(songs) { index, s ->
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))

                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            onDismiss()
                            viewmodel.insertSong(
                                QQSong(
                                    id = s.id.toString(),
                                    title = s.title,
                                    artist = if (s.singer.isNotEmpty()) s.singer[0].name else "",
                                    album = s.album.name,
                                    duration = s.interval,
                                )
                            )
                            viewmodel.getLyricNew(
                                id = s.id,
                                title = s.title,
                                album = s.album.name,
                                artist = if (s.singer.isNotEmpty()) s.singer[0].name else "",
                                duration = s.interval,
                            )
                        }
                        .padding(4.dp),

                    ) {
                    Text(
                        text = "${s.name}・${formatSeconds(s.interval)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${s.singer.joinToString { it.name }} - ${s.album.name}",
                        color = if (abs(formatMilliseconds(duration) - s.interval) < 15) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )
                }

                if (index < songs.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline // 分割线颜色
                    )
                }
            }


        }
    }

}