package com.ljyh.mei.ui.component.player.component

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.PlayerHorizontalPadding
import com.ljyh.mei.constants.ProgressBarStyle
import com.ljyh.mei.constants.ProgressBarStyleKey
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.utils.rememberEnumPreference

@OptIn(UnstableApi::class)
@Composable
fun PlayerControlsSection(
    sliderPosition: Float,
    duration: Long,
    isPlaying: Boolean,
    playbackState: Int,
    playerConnection: PlayerConnection,
    onLyricClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onMoreClick: () -> Unit,
    isCompact: Boolean = false // 新增参数
) {
    // 紧凑模式下间距减半
    val spacerHeight = if (isCompact) 8.dp else 24.dp

    val (progressBarStyle, _) = rememberEnumPreference(
        key = ProgressBarStyleKey,
        defaultValue = ProgressBarStyle.WAVE
    )

    val isFM = playerConnection.isFMMode.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (progressBarStyle == ProgressBarStyle.LINEAR) {
            FluidProgressSlider(
                position = sliderPosition.toLong(),
                duration = duration,
                onPositionChange = { newPosition ->
                    playerConnection.player.seekTo(newPosition)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 8.dp)
            )
        } else {
            PlayerProgressSlider(
                position = sliderPosition.toLong(),
                duration = duration,
                isPlaying = isPlaying, // 波浪进度条需要这个参数
                onPositionChange = { newPosition ->
                    playerConnection.player.seekTo(newPosition)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 8.dp)
            )
        }


        Spacer(Modifier.height(spacerHeight))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding)
        ) {
            PlayerControls(
                playerConnection = playerConnection,
                canSkipPrevious = !isFM.value || playerConnection.player.hasPreviousMediaItem(),
                canSkipNext = playerConnection.player.hasNextMediaItem(),
                isPlaying = isPlaying,
                playbackState = playbackState,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 紧凑模式下可能需要隐藏底部 Toolbar 或者减小间距
        if (!isCompact) {
            Spacer(Modifier.height(spacerHeight))
            PlayerActionToolbar(
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                onLyricClick = onLyricClick,
                onPlaylistClick = onPlaylistClick,
                onSleepTimerClick = onSleepTimerClick,
                onAddToPlaylistClick = onAddToPlaylistClick,
                onMoreClick = onMoreClick
            )
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            Spacer(Modifier.height(16.dp))
        } else {
            // 紧凑模式底部留白少一点
            Spacer(Modifier.height(16.dp))
        }
    }
}