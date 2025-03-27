package com.ljyh.music.ui.component

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ljyh.music.data.model.MediaMetadata
import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.ui.component.player.component.TrackBottomSheet
import com.ljyh.music.ui.screen.playlist.PlaylistViewModel
import com.ljyh.music.utils.TimeUtils.formatDuration
import com.ljyh.music.utils.smallImage


@Composable
fun Track(
    viewModel: PlaylistViewModel,
    track: PlaylistDetail.Playlist.Track,
    onclick: () -> Unit
) {

    var showBottomSheet by remember { mutableStateOf(false) }
    TrackBottomSheet(
        showBottomSheet,
        viewModel,
        MediaMetadata(
            id = track.id,
            title = track.name,
            coverUrl = track.al.picUrl,
            artists = track.ar.map { MediaMetadata.Artist(it.Id, it.name) },
            duration = track.dt,
            album = MediaMetadata.Album(track.al.Id, track.al.name),
            explicit = false,
        ),
    ) {
        showBottomSheet = false
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
            Row {
                Text(
                    text = track.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!track.tns.isNullOrEmpty()) {
                    Text(
                        text = "(${track.tns[0]})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

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