package com.ljyh.mei.ui.component.player.controls

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.R
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
    onPlaylistClick: () -> Unit,
    showLyricsIcon: Boolean = false,
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
                        PlayMode.REPEAT_MODE_ONE -> R.drawable.ic_player_repeat_one // 单曲循环
                        PlayMode.REPEAT_MODE_ALL -> R.drawable.ic_player_repeat    // 列表循环
                        PlayMode.SHUFFLE_MODE_ALL -> R.drawable.ic_player_shuffle  // 随机播放
                    }
                    AnimatedContent(targetState = icon, label = "PlayModeIcon") { targetIcon ->
                        Icon(
                            painter = painterResource(targetIcon),
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
                        painter = painterResource(R.drawable.ic_player_previous),
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
                        painter = painterResource(R.drawable.ic_player_next),
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
                        imageVector = if (showLyricsIcon) Icons.Rounded.Lyrics
                        else Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = if (showLyricsIcon) "歌词" else "播放队列",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
