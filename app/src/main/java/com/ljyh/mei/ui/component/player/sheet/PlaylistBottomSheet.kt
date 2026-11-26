package com.ljyh.mei.ui.component.player.sheet

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.metadata
import com.ljyh.mei.playback.queue.queueSource
import com.ljyh.mei.ui.component.sheet.MeiAdaptiveSheet
import com.ljyh.mei.ui.component.sheet.meiSheetContentHeight
import com.ljyh.mei.ui.component.sheet.rememberMeiAdaptiveSheetState
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.utils.TimeUtils.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PlaylistContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showTitleBar: Boolean = true,
    titleTopPadding: Dp = 8.dp,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val queue by playerConnection.queueState.collectAsState()
    val rows = queue.items
    val latestRows = rememberUpdatedState(rows)
    val currentIndex = queue.currentIndex
    var initiallyPositioned by rememberSaveable(playerConnection) { mutableStateOf(false) }

    LaunchedEffect(rows.size, currentIndex) {
        if (!initiallyPositioned && rows.isNotEmpty()) {
            lazyListState.scrollToItem(currentIndex.coerceIn(0, rows.lastIndex))
            initiallyPositioned = true
        }
    }

    LaunchedEffect(playerConnection, lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.takeIf { it.isNotEmpty() }?.let { visible ->
                val first = visible.first().index
                val last = visible.last().index
                ((first - 2)..(last + 4)).mapNotNull { index ->
                    latestRows.value.getOrNull(index)?.takeIf { it.item.metadata == null }?.entryId
                }
            }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collectLatest { entryIds ->
                if (entryIds.isNotEmpty()) {
                    delay(100L)
                    playerConnection.loadQueueMetadata(entryIds)
                }
            }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val source = latestRows.value.getOrNull(from) ?: return
        val target = latestRows.value.getOrNull(to) ?: return
        if (playerConnection.moveQueueEntry(source.entryId, target.entryId)) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        moveQueueItem(from.index, to.index)
    }

    val currentSource = rows.getOrNull(currentIndex)?.item?.queueSource?.displayText
    Column(modifier = modifier) {
        if (showTitleBar) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = titleTopPadding, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "播放队列 · ${rows.size} 首",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (currentSource != null) {
                        Text(
                            text = currentSource,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭播放队列")
                }
            }
        }

        if (rows.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("播放队列为空", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp),
                state = lazyListState,
            ) {
                itemsIndexed(rows, key = { _, row -> row.entryId }) { index, row ->
                    ReorderableItem(reorderState, key = row.entryId) { isDragging ->
                        QueueTrackRow(
                            item = row.item,
                            isCurrentPlaying = row.entryId == queue.currentEntryId,
                            isDragging = isDragging,
                            dragHandle = Modifier
                                .longPressDraggableHandle(
                                    onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                                    onDragStopped = { haptics.performHapticFeedback(HapticFeedbackType.GestureEnd) },
                                )
                                .semantics {
                                    contentDescription = "调整歌曲顺序"
                                    customActions = buildList {
                                        if (index > 0) add(CustomAccessibilityAction("上移") {
                                            moveQueueItem(index, index - 1)
                                            true
                                        })
                                        if (index < rows.lastIndex) add(CustomAccessibilityAction("下移") {
                                            moveQueueItem(index, index + 1)
                                            true
                                        })
                                    }
                                },
                            onItemClick = { playerConnection.seekToQueueEntry(row.entryId) },
                            onRemoveClick = {
                                if (latestRows.value.size <= 1) {
                                    Toast.makeText(context, "不能移除最后一首歌曲", Toast.LENGTH_SHORT).show()
                                } else playerConnection.removeQueueEntry(row.entryId)
                            },
                        )
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistBottomSheet(onDismiss: () -> Unit, embedded: Boolean = false) {
    val sheetState = rememberMeiAdaptiveSheetState()
    val closeSheet: () -> Unit = if (embedded) onDismiss else sheetState::hide
    val sheetContent: @Composable () -> Unit = {
        PlaylistContent(
            onDismiss = closeSheet,
            modifier = Modifier
                .fillMaxWidth()
                .height(meiSheetContentHeight(.78f)),
        )
    }
    if (embedded) {
        sheetContent()
    } else {
        MeiAdaptiveSheet(onDismissRequest = onDismiss, state = sheetState) {
            sheetContent()
        }
    }
}

@Composable
private fun QueueTrackRow(
    item: MediaItem,
    isCurrentPlaying: Boolean,
    isDragging: Boolean,
    dragHandle: Modifier,
    onItemClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    val metadata: MediaMetadata? = item.metadata
    val background = when {
        isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh
        isCurrentPlaying -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val titleColor = if (isCurrentPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val subtitle = metadata?.let {
        val artists = it.artists.joinToString(", ") { artist -> artist.name }
        if (artists.isBlank()) formatDuration(it.duration) else "$artists · ${formatDuration(it.duration)}"
    } ?: "正在获取歌曲信息"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onItemClick)
            .padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (metadata?.coverUrl?.isNotBlank() == true) {
            AsyncImage(
                model = metadata.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = metadata?.title ?: item.mediaMetadata.title?.toString() ?: "正在加载歌曲",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Medium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(modifier = dragHandle.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.DragHandle, contentDescription = "长按调整顺序", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRemoveClick, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "从队列移除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
