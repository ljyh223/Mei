package com.ljyh.mei.ui.component.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljyh.mei.constants.CommonImageRadius
import com.ljyh.mei.constants.TrackThumbnailSize
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.ui.component.playlist.PlayingImageView
import com.ljyh.mei.utils.smallImage

@Composable
fun Track(
    track: MediaMetadata,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPlaying) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 封面图 / 播放状态
        PlayingImageView(
            imageUrl = track.coverUrl.smallImage(),
            isPlaying = isPlaying,
            modifier = Modifier
                .size(TrackThumbnailSize)
                .clip(RoundedCornerShape(CommonImageRadius))
        )

        // 2. 文字信息区域
        Column(
            modifier = Modifier.weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // 标题行
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title,
                    fontSize = 16.sp, // 标题字号加大
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    // 正在播放时使用 Primary 颜色高亮
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // 别名/翻译 (TNS)
                if (!track.tns.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${track.tns})",
                        fontSize = 13.sp,
                        color = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = buildString {
                    append(track.artists.joinToString(" / ") { it.name })
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }


        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "更多选项",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}