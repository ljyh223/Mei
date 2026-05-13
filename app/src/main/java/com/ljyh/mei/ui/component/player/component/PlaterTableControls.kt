package com.ljyh.mei.ui.component.player.component


import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.extensions.togglePlayPause
import com.ljyh.mei.playback.PlayMode
import com.ljyh.mei.playback.PlayerConnection

@OptIn(UnstableApi::class)
@Composable
fun PlayerTableControls(
    modifier: Modifier = Modifier,
    playerConnection: PlayerConnection,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isPlaying: Boolean,
    playbackState: Int,
    onPlaylistClick: () -> Unit
){
    val playModeValue by playerConnection.repeatMode.collectAsState()
    val playMode = remember(playModeValue) {
        PlayMode.fromInt(playModeValue) ?: PlayMode.REPEAT_MODE_ALL
    }
    Box(modifier = modifier){
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {

            Box(modifier = Modifier.weight(1f)){
                IconButton(
                    modifier = Modifier.size(36.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(4.dp)),
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
                            tint = Color.White
                        )
                    }
                }
            }


            //previous
            Box(modifier = Modifier.weight(1f)) {
                IconButton(
                    enabled = canSkipPrevious,
                    onClick = playerConnection::seekToPrevious,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                            .size(48.dp)

                    )
                }
            }
            //play/pause
            Box(modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = {
                        if (playbackState == STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.player.togglePlayPause()
                        }
                    },
                    modifier = Modifier
                        .size(84.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = if (playbackState == STATE_ENDED) Icons.Rounded.Replay else if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(84.dp)
                    )
                }
            }


            //next
            Box(modifier = Modifier.weight(1f)) {
                IconButton(
                    enabled = canSkipNext,
                    onClick = playerConnection::seekToNext,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                            .size(48.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)){
                IconButton(
                    onClick = onPlaylistClick,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "播放队列",
                        tint = Color.White
                    )
                }
            }
        }
    }
}