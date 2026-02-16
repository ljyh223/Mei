package com.ljyh.mei.ui.component.player.component.classic

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.PlayerHorizontalPadding
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.component.player.component.PlayerControls
import com.ljyh.mei.ui.component.player.component.LyricScreen
import com.ljyh.mei.ui.component.player.component.PlayerActionToolbar
import com.ljyh.mei.ui.component.player.component.PlayerProgressSlider
import com.ljyh.mei.ui.component.player.component.classic.component.Cover
import com.ljyh.mei.ui.component.player.overlay.PlayerOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.model.LyricSource

@Composable
fun ClassicTabletLayout(
    stateContainer: PlayerStateContainer,
    overlayHandler: PlayerOverlayHandler
) {
    val context = LocalContext.current
    val mediaMetadata by stateContainer.mediaMetadata
    val isPlaying by stateContainer.isPlaying
    val playbackState by stateContainer.playbackState
    val sliderPosition by remember { derivedStateOf { stateContainer.sliderPosition } }
    val duration by remember { derivedStateOf { stateContainer.duration } }
    val lyricLine by remember { derivedStateOf { stateContainer.lyricLine } }


    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
    ){

        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            mediaMetadata?.let {
                Cover(
                    playerConnection = stateContainer.playerConnection,
                    mediaMetadata = it,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .aspectRatio(1f)
                )
            }

            Spacer(Modifier.height(48.dp))

            PlayerProgressSlider(
                position = sliderPosition.toLong(),
                duration = duration,
                isPlaying = isPlaying,
                onPositionChange = {
                    stateContainer.playerConnection.player.seekTo(it)
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
            )

            Spacer(Modifier.height(16.dp))

            PlayerControls(
                playerConnection = stateContainer.playerConnection,
                canSkipPrevious = stateContainer.canSkipPrevious.value,
                canSkipNext = stateContainer.canSkipNext.value,
                isPlaying = isPlaying,
                playbackState = playbackState,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
            )

            Spacer(Modifier.height(16.dp))

            PlayerActionToolbar(
                modifier = Modifier.fillMaxWidth(0.6f),
                onLyricClick = {},
                onPlaylistClick = { overlayHandler.showPlaylist() },
                onSleepTimerClick = { overlayHandler.showSleepTimer() },
                onAddToPlaylistClick = {
                    mediaMetadata?.let {
                        overlayHandler.showAddToPlaylist(it.id)
                    }
                },
                onMoreClick = {
                    overlayHandler.showMoreAction()
                }
            )
        }

        Spacer(Modifier.width(32.dp))

        LyricScreen(
            lyricData = lyricLine,
            playerConnection = stateContainer.playerConnection,
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()
                .padding(horizontal = PlayerHorizontalPadding),
            onClick = { source ->
                mediaMetadata?.let {
                    if (overlayHandler.currentOverlayValue is OverlayState.None && stateContainer.playerViewModel.searchResult.value is Resource.Success) {
                        overlayHandler.showQQMusicSelection(
                            searchResult = stateContainer.playerViewModel.searchResult.value as Resource.Success,
                            mediaMetadata = it
                        )
                    }
                }
            },
            onLongClick = { source ->
                if (source == LyricSource.QQMusic && mediaMetadata != null) {
                    stateContainer.playerViewModel.deleteSongById(id = mediaMetadata!!.id.toString())
                    Toast.makeText(context, "已删除QQ音乐歌词", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            onToggleControls = {}
        )
    }
}
