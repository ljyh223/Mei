package com.ljyh.mei.ui.component.player.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.extensions.togglePlayPause
import com.ljyh.mei.playback.PlayerConnection

@OptIn(UnstableApi::class)
@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    playerConnection: PlayerConnection,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isPlaying: Boolean,
    playbackState: Int,
){
    Box(modifier = modifier){
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {


            //previous
            Box(modifier = Modifier.weight(1f)) {
                PlayerControlButton(
                    enabled = canSkipPrevious,
                    onClick = playerConnection::seekToPrevious,
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "上一首",
                        tint = androidx.compose.material3.LocalContentColor.current,
                        modifier = Modifier.align(Alignment.Center)
                            .size(28.dp)

                    )
                }
            }
            //play/pause
            Box(modifier = Modifier.weight(1f)) {
                PlayerControlButton(
                    onClick = {
                        if (playbackState == STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.player.togglePlayPause()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center),
                    size = 76.dp
                ) {
                    Icon(
                        imageVector = if (playbackState == STATE_ENDED) Icons.Rounded.Replay else if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = androidx.compose.material3.LocalContentColor.current,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(42.dp)
                    )
                }
            }


            //next
            Box(modifier = Modifier.weight(1f)) {
                PlayerControlButton(
                    enabled = canSkipNext,
                    onClick = playerConnection::seekToNext,
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "下一首",
                        tint = androidx.compose.material3.LocalContentColor.current,
                        modifier = Modifier.align(Alignment.Center)
                            .size(28.dp)
                    )
                }
            }

        }
    }

}
