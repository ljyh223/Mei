package com.ljyh.mei.ui.component.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.UserPlaylist
import com.ljyh.mei.utils.smallImage

@Composable
fun PlaylistItem(
    playlist: UserPlaylist.Playlist,
    isPlaying: Boolean = false,
    onclick: () -> Unit
) {

    Row(
        modifier = Modifier
            .padding(8.dp)
            .height(48.dp)
            .clickable { onclick() },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,

        ) {

//        PlayingImageView(
//            imageUrl = playlist.coverImgUrl.smallImage(), // 替换为您的图片URL
//            isPlaying = isPlaying,
//            modifier = Modifier
//                .size(48.dp)
//                .clip(RoundedCornerShape(6.dp))
//        )
        AsyncImage(
            model = playlist.coverImgUrl.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = playlist.name,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.trackCount} • ${playlist.creator.nickname}",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}
