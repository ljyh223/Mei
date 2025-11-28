package com.ljyh.mei.ui.component.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.ljyh.mei.constants.CommonImageRadius
import com.ljyh.mei.constants.PlaylistThumbnailSize
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.utils.smallImage

@Composable
fun PlaylistItem(
    playlist: Playlist,
    onClick: (String) -> Unit
) {

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable { onClick(playlist.id) },
        verticalAlignment = Alignment.CenterVertically,

        ) {
        AsyncImage(
            model = playlist.cover.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(PlaylistThumbnailSize)
                .clip(RoundedCornerShape(CommonImageRadius))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = playlist.title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.count} • ${playlist.authorName}",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}
