package com.ljyh.mei.ui.component.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljyh.mei.constants.CommonImageRadius
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.playlist.PlayingImageView
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.utils.TimeUtils.formatDuration
import com.ljyh.mei.utils.image.smallImage
@Composable
fun Track(
    track: MediaMetadata,
    index: Int? = null,
    isTablet: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    TrackContent(
        track = track,
        index = index,
        isTablet = isTablet,
        isPlaying = isPlaying,
        isLoading = false,
        onClick = onClick,
        onMoreClick = onMoreClick,
    )
}

@Composable
fun TrackPlaceholder(
    index: Int,
    isTablet: Boolean = false,
) {
    TrackContent(
        track = null,
        index = index,
        isTablet = isTablet,
        isPlaying = false,
        isLoading = true,
        onClick = {},
        onMoreClick = {},
    )
}

@Composable
private fun TrackContent(
    track: MediaMetadata?,
    index: Int?,
    isTablet: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPlaying) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent)
            .clickable(enabled = !isLoading) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- 1. 序号 (仅平板) ---
        if (isTablet && index != null) {
            Text(
                text = (index + 1).toString(),
                modifier = Modifier.width(36.dp).skeleton(isLoading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }

        // --- 2. 封面与标题 ---
        // 手机端不使用 weight(4f)，而是占据剩余空间 weight(1f)
        Row(
            modifier = Modifier.weight(if (isTablet) 4f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                Spacer(
                    modifier = Modifier
                        .size(if (isTablet) 40.dp else 48.dp)
                        .skeleton(true, RoundedCornerShape(CommonImageRadius))
                )
            } else {
                PlayingImageView(
                    imageUrl = track!!.coverUrl.smallImage(),
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .size(if (isTablet) 40.dp else 48.dp)
                        .clip(RoundedCornerShape(CommonImageRadius))
                )
            }

            Column(
                modifier = Modifier.padding(start = 16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track?.title ?: "歌曲标题占位文字",
                    modifier = Modifier.skeleton(isLoading),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (isTablet) 15.sp else 16.sp
                    ),
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = track?.artists?.joinToString(" / ") { it.name } ?: "歌手名称",
                    modifier = Modifier.skeleton(isLoading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- 3. 专辑列 (仅平板) ---
        if (isTablet) {
            Text(
                text = track?.album?.title ?: "专辑名称",
                modifier = Modifier.weight(3f).padding(horizontal = 8.dp).skeleton(isLoading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // --- 4. 时长列 (仅平板) ---
        if (isTablet) {
            Text(
                text = track?.let { formatDuration(it.duration) } ?: "00:00",
                modifier = Modifier.width(60.dp).skeleton(isLoading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }

        // --- 5. 更多按钮 ---
        IconButton(
            onClick = onMoreClick,
            enabled = !isLoading,
            modifier = Modifier.padding(start = 8.dp).size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "更多",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).skeleton(isLoading, CircleShape)
            )
        }
    }
}
