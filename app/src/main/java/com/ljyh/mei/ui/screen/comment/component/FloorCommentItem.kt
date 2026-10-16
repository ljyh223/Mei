package com.ljyh.mei.ui.screen.comment.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.domain.FloorCommentEntry

@Composable
fun FloorCommentItem(
    comment: FloorCommentEntry,
    modifier: Modifier = Modifier,
    lightText: Boolean = false,
) {
    val primaryText = if (lightText) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryText = if (lightText) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
    val mutedText = if (lightText) Color.White.copy(alpha = 0.68f) else MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        AsyncImage(
            model = comment.user.avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.user.nickname,
                style = MaterialTheme.typography.labelMedium,
                color = secondaryText
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodySmall,
                color = primaryText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedText
                )

                val location = comment.location
                if (location.isNotEmpty()) {
                    Text(
                        text = "  IP: $location",
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedText
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (comment.likedCount > 0) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = mutedText
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = comment.likedCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedText
                    )
                }
            }
        }
    }
}
