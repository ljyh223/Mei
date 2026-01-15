package com.ljyh.mei.ui.screen.setting.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.ui.local.LocalNavController
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    viewModel: LogViewModel = hiltViewModel()
) {
    val fileList by viewModel.logFiles.collectAsState()
    val content by viewModel.currentFileContent.collectAsState()
    val navController = LocalNavController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日志收集") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAllLogs() }) {
                        Icon(Icons.Default.Delete, "Clear All")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(fileList) { file ->
                LogFileItem(file = file) {
                    viewModel.readContent(file)
                }
                Divider()
            }
        }
    }

    // 如果 content 不为空，显示详情弹窗
    if (content != null) {
        LogDetailDialog(
            content = content!!,
            fileName = "日志详情", // 也可以存当前文件名
            onDismiss = { viewModel.closeContent() },
            onShare = {
                // 这里需要知道当前是哪个文件，简化起见，你可以在VM里存currentFile
                // 或者简单处理：分享时需要传递 file 对象，这里为演示逻辑
                // 实际使用建议在 VM 记录 currentSelectedFile
                // 下面是一个假设的逻辑，你需要让VM记录当前选中的文件
                val currentFile = fileList.find { it.readText() == content }
                if (currentFile != null) {
                    viewModel.shareFile(currentFile)
                }
            }
        )
    }
}

@Composable
fun LogFileItem(file: File, onClick: () -> Unit) {
    val isCrash = file.name.startsWith("crash")
    val date = Date(file.lastModified())
    val format = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isCrash) "崩溃错误" else "运行日志",
                color = if (isCrash) Color.Red else Color.Blue,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = format.format(date),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = file.name, style = MaterialTheme.typography.bodySmall)
        Text(
            text = "Size: ${file.length() / 1024} KB",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailDialog(
    content: String,
    fileName: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(fileName, style = MaterialTheme.typography.titleSmall) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, "Close")
                        }
                    },
                    actions = {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, "Share")
                        }
                    }
                )

                // 日志内容滚动区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .background(Color(0xFFEEEEEE)) // 浅灰背景模拟控制台
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = content,
                        fontFamily = FontFamily.Monospace, // 等宽字体更像日志
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}