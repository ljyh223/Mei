package com.ljyh.music.ui.component.player.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.MultipleStop
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.session.legacy.PlaybackStateCompat
import androidx.media3.session.legacy.PlaybackStateCompat.SHUFFLE_MODE_ALL
import com.ljyh.music.constants.PlayerHorizontalPadding
import com.ljyh.music.extensions.togglePlayPause
import com.ljyh.music.extensions.toggleRepeatMode
import com.ljyh.music.playback.PlayMode
import com.ljyh.music.playback.PlayerConnection

@Composable
fun Controls(
    playerConnection: PlayerConnection,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isPlaying: Boolean,
    playbackState: Int,
    playMode: PlayMode
){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            IconButton(
                onClick = {
                    playerConnection.switchPlayMode(playMode)
                },
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Icon(
                    imageVector = when (playMode) {
                        PlayMode.REPEAT_MODE_OFF-> Icons.Rounded.MultipleStop
                        PlayMode.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                        PlayMode.REPEAT_MODE_ALL -> Icons.Rounded.Repeat
                        PlayMode.SHUFFLE_MODE_ALL -> Icons.Rounded.Shuffle
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                )

            }
        }

        //previous
        Box(modifier = Modifier.weight(1f)) {
            IconButton(
                enabled = canSkipPrevious,
                onClick = playerConnection::seekToPrevious,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(Modifier.width(8.dp))
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
                    .size(48.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Icon(
                    imageVector = if (playbackState == STATE_ENDED) Icons.Rounded.Replay else if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            }
        }


        Spacer(Modifier.width(8.dp))
        //next
        Box(modifier = Modifier.weight(1f)) {
            IconButton(
                enabled = canSkipNext,
                onClick = playerConnection::seekToNext,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        //repeat
        Box(modifier = Modifier.weight(1f)) {

            IconButton(
                onClick = playerConnection.player::toggleRepeatMode,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                )

            }
        }
    }
}