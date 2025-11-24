package com.ljyh.mei.ui.component.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.PlaylistDetail
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.component.ListDialog
import com.ljyh.mei.utils.smallImage

@Composable
fun AddToPlaylistDialog(
    isVisible: Boolean,
    playlists: List<Playlist>, // 数据由外部传入
    onDismiss: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit // 选中回调
) {
    if (isVisible) {
        ListDialog(
            modifier = Modifier.padding(16.dp),
            onDismiss = onDismiss
        ) {
            items(playlists) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPlaylist(playlist) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = playlist.cover.smallImage(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(text = playlist.title, fontSize = 14.sp)
                        Text(text = "${playlist.count}首", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}