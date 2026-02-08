package com.ljyh.mei.ui.component.player.component.sheet


import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.PlayerActionKey
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.model.PlayerAction
import com.ljyh.mei.utils.rememberPreference
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerActionSettingsSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // 读取保存的设置
    val (actionString, onActionStringChange) = rememberPreference(
        key = PlayerActionKey,
        defaultValue = PlayerAction.toSettings(PlayerAction.defaultActions)
    )

    // 解析当前选中的 Actions
    val selectedActions = remember(actionString) {
        PlayerAction.fromSettings(actionString).toMutableStateList()
    }

    // 计算未选中的 Actions
    val availableActions = remember(selectedActions) {
        PlayerAction.entries.filter { !selectedActions.contains(it) }
    }

    // 保存逻辑
    fun save() {
        Timber.tag("PlayerActionSettingsSheet").d(PlayerAction.toSettings(selectedActions))
        onActionStringChange(PlayerAction.toSettings(selectedActions))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "自定义底部栏",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "点击图标进行添加或移除，最多选择 5 个",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // --- 区域 1: 已启用的功能 (可排序/移除) ---
            Text("显示中 (点击移除)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp) // 使用 spaceBy 而不是 SpaceEvenly 来展示紧凑效果
            ) {
                if (selectedActions.isEmpty()) {
                    Text("请至少选择一个功能", color = MaterialTheme.colorScheme.error)
                }

                selectedActions.forEach { action ->
                    ActionChip(
                        action = action,
                        isSelected = true,
                        onClick = {
                            // 移除逻辑
                            if (selectedActions.size > 1) { // 至少保留一个
                                selectedActions.remove(action)
                                save()
                            } else {
                                Toast.makeText(context, "至少保留一个功能", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- 区域 2: 可添加的功能 ---
            Text("更多功能 (点击添加)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            // 使用 FlowRow (如果图标很多) 或者 LazyRow
            // 这里假设图标不多，用 Row 简单展示，如果多可以用 FlowRow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                availableActions.forEach { action ->
                    ActionChip(
                        action = action,
                        isSelected = false,
                        onClick = {
                            // 添加逻辑
                            if (selectedActions.size < 5) { // 限制最多5个，防止挤爆屏幕
                                selectedActions.add(action)
                                save()
                            } else {
                                Toast.makeText(context, "最多只能显示 5 个功能", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActionChip(
    action: PlayerAction,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}