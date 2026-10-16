package com.ljyh.mei.ui.component.player.component.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.MoreActionOrderKey
import com.ljyh.mei.ui.component.player.component.rememberSleepTimerStatus
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.MoreAction
import com.ljyh.mei.utils.preferences.rememberPreference
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreActionsSheet(
    onDismissRequest: () -> Unit,
    onActionClick: (MoreAction) -> Unit,
) {
    val sleepTimerStatus = LocalPlayerConnection.current?.let {
        rememberSleepTimerStatus(it.service.sleepTimer)
    }
    val (savedOrder, saveOrder) = rememberPreference(MoreActionOrderKey, defaultValue = "")
    var actions by remember { mutableStateOf(MoreAction.entries.toList()) }
    val draft = remember { mutableStateListOf<MoreAction>() }
    var editing by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        draft.moveAction(from.index, to.index)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target -> target != SheetValue.Hidden || !editing },
    )
    LaunchedEffect(savedOrder) { actions = MoreAction.orderedBy(savedOrder) }

    fun leaveEdit() {
        editing = false
        draft.clear()
    }

    val sheetHeight = minOf(LocalConfiguration.current.screenHeightDp.dp * 0.52f, 440.dp)
    ModalBottomSheet(
        onDismissRequest = {
            if (editing) leaveEdit() else onDismissRequest()
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        BackHandler(enabled = editing) { leaveEdit() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (editing) {
                    IconButton(onClick = ::leaveEdit) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "取消调整顺序")
                    }
                }
                Text(
                    text = if (editing) "调整顺序" else "更多操作",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (!editing && sleepTimerStatus != null) {
                    TextButton(onClick = { onActionClick(MoreAction.SLEEP_TIMER) }) {
                        Text(
                            text = "定时 $sleepTimerStatus",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        )
                    }
                }
                if (editing) {
                    TextButton(onClick = {
                        val newOrder = draft.toList()
                        actions = newOrder
                        saveOrder(newOrder.joinToString(",") { it.id })
                        leaveEdit()
                    }) { Text("完成") }
                } else {
                    IconButton(onClick = {
                        draft.clear()
                        draft.addAll(actions)
                        editing = true
                    }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "调整操作顺序")
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                if (editing) {
                    itemsIndexed(draft, key = { _, action -> action.id }) { index, action ->
                        ReorderableItem(reorderState, key = action.id) { isDragging ->
                            Surface(
                                color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                                shadowElevation = if (isDragging) 4.dp else 0.dp,
                            ) {
                                ListItem(
                                    headlineContent = { Text(action.label) },
                                    leadingContent = { Icon(action.icon, contentDescription = null) },
                                    trailingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .longPressDraggableHandle(
                                                    onDragStarted = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                                    },
                                                    onDragStopped = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                                    },
                                                )
                                                .semantics(mergeDescendants = true) {
                                                    contentDescription = "调整${action.label}顺序"
                                                    customActions = buildList {
                                                        if (index > 0) add(CustomAccessibilityAction("上移") {
                                                            draft.moveAction(index, index - 1)
                                                            true
                                                        })
                                                        if (index < draft.lastIndex) add(CustomAccessibilityAction("下移") {
                                                            draft.moveAction(index, index + 1)
                                                            true
                                                        })
                                                    }
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(Icons.Rounded.DragHandle, contentDescription = null)
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                )
                            }
                        }
                    }
                } else {
                    items(actions, key = MoreAction::id) { action ->
                        MoreActionRow(action) { onActionClick(action) }
                    }
                }
            }

            if (editing) {
                TextButton(
                    onClick = {
                        draft.clear()
                        draft.addAll(MoreAction.entries)
                    },
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                ) { Text("恢复默认") }
            }
        }
    }
}

@Composable
private fun MoreActionRow(action: MoreAction, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(action.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        },
        leadingContent = {
            Icon(action.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun MutableList<MoreAction>.moveAction(from: Int, to: Int) {
    if (from == to || from !in indices || to !in indices) return
    add(to, removeAt(from))
}
