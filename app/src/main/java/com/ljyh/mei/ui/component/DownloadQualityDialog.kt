package com.ljyh.mei.ui.component

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.DownloadQuality
import com.ljyh.mei.download.Android10LyricTree

@Composable
fun DownloadConfirmDialog(
    currentQuality: DownloadQuality,
    downloadPath: String,
    onDismiss: () -> Unit,
    onConfirm: (saveSeparateLyrics: Boolean) -> Unit,
    onGoToSettings: () -> Unit,
    onGoToDownloadManage: () -> Unit = {}
) {
    val context = LocalContext.current
    var saveSeparateLyrics by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        saveSeparateLyrics = Environment.isExternalStorageManager()
        if (!saveSeparateLyrics) {
            Toast.makeText(context, "未获得所有文件访问权限，已取消单独保存歌词", Toast.LENGTH_SHORT).show()
        }
    }
    val musicFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        saveSeparateLyrics = uri != null && Android10LyricTree.persistTree(context, uri)
        if (uri != null && !saveSeparateLyrics) {
            Toast.makeText(context, "请选择内部存储的 Music 或 Music/Mei 文件夹", Toast.LENGTH_SHORT).show()
        }
    }
    fun requestLyricStorage() {
        if (Build.VERSION.SDK_INT == 29) {
            if (Android10LyricTree.grantedTree(context) != null) saveSeparateLyrics = true
            else musicFolderLauncher.launch(null)
            return
        }
        if (Environment.isExternalStorageManager()) {
            saveSeparateLyrics = true
            return
        }
        val appSettings = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        try {
            permissionLauncher.launch(appSettings)
        } catch (_: Exception) {
            permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认下载") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "当前音质",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = currentQuality.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentQuality.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "保存位置",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = downloadPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = saveSeparateLyrics,
                        onCheckedChange = { checked ->
                            if (checked) requestLyricStorage() else saveSeparateLyrics = false
                        },
                    )
                    Column {
                        Text("单独保存歌词文件")
                        Text(
                            if (Build.VERSION.SDK_INT == 29)
                                "与歌曲同目录保存为 .lrc 或 .ttml；请选择内部存储的 Music 文件夹"
                            else "与歌曲同目录保存为 .lrc 或 .ttml；开启需授权访问所有文件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (saveSeparateLyrics &&
                    ((Build.VERSION.SDK_INT == 29 && Android10LyricTree.grantedTree(context) == null) ||
                        (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()))
                ) requestLyricStorage()
                else onConfirm(saveSeparateLyrics)
            }) {
                Text("开始下载")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onGoToSettings) {
                    Text("修改设置")
                }
                TextButton(onClick = onGoToDownloadManage) {
                    Text("查看队列")
                }
            }
        }
    )
}
