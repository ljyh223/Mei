package com.ljyh.mei.ui.component.player.component.sheet

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.metadata
import com.ljyh.mei.extensions.mediaItems
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.utils.TimeUtils.formatDuration
import com.ljyh.mei.utils.smallImage
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.media3.common.util.UnstableApi

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistBottomSheet(
    onDismiss: () -> Unit
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState()
    val mediaItems = remember {
        mutableStateListOf<MediaItem>().apply {
            addAll(playerConnection.player.mediaItems)
        }
    }

    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        mediaItems.move(from.index, to.index)
        playerConnection.player.moveMediaItem(from.index, to.index)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
    val currentMediaItemIndex by playerConnection.currentMediaItemIndex.collectAsState()

    LaunchedEffect(playerConnection) {
        playerConnection.player.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                val newItems = playerConnection.player.mediaItems
                mediaItems.clear()
                mediaItems.addAll(newItems)
            }
        })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(
                    bottom = WindowInsets.systemBars.asPaddingValues()
                        .calculateBottomPadding()
                )
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "播放列表 (${mediaItems.size}首)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 播放列表
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                state = lazyListState,
            ) {
                itemsIndexed(
                    mediaItems,
                    key = { _, item -> item.mediaId }) { index, mediaItem ->
                    ReorderableItem(
                        reorderableLazyListState,
                        key = mediaItem.mediaId
                    ) { isDragging ->
                        mediaItem.metadata?.let {
                            PlaylistItem(
                                modifier = Modifier.longPressDraggableHandle(
                                    onDragStarted = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    },
                                    onDragStopped = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    },
                                ),
                                metadata = it,
                                isCurrentPlaying = index == currentMediaItemIndex,
                                onItemClick = {
                                    playerConnection.player.seekToDefaultPosition(index)
                                    playerConnection.player.playWhenReady = true
                                },
                                onRemoveClick = {
                                    if (mediaItems.size > 1) {
                                        playerConnection.player.removeMediaItem(index)
                                        mediaItems.removeAt(index)
                                        if (index == currentMediaItemIndex) {
                                            playerConnection.seekToNext()
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "不能移除最后一首歌曲",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistItem(
    modifier: Modifier,
    metadata: MediaMetadata,
    isCurrentPlaying: Boolean,
    onItemClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val backgroundColor = if (isCurrentPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    val textColor = if (isCurrentPlaying) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onItemClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 歌曲封面
        AsyncImage(
            model = metadata.coverUrl.smallImage(),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
        )

        // 歌曲信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = metadata.title,
                fontSize = 14.sp,
                fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${metadata.artists.joinToString(", ") { it.name }} • ${
                    formatDuration(
                        metadata.duration
                    )
                }",
                fontSize = 12.sp,
                color = if (isCurrentPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 移除按钮
        IconButton(
            onClick = onRemoveClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "移除",
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val item = removeAt(from)
    add(to, item)
}