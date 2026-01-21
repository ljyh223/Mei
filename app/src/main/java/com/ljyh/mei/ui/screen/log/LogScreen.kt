package com.ljyh.mei.ui.screen.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
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
    val darkTheme = isSystemInDarkTheme()

    val crashColor = if (darkTheme) Color(0xFFCF6679) else Color.Red
    val logColor = if (darkTheme) Color(0xFFBB86FC) else Color.Blue
    val secondaryTextColor = if (darkTheme) Color(0xFFB0B0B0) else Color.Gray
    val logBackgroundColor = if (darkTheme) Color(0xFF1E1E1E) else Color(0xFFEEEEEE)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日志收集") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAllLogs() }) {
                        Icon(
                            Icons.Default.Delete,
                            "Clear All",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
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
                LogFileItem(
                    file = file,
                    crashColor = crashColor,
                    logColor = logColor,
                    secondaryTextColor = secondaryTextColor,
                    onClick = { viewModel.readContent(file) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    // 如果 content 不为空，显示详情弹窗
    if (content != null) {
        LogDetailDialog(
            content = content!!,
            fileName = "日志详情",
            onDismiss = { viewModel.closeContent() },
            onShare = {
                val currentFile = fileList.find { it.readText() == content }
                if (currentFile != null) {
                    viewModel.shareCurrentFile()
                }
            },
            logBackgroundColor = logBackgroundColor
        )
    }
}

@Composable
fun LogFileItem(
    file: File,
    crashColor: Color,
    logColor: Color,
    secondaryTextColor: Color,
    onClick: () -> Unit
) {
    val isCrash = file.name.startsWith("crash")
    val date = Date(file.lastModified())
    val format = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isCrash) "崩溃错误" else "运行日志",
                    color = if (isCrash) crashColor else logColor,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = format.format(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Size: ${file.length() / 1024} KB",
                style = MaterialTheme.typography.labelSmall,
                color = secondaryTextColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailDialog(
    content: String,
    fileName: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    logBackgroundColor: Color
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onShare) {
                            Icon(
                                Icons.Default.Share,
                                "Share",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )

                // 日志内容滚动区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .background(logBackgroundColor)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = content,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
