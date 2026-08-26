package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.auth.QrLoginUiState
import com.ljyh.mei.ui.component.QrCode

@Composable
fun QrLoginDialog(
    state: QrLoginUiState,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val loginUrl = when (state) {
        is QrLoginUiState.Waiting -> state.loginUrl
        is QrLoginUiState.Scanned -> state.loginUrl
        is QrLoginUiState.Expired -> state.loginUrl
        is QrLoginUiState.Error -> state.loginUrl
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "扫码登录网易云音乐",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(216.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(12.dp),
                ) {
                    if (loginUrl != null) {
                        QrCode(
                            content = loginUrl,
                            modifier = Modifier.size(192.dp),
                        )
                    } else {
                        when (state) {
                            QrLoginUiState.Loading, QrLoginUiState.Idle ->
                                CircularProgressIndicator()

                            is QrLoginUiState.Success -> Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(64.dp),
                            )

                            is QrLoginUiState.Error -> Icon(
                                imageVector = Icons.Rounded.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp),
                            )

                            else -> Unit
                        }
                    }

                    if (state is QrLoginUiState.Scanned) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            tonalElevation = 4.dp,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("已扫码，请在手机上确认")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = state.statusText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (state) {
                        is QrLoginUiState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "只有一台设备时，可先截图，再到网易云音乐 App 的扫一扫中从相册识别。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            if (state is QrLoginUiState.Expired || state is QrLoginUiState.Error) {
                TextButton(onClick = onRefresh) {
                    Text("刷新二维码")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private fun QrLoginUiState.statusText(): String = when (this) {
    QrLoginUiState.Idle, QrLoginUiState.Loading -> "正在获取二维码…"
    is QrLoginUiState.Waiting -> "打开网易云音乐 App，扫一扫登录"
    is QrLoginUiState.Scanned -> nickname
        ?.takeIf(String::isNotBlank)
        ?.let { "$it，请在手机上确认" }
        ?: "已扫码，请在手机上确认"

    is QrLoginUiState.Expired -> "二维码已失效，请刷新"
    is QrLoginUiState.Success -> nickname
        ?.takeIf(String::isNotBlank)
        ?.let { "登录成功，欢迎回来，$it" }
        ?: "登录成功"

    is QrLoginUiState.Error -> message
}
