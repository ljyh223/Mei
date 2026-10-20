package com.ljyh.mei.ui.component.player.controls

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.PlayerActionKey
import com.ljyh.mei.playback.PlayMode
import com.ljyh.mei.ui.component.player.LocalPlayerForegroundColor
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.PlayerAction
import com.ljyh.mei.utils.preferences.rememberPreference
import timber.log.Timber

@androidx.annotation.OptIn(UnstableApi::class, ExperimentalStdlibApi::class)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerActionToolbar(
    modifier: Modifier = Modifier,
    onLyricClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val foreground = LocalPlayerForegroundColor.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current

    val (actionString, _) = rememberPreference(
        key = PlayerActionKey,
        defaultValue = PlayerAction.toSettings(PlayerAction.defaultActions)
    )
    val actions = remember(actionString) {
        val actions = PlayerAction.fromSettings(actionString)
        Timber.tag("PlayerActionToolbar").d(actionString)
        actions
    }


    // 播放模式
    val playModeValue by playerConnection.repeatMode.collectAsState()
    val playMode = remember(playModeValue) {
        PlayMode.fromInt(playModeValue) ?: PlayMode.REPEAT_MODE_ALL
    }

    val sleepTimer = playerConnection.service.sleepTimer
    val sleepTimerStatus = rememberSleepTimerStatus(sleepTimer)
    val sleepTimerEnabled = sleepTimerStatus != null



    Spacer(Modifier.height(16.dp))

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {

        actions.forEach { action ->
            when (action) {
                PlayerAction.PLAY_MODE -> {
                    ShadowedIconButton(
                        onClick = { playerConnection.switchPlayMode() }
                    ) {
                        val icon = when (playMode) {
                            PlayMode.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne // 单曲循环
                            PlayMode.REPEAT_MODE_ALL -> Icons.Rounded.Repeat    // 列表循环
                            PlayMode.SHUFFLE_MODE_ALL -> Icons.Rounded.Shuffle  // 随机播放
                        }
                        AnimatedContent(targetState = icon, label = "PlayModeIcon") { targetIcon ->
                            Icon(
                                imageVector = targetIcon,
                                contentDescription = "播放模式",
                                tint = foreground
                            )
                        }
                    }
                }

                PlayerAction.QUEUE -> {
                    ShadowedIconButton(
                        onClick = onPlaylistClick
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "播放队列",
                            tint = foreground
                        )
                    }
                }

                PlayerAction.LYRICS -> {
                    ShadowedIconButton(
                        onClick = onLyricClick
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = "Lyrics",
                            tint = foreground
                        )
                    }

                }

                PlayerAction.SLEEP_TIMER -> {
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
                                        .background(foreground.copy(alpha = 0.2f))
                                        .clickable(onClick = onSleepTimerClick)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = sleepTimerStatus.orEmpty(),
                                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                        color = foreground,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            } else {
                                // 普通图标状态
                                ShadowedIconButton(
                                    onClick = onSleepTimerClick
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bedtime,
                                        contentDescription = "睡眠定时器",
                                        tint = foreground
                                    )
                                }
                            }
                        }
                    }
                }

                PlayerAction.ADD_TO_PLAYLIST -> {
                    ShadowedIconButton(
                        onClick = onAddToPlaylistClick
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "添加到歌单",
                            tint = foreground
                        )
                    }
                }

                PlayerAction.DOWNLOAD -> {
                    ShadowedIconButton(
                        onClick = {
                            Toast.makeText(context, "暂未实现", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "下载",
                            tint = foreground
                        )
                    }
                }

                PlayerAction.MORE -> {
                    val showTimerStatus = sleepTimerStatus != null && PlayerAction.SLEEP_TIMER !in actions
                    ShadowedIconButton(
                        onClick = onMoreClick
                    ) {
                        if (showTimerStatus) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "更多功能，睡眠定时剩余 $sleepTimerStatus",
                                    tint = foreground,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = sleepTimerStatus.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    fontSize = 10.sp,
                                    color = foreground,
                                    maxLines = 1,
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "更多功能",
                                tint = foreground
                            )
                        }
                    }
                }
            }
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
