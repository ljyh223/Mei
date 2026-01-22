package com.ljyh.mei.ui.component.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// TODO 创建歌单似乎未落地
fun EditPlaylistSheet(
    isVisible: Boolean,
    defaultText: String = "",
    defaultHidden: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (text: String, hidePlaylist: Boolean) -> Unit
) {
    if (!isVisible) return

    var text by remember { mutableStateOf(defaultText) }
    var hidden by remember { mutableStateOf(defaultHidden) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null  // 不要默认的顶部小横条
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {

            // ====== 顶部按钮栏 ======
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "取消",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onDismiss() }
                )

                Text(
                    "完成",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        onConfirm(text, hidden)
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ====== 输入框 ======
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("输入新建歌单标题") }
            )

            Spacer(Modifier.height(20.dp))

            // ====== 勾选框 ======
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = hidden,
                    onCheckedChange = { hidden = it }
                )
                Text("隐私歌单")
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
