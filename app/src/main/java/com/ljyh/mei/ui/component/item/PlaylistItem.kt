package com.ljyh.mei.ui.component.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.PlaylistThumbnailSize
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.utils.image.smallImage
import com.ljyh.mei.ui.component.layout.meiPageGutter

@Composable
fun PlaylistItem(
    playlist: Playlist,
    onClick: (String) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth().heightIn(min = 80.dp)
            .clickable { onClick(playlist.id) }
            .padding(horizontal = meiPageGutter(), vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,

        ) {
        AsyncImage(
            model = playlist.cover.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(PlaylistThumbnailSize)
                .clip(MaterialTheme.shapes.small)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.count} 首 · ${playlist.authorName}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}
