package com.ljyh.mei.ui.component.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.PlayModeKey
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.playback.PlayMode
import com.ljyh.mei.ui.component.player.component.PlaylistSheet
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.utils.TimeUtils.makeTimeString
import com.ljyh.mei.utils.rememberPreference
import androidx.hilt.navigation.compose.hiltViewModel
import com.ljyh.mei.ui.component.player.component.PlayerBottomSheet
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerActionToolbar(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel,
    mediaMetadata: MediaMetadata? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return

    // 状态管理
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showTrackBottomSheet by remember { mutableStateOf(false) }

    // 播放模式
    var playModeValue by rememberPreference(PlayModeKey, 2) // 默认列表循环
    val playMode =
        remember(playModeValue) { PlayMode.fromInt(playModeValue) ?: PlayMode.REPEAT_MODE_ALL }

    // 喜欢状态 (处理 null 状态)
    val isLiked by playerViewModel.like.collectAsState(initial = null)

    // 歌单 ViewModel
    val playlistViewModel: PlaylistViewModel = hiltViewModel()

    // 监听歌曲切换更新喜欢状态
    LaunchedEffect(mediaMetadata?.id) {
        mediaMetadata?.let { playerViewModel.getLike(it.id.toString()) }
    }

    // 睡眠定时器逻辑
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }
    val sleepTimer = playerConnection.service.sleepTimer
    val sleepTimerEnabled = remember(sleepTimer.triggerTime, sleepTimer.pauseWhenSongEnd) {
        sleepTimer.isActive
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (sleepTimer.pauseWhenSongEnd) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    sleepTimer.triggerTime - System.currentTimeMillis()
                }
                delay(1000L) // 每秒更新
            }
        }
    }

    // --- 弹窗与 Sheet ---
    if (showSleepTimerDialog) {
        SleepTimerDialog(onDismiss = { showSleepTimerDialog = false })
    }

    // 播放列表 Sheet (Queue)
    PlaylistSheet(
        showBottomSheet = showPlaylistSheet,
        onDismiss = { showPlaylistSheet = false }
    )

    mediaMetadata?.let {
        PlayerBottomSheet(
            showBottomSheet = showTrackBottomSheet,
            viewModel = playlistViewModel,
            playerViewModel = playerViewModel,
            mediaMetadata = it,
            onDismiss = { showTrackBottomSheet = false }
        )
    }

    Spacer(Modifier.height(16.dp))

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {

        // 1. 播放模式切换
        ShadowedIconButton(
            onClick = { playModeValue = playerConnection.switchPlayMode(playMode) }
        ) {
            Icon(
                imageVector = when (playMode) {
                    PlayMode.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                    PlayMode.REPEAT_MODE_ALL -> Icons.Rounded.Repeat
                    PlayMode.SHUFFLE_MODE_ALL -> Icons.Rounded.Shuffle
                },
                contentDescription = "播放模式",
                tint = Color.White
            )
        }

        // 2. 播放队列 (Queue)
        ShadowedIconButton(
            onClick = { showPlaylistSheet = true }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = "播放队列",
                tint = Color.White
            )
        }

        // 3. 睡眠定时器 (带动画切换)
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = sleepTimerEnabled,
                label = "sleepTimer"
            ) { isEnabled ->
                if (isEnabled) {
                    // 倒计时状态：显示胶囊形状的时间
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.2f)) // 半透明背景
                            .clickable(onClick = sleepTimer::clear)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = makeTimeString(sleepTimerTimeLeft),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // 普通图标状态
                    ShadowedIconButton(
                        onClick = { showSleepTimerDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = "睡眠定时器",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // 4. 喜欢 (Like)
        ShadowedIconButton(
            onClick = {
                mediaMetadata?.let { playerViewModel.like(id = it.id.toString()) }
            }
        ) {

            val isLikedSafe = if (isLiked == null) false else true
            Icon(
                imageVector = if (isLikedSafe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "喜欢",
                // 喜欢状态用高亮色(如红色)还是保持白色
                // 既然是流体背景，保持白色最稳妥，或者用 Accent Color
                tint = if (isLikedSafe) Color.White else Color.White.copy(alpha = 0.7f)
            )
        }

        // 5. 添加到歌单 (Add)
        ShadowedIconButton(
            onClick = { showTrackBottomSheet = true }
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "添加到歌单",
                tint = Color.White
            )
        }
    }
}

@Composable
fun ShadowedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(48.dp)
    ) {
        Box(
            modifier = Modifier
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class) // FlowRow 需要这个
@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var selectedOption by remember { mutableStateOf<String?>(null) } // 默认选 "0" 或 null

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Bedtime, contentDescription = null) },
        title = { Text("睡眠定时器", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 预览时间
                val endTimeString = remember(selectedOption) {
                    val minutes = selectedOption?.toIntOrNull() ?: 0
                    if (minutes > 0) {
                        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                        "将在 " + SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                        ).format(Date(endTime)) + " 停止播放"
                    } else {
                        "选择停止时间"
                    }
                }

                Text(
                    text = endTimeString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 选项网格 (使用 FlowRow 自动换行)
                FlowRow(
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val options = listOf("5", "10", "15", "30", "45", "60")
                    options.forEach { option ->
                        FilterChip(
                            selected = option == selectedOption,
                            onClick = {
                                selectedOption = if (selectedOption == option) null else option
                            },
                            label = { Text("$option 分") },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 特殊选项：播完当前首
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        playerConnection.service.sleepTimer.start(-1) // -1 代表播完当前
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("播放完当前歌曲后停止")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    selectedOption?.toIntOrNull()?.let {
                        playerConnection.service.sleepTimer.start(it)
                    }
                },
                enabled = selectedOption != null
            ) {
                Text("开始计时")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

