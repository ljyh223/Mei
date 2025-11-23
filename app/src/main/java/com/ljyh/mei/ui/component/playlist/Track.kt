package com.ljyh.mei.ui.component.playlist

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onClick: () -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    // 建议：如果 TrackBottomSheet 比较重，可以考虑将其状态提升到 PlaylistScreen
    if (showBottomSheet) {
        TrackBottomSheet(
            show = showBottomSheet,
            viewModel = viewModel,
            track = track,
            onDismiss = { showBottomSheet = false }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 如果正在播放，可以给一个淡淡的背景色（可选）
            .background(if (isPlaying) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp), // 增加垂直间距，更符合手指触控
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 封面图 / 播放状态
        PlayingImageView(
            imageUrl = track.coverUrl.smallImage(),
            isPlaying = isPlaying,
            modifier = Modifier
                .size(50.dp) // 稍微加大一点尺寸
                .clip(RoundedCornerShape(8.dp)) // 圆角稍微大一点，更圆润
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 2. 文字信息区域
        Column(
            modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(1f, fill = false) // 让标题优先显示，但不强占所有空间
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

            Spacer(modifier = Modifier.height(4.dp)) // 标题和副标题之间的间距

            // 副标题行 (歌手 • 时长)
            Text(
                text = buildString {
                    append(track.artists.joinToString(" / ") { it.name }) // 通常使用 / 分隔歌手
                    // 如果有专辑名也可以加在这里
                    // append(" - ${track.album}")
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant, // 使用 Variant 颜色，对比度更好
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3. 右侧操作区 (时长 或 菜单)
        // 建议：可以在这里显示时长，把菜单放在最右边，或者只放菜单
        // 这里我保留了你的菜单按钮，但你可以考虑把 formatDuration 放在这里，如果不拥挤的话

        IconButton(
            onClick = { showBottomSheet = true },
            modifier = Modifier.size(24.dp) // 限制 IconButton 的点击区域视觉大小，防止占位太大
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