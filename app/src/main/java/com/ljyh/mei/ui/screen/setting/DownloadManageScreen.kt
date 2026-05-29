package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Pending
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.room.DownloadStatus
import com.ljyh.mei.data.model.room.DownloadTask
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.utils.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class DownloadFilter(val label: String, val icon: ImageVector) {
    ALL("全部", Icons.Rounded.Download),
    ACTIVE("进行中", Icons.Rounded.Download),
    PAUSED("已暂停", Icons.Rounded.Pause),
    COMPLETED("已完成", Icons.Rounded.CheckCircle),
    FAILED("失败", Icons.Rounded.Error),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManageScreen(
    scrollBehavior: TopAppBarScrollBehavior
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = AppDatabase.getDatabase(context)
    val allTasks by db.downloadDao().getAll().collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }

    val filteredTasks = remember(allTasks, selectedFilter) {
        when (selectedFilter) {
            DownloadFilter.ALL -> allTasks
            DownloadFilter.ACTIVE -> allTasks.filter {
                it.status == DownloadStatus.PENDING || it.status == DownloadStatus.DOWNLOADING
            }
            DownloadFilter.PAUSED -> allTasks.filter { it.status == DownloadStatus.PAUSED }
            DownloadFilter.COMPLETED -> allTasks.filter { it.status == DownloadStatus.COMPLETED }
            DownloadFilter.FAILED -> allTasks.filter { it.status == DownloadStatus.FAILED }
        }
    }

    val statusCounts = remember(allTasks) {
        DownloadFilter.entries.associateWith { filter ->
            when (filter) {
                DownloadFilter.ALL -> allTasks.size
                DownloadFilter.ACTIVE -> allTasks.count {
                    it.status == DownloadStatus.PENDING || it.status == DownloadStatus.DOWNLOADING
                }
                DownloadFilter.PAUSED -> allTasks.count { it.status == DownloadStatus.PAUSED }
                DownloadFilter.COMPLETED -> allTasks.count { it.status == DownloadStatus.COMPLETED }
                DownloadFilter.FAILED -> allTasks.count { it.status == DownloadStatus.FAILED }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("下载管理") },
                navigationIcon = {
                    com.ljyh.mei.ui.component.IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (allTasks.isNotEmpty()) {
                        IconButton(onClick = { DownloadManager.deleteAll(context) }) {
                            Icon(Icons.Rounded.Delete, "清空", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadFilter.entries.forEach { filter ->
                    val count = statusCounts[filter] ?: 0
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text("${filter.label} $count")
                        },
                        leadingIcon = {
                            Icon(
                                filter.icon,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    )
                }
            }

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 64.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        "没有${selectedFilter.label}的任务",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredTasks, key = { it.songId }) { task ->
                        DownloadTaskItem(
                            task = task,
                            onPause = { DownloadManager.pauseSong(context, task.songId) },
                            onResume = {
                                scope.launch {
                                    DownloadManager.resumeSong(context, task.songId, "恢复下载")
                                }
                            },
                            onDelete = { DownloadManager.deleteTask(context, task.songId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadTaskItem(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val (statusIcon, statusColor) = when (task.status) {
        DownloadStatus.DOWNLOADING -> Icons.Rounded.Download to MaterialTheme.colorScheme.primary
        DownloadStatus.COMPLETED -> Icons.Rounded.CheckCircle to Color(0xFF4CAF50)
        DownloadStatus.FAILED -> Icons.Rounded.Error to MaterialTheme.colorScheme.error
        DownloadStatus.PENDING -> Icons.Rounded.Pending to MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.PAUSED -> Icons.Rounded.Pause to Color(0xFFFFA726)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp)) {
            AsyncImage(
                model = task.songCover.ifEmpty { null },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            ) {
                Icon(
                    statusIcon, null,
                    tint = statusColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.songTitle.ifEmpty { task.songId },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                task.songArtist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (task.status == DownloadStatus.DOWNLOADING) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        Text(
            when (task.status) {
                DownloadStatus.PENDING -> "等待"
                DownloadStatus.DOWNLOADING -> "${task.progress}%"
                DownloadStatus.PAUSED -> "已暂停"
                DownloadStatus.COMPLETED -> "完成"
                DownloadStatus.FAILED -> "失败"
            },
            style = MaterialTheme.typography.labelSmall,
            color = statusColor
        )

        Spacer(Modifier.width(4.dp))

        when (task.status) {
            DownloadStatus.PENDING, DownloadStatus.DOWNLOADING -> {
                IconButton(onClick = onPause) {
                    Icon(Icons.Rounded.Pause, "暂停", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, "删除", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
            DownloadStatus.PAUSED -> {
                IconButton(onClick = onResume) {
                    Icon(Icons.Rounded.PlayArrow, "恢复", modifier = Modifier.size(18.dp),
                        tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, "删除", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
            DownloadStatus.COMPLETED, DownloadStatus.FAILED -> {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, "删除", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}
