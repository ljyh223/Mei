package com.ljyh.mei.ui.component.player.component

import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.playback.PlayMode
import com.ljyh.mei.ui.component.player.PlayerViewModel
import com.ljyh.mei.ui.component.playlist.AddToPlaylistSheet
import com.ljyh.mei.ui.component.playlist.EditPlaylistSheet
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.utils.TimeUtils.makeTimeString
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerActionToolbar(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    mediaMetadata: MediaMetadata? = null,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val allMePlaylist by playlistViewModel.playlist.collectAsState()

    // 状态管理
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showEditPlaylistDialog by remember { mutableStateOf(false) }

    // 播放模式
    val playModeValue by playerConnection.repeatMode.collectAsState()
    val playMode = remember(playModeValue) {
        PlayMode.fromInt(playModeValue) ?: PlayMode.REPEAT_MODE_ALL
    }

    // 喜欢状态 (处理 null 状态)
    val isLiked by playerViewModel.like.collectAsState(initial = null)
    // 睡眠定时器逻辑
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }
    val sleepTimer = playerConnection.service.sleepTimer
    val sleepTimerEnabled = remember(sleepTimer.triggerTime, sleepTimer.pauseWhenSongEnd) {
        sleepTimer.isActive
    }

    // 监听歌曲切换更新喜欢状态
    LaunchedEffect(mediaMetadata?.id) {
        mediaMetadata?.let { playerViewModel.getLike(it.id.toString()) }
    }



    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (sleepTimer.pauseWhenSongEnd) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    sleepTimer.triggerTime - System.currentTimeMillis()
                }
                delay(1000L)
            }
        }
    }

    SleepTimerSheet(showSleepTimerDialog) { showSleepTimerDialog = false }

    // 播放列表 Sheet (Queue)
    PlaylistSheet(
        showBottomSheet = showPlaylistSheet,
        onDismiss = { showPlaylistSheet = false }
    )
    AddToPlaylistSheet(
        isVisible = showAddToPlaylistDialog,
        playlists = allMePlaylist,
        onDismiss = { showAddToPlaylistDialog = false },
        onSelectPlaylist = { selectedPlaylist ->
            // 执行添加逻辑
            if (mediaMetadata != null) {
                playlistViewModel.addSongToPlaylist(
                    pid = selectedPlaylist.id,
                    trackIds = mediaMetadata.id.toString()
                )
                Toast.makeText(
                    context,
                    "已添加到 ${selectedPlaylist.title}",
                    Toast.LENGTH_SHORT
                ).show()
                Timber.tag("Playlist").d("Added ${mediaMetadata.title} to ${selectedPlaylist.title}")
            }
            showAddToPlaylistDialog = false
        },
        onCreateNewPlaylist = {
            showEditPlaylistDialog = true
        }
    )

    EditPlaylistSheet(
        isVisible = showEditPlaylistDialog,
        defaultText = "",
        defaultHidden = false,
        onDismiss = { showEditPlaylistDialog = false },
        onConfirm = { text, hidePlaylist ->
            // 执行修改逻辑
            Toast.makeText(
                context,
                "创建歌单成功",
                Toast.LENGTH_SHORT
            ).show()
            Timber.tag("Playlist").d("Edited playlist")
            showEditPlaylistDialog = false
            // 刷新歌单
            playlistViewModel.getAllMePlaylist()

        }
    )


    Spacer(Modifier.height(16.dp))

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {

        ShadowedIconButton(
            onClick = { playerConnection.switchPlayMode() }
        ) {
            val icon = when (playMode) {
                PlayMode.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne // 单曲循环
                PlayMode.REPEAT_MODE_ALL -> Icons.Rounded.Repeat    // 列表循环
                PlayMode.SHUFFLE_MODE_ALL -> Icons.Rounded.Shuffle  // 随机播放
            }

            // 为了更好的用户体验，可以加上淡入淡出动画
            AnimatedContent(targetState = icon, label = "PlayModeIcon") { targetIcon ->
                Icon(
                    imageVector = targetIcon,
                    contentDescription = "播放模式",
                    tint = Color.White
                )
            }
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(onClick = sleepTimer::clear)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = makeTimeString(sleepTimerTimeLeft),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
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
                tint = if (isLikedSafe) Color.White else Color.White.copy(alpha = 0.7f)
            )
        }

        // 5. 添加到歌单 (Add)
        ShadowedIconButton(
            onClick = {
                showAddToPlaylistDialog = true
                playlistViewModel.getAllMePlaylist()
            }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepTimerSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedOption by remember { mutableStateOf<String?>(null) }

    if (isVisible) {
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
}