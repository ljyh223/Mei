package com.ljyh.mei.ui.component.playlist

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
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.utils.TimeUtils.formatDuration
import com.ljyh.mei.utils.smallImage


@Composable
fun Track(
    viewModel: PlaylistViewModel,
    track: MediaMetadata,
    isPlaying: Boolean = false,
    onclick: () -> Unit
) {

    var showBottomSheet by remember { mutableStateOf(false) }
    TrackBottomSheet(
        showBottomSheet,
        viewModel,
        track,
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

        PlayingImageView(
            imageUrl = track.coverUrl.smallImage(), // 替换为您的图片URL
            isPlaying = isPlaying,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!track.tns.isNullOrEmpty()) {
                    Text(
                        text = "(${track.tns})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "${track.artists.joinToString(", ") { it.name }} • ${formatDuration(track.duration)}",
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