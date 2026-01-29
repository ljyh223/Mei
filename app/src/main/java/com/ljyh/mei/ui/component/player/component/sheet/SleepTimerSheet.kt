package com.ljyh.mei.ui.component.player.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.playback.PlayerConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepTimerSheet(
    playerConnection: PlayerConnection,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedOption by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // 1. 标题
            Text(
                text = "睡眠定时器",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // 2. 预估结束时间提示
            val endTimeString = remember(selectedOption) {
                val minutes = selectedOption?.toIntOrNull() ?: 0
                if (minutes > 0) {
                    val endTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                    "将在 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(endTime)) + " 停止播放"
                } else {
                    "选择定时关闭时间"
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = endTimeString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. 定时选项 (网格布局)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 3
            ) {
                val options = listOf("5", "10", "15", "30", "45", "60")
                options.forEach { option ->
                    val isSelected = option == selectedOption
                    // 使用自定义的 Box 代替 FilterChip 以获得更像音乐 App 的大按钮感
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { selectedOption = option }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$option 分钟",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. 特殊选项：播放完当前歌曲
            ListItem(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        playerConnection.service.sleepTimer.start(-1)
                        onDismiss()
                    },
                headlineContent = { Text("播放完当前歌曲后停止") },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. 确认按钮
            OutlinedButton(
                onClick = {
                    selectedOption?.toIntOrNull()?.let {
                        playerConnection.service.sleepTimer.start(it)
                    }
                    onDismiss()
                },
                enabled = selectedOption != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("开始计时", modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}