package com.ljyh.mei.ui.component.player.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player.STATE_ENDED
import com.ljyh.mei.extensions.togglePlayPause
import com.ljyh.mei.playback.PlayerConnection

@Composable
fun Controls(
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

        }
    }

}