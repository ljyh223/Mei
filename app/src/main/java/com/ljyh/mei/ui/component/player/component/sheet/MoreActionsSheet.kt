package com.ljyh.mei.ui.component.player.component.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.player.PlayerViewModel
import com.ljyh.mei.ui.model.MoreAction
import com.ljyh.mei.ui.model.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreActionsSheet(
    onDismissRequest: () -> Unit,
    onActionClick: (MoreAction) -> Unit,
    viewModel: PlayerViewModel
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val sortOrder by viewModel.moreSortOrder.collectAsState()
    val moreActions by viewModel.sortedMoreActions.collectAsState()
    val primaryActions = listOf(MoreAction.ADD_TO_PLAYLIST, MoreAction.VIEW_PLAYLIST, MoreAction.COMMENT)
    val secondaryActions = moreActions.filterNot { it in primaryActions }
    val expanded = sheetState.currentValue == SheetValue.Expanded
    val sheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.82f

    // 排序 Dialog 状态
    var showSortOptions by remember { mutableStateOf(false) }

    if (showSortOptions) {
        SortOptionsDialog(
            currentOrder = sortOrder,
            onOrderSelected = {
                viewModel.setMoreSortOrder(it)
                showSortOptions = false
            },
            onDismiss = { showSortOptions = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {

            // --- 1. 标题栏 (手动实现简单的 Header) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "更多操作",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // 排序按钮
                IconButton(onClick = { showSortOptions = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "排序设置",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            primaryActions.forEach { action ->
                MoreActionRow(action) { onActionClick(action) }
            }
            androidx.compose.material3.HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            if (!expanded) {
                Text(
                    text = "上拉查看更多",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (expanded) items(items = secondaryActions, key = { it.id }) { action ->
                    MoreActionRow(action) { onActionClick(action) }
                }
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

@Composable
private fun SortOptionsDialog(
    currentOrder: SortOrder,
    onOrderSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择排序方式") },
        text = {
            Column {
                SortOptionRow(
                    text = "使用频率",
                    selected = currentOrder == SortOrder.FREQUENCY,
                    onClick = { onOrderSelected(SortOrder.FREQUENCY) }
                )
                SortOptionRow(
                    text = "风险等级",
                    selected = currentOrder == SortOrder.RISK,
                    onClick = { onOrderSelected(SortOrder.RISK) }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}

@Composable
private fun SortOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(text = text)
    }
}
