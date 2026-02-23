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
import com.ljyh.mei.ui.component.player.component.LyricScreen
import com.ljyh.mei.ui.component.player.component.classic.component.Cover
import com.ljyh.mei.ui.component.player.overlay.PlayerOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.model.LyricSource

@Composable
fun ClassicImmersiveLayout(
    stateContainer: PlayerStateContainer,
    overlayHandler: PlayerOverlayHandler
) {
    val context = LocalContext.current
    val mediaMetadata by stateContainer.mediaMetadata
    val lyricLine by remember { derivedStateOf { stateContainer.lyricLine } }


    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
    ) {


        mediaMetadata?.let {
            Column(
                modifier = Modifier.fillMaxHeight()
                    .fillMaxWidth(0.45f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Cover(
                    playerConnection = stateContainer.playerConnection,
                    mediaMetadata = it,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .aspectRatio(1f),
                    onDoubleClick = { offset, width ->

                        val player = stateContainer.playerConnection.player
                        val third = width / 3f

                        when {
                            offset.x < third -> {
                                // 左侧：上一首
                                if (player.hasPreviousMediaItem()) {
                                    player.seekToPreviousMediaItem()
                                }
                            }

                            offset.x < third * 2 -> {
                                player.playWhenReady = !player.playWhenReady
                            }

                            else -> {
                                // 右侧：下一首
                                if (player.hasNextMediaItem()) {
                                    player.seekToNextMediaItem()
                                }
                            }
                        }
                    }
                )
            }

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
            controlsVisible = stateContainer.controlsVisible,
            onToggleControls = {},
        )
    }
}