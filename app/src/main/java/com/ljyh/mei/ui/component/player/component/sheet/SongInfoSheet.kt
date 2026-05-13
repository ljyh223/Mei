package com.ljyh.mei.ui.component.player.component.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.utils.TimeUtils
import com.ljyh.mei.utils.setClipboard
import com.ljyh.mei.utils.image.saveImageToGallery
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoSheet(
    metadata: MediaMetadata,
    qqSongId: String?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "歌曲信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 封面图片 + 下载按钮
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.BottomEnd
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(metadata.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Surface(
                    shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                ) {
                    IconButton(
                        onClick = {
                            scope.launch { saveImageToGallery(context, metadata.coverUrl) }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "保存封面",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            // 基本信息
            SectionLabel("基本信息")
            InfoItem(
                label = "歌曲 ID",
                value = metadata.id.toString(),
                monospace = true,
                onCopy = { setClipboard(context, metadata.id.toString(), "歌曲 ID") }
            )
            InfoItem(
                label = "歌曲名称",
                value = metadata.title,
                onCopy = { setClipboard(context, metadata.title, "歌曲名称") }
            )
            metadata.tns?.let { tns ->
                InfoItem(
                    label = "译名",
                    value = tns,
                    onCopy = { setClipboard(context, tns, "译名") }
                )
            }

            // QQ 音乐
            if (qqSongId != null) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                SectionLabel("QQ 音乐")
                InfoItem(
                    label = "QQ 歌曲 ID",
                    value = qqSongId,
                    monospace = true,
                    onCopy = { setClipboard(context, qqSongId, "QQ 歌曲 ID") }
                )
            }

            // 歌手
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            SectionLabel("歌手")
            metadata.artists.forEach { artist ->
                InfoItem(
                    label = "歌手",
                    value = artist.name,
                    onCopy = { setClipboard(context, artist.name, "歌手") }
                )
                InfoItem(
                    label = "歌手 ID",
                    value = artist.id.toString(),
                    monospace = true,
                    onCopy = { setClipboard(context, artist.id.toString(), "歌手 ID") }
                )
            }

            // 专辑
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            SectionLabel("专辑")
            InfoItem(
                label = "专辑名称",
                value = metadata.album.title,
                onCopy = { setClipboard(context, metadata.album.title, "专辑名称") }
            )
            InfoItem(
                label = "专辑 ID",
                value = metadata.album.id.toString(),
                monospace = true,
                onCopy = { setClipboard(context, metadata.album.id.toString(), "专辑 ID") }
            )

            // 其他
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            SectionLabel("其他")
            InfoItem(
                label = "时长",
                value = TimeUtils.formatDuration(metadata.duration),
                onCopy = { setClipboard(context, TimeUtils.formatDuration(metadata.duration), "时长") }
            )
            InfoItem(
                label = "Explicit",
                value = if (metadata.explicit) "是" else "否",
                onCopy = { setClipboard(context, if (metadata.explicit) "是" else "否", "Explicit") }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    monospace: Boolean = false,
    onCopy: () -> Unit
) {
    val valueFontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default

    ListItem(
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        supportingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = valueFontFamily,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "复制",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
