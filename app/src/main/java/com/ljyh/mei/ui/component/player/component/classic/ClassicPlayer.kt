package com.ljyh.mei.ui.component.player.component.classic

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ljyh.mei.constants.FloatingCapsuleHorizontalPadding
import com.ljyh.mei.constants.FloatingCapsuleMiniPlayerHeight
import com.ljyh.mei.ui.component.FloatingCapsulePlayerBarContent
import com.ljyh.mei.ui.component.player.component.FluidBackground
import com.ljyh.mei.ui.component.player.overlay.PlayerOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.component.sheet.BottomSheet
import com.ljyh.mei.ui.component.sheet.BottomSheetMorphSpec
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.sheet.HorizontalSwipeDirection
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.audio.AudioVisualizerManager


@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(UnstableApi::class)
@Composable
fun ClassicPlayer(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    stateContainer: PlayerStateContainer,
    overlayHandler: PlayerOverlayHandler,
    collapsedBottomOffset: Dp = 0.dp,
) {

    val device = rememberDeviceInfo()
    val navController = LocalNavController.current

    val isSystemInDarkTheme = isSystemInDarkTheme()



    // --- 从状态容器获取数据 ---
    val mediaMetadata by stateContainer.mediaMetadata
    val sliderPosition by remember { derivedStateOf { stateContainer.sliderPosition } }
    val duration by remember { derivedStateOf { stateContainer.duration } }

    // 背景颜色计算
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = remember(isSystemInDarkTheme, state.value, state.collapsedBound) {
        if (isSystemInDarkTheme && state.value > state.collapsedBound) {
            lerp(colorScheme.surfaceContainer, Color.Black, state.progress)
        } else {
            colorScheme.surfaceContainer
        }
    }




    BottomSheet(
        state = state,
        modifier = modifier,
        backgroundColor = backgroundColor,
        onDismiss = {
            stateContainer.playerConnection.player.stop()
            stateContainer.playerConnection.player.clearMediaItems()
        },
        onHorizontalSwipe = { direction ->
            when (direction) {
                HorizontalSwipeDirection.Left -> stateContainer.playerConnection.seekToNext()
                HorizontalSwipeDirection.Right -> stateContainer.playerConnection.seekToPrevious()
            }
        },
        collapsedContent = {
            FloatingCapsulePlayerBarContent(
                title = mediaMetadata?.title,
                artist = mediaMetadata?.artists?.joinToString { it.name },
                coverUrl = mediaMetadata?.coverUrl,
                isPlaying = stateContainer.isPlaying.value,
                canSkipNext = stateContainer.canSkipNext.value,
                onClick = state::expandSoft,
                onPlayPause = {
                    val player = stateContainer.playerConnection.player
                    if (player.isPlaying) player.pause() else player.play()
                },
                onNext = stateContainer.playerConnection::seekToNext,
            )
        },
        morphSpec = BottomSheetMorphSpec(
            collapsedHorizontalMargin = FloatingCapsuleHorizontalPadding,
            collapsedMaxWidth = if (device.isTablet && device.isLandscape) 560.dp else null,
            collapsedCornerRadius = 24.dp,
            expandedHorizontalMargin = 0.dp,
            expandedCornerRadius = 0.dp,
            collapsedHeight = FloatingCapsuleMiniPlayerHeight,
            collapsedBottomMargin = 8.dp + collapsedBottomOffset,
            expandedBottomMargin = 0.dp,
        )
    ) {

        val coverUrl = mediaMetadata?.coverUrl
        val isPlaying by stateContainer.isPlaying
        val context = LocalContext.current

        val audioVisualizerManager = remember { AudioVisualizerManager(context) }

        LaunchedEffect(stateContainer.playerConnection.player) {
            val player = stateContainer.playerConnection.player as? ExoPlayer
            player?.audioSessionId?.let { sessionId ->
                audioVisualizerManager.attachToPlayer(sessionId)
            }
        }

        FluidBackground(
            imageUrl = coverUrl,
            audioVisualizerManager = audioVisualizerManager,
            isPlaying = isPlaying
        )


        val layoutMode = when {
            device.isTablet && device.isLandscape -> PlayerLayoutMode.Tablet
            !device.isTablet && device.isLandscape -> PlayerLayoutMode.ImmersiveLandscape
            else -> PlayerLayoutMode.PhonePortrait
        }

//        Timber.tag("PlayerLayoutMode").d(layoutMode.name)


        when (layoutMode) {
            PlayerLayoutMode.PhonePortrait -> ClassicPhoneLayout(stateContainer, overlayHandler)
            PlayerLayoutMode.Tablet -> ClassicTabletLayout(
                stateContainer = stateContainer,
                overlayHandler = overlayHandler,
                onViewAllComments = { songId ->
                    overlayHandler.showLyrics()
                    state.collapseThen {
                        Screen.Comment.navigate(navController) {
                            addPath(songId.toString())
                        }
                    }
                },
            )
            PlayerLayoutMode.ImmersiveLandscape -> ClassicImmersiveLayout(stateContainer, overlayHandler)
        }


    }
}
