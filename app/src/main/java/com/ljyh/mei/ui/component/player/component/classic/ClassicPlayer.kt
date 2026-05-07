package com.ljyh.mei.ui.component.player.component.classic

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.ui.component.player.MiniPlayer
import com.ljyh.mei.ui.component.player.component.AmbientBackground
import com.ljyh.mei.ui.component.player.overlay.PlayerOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.component.sheet.BottomSheet
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.sheet.HorizontalSwipeDirection
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo

@SuppressLint("ConfigurationScreenWidthHeight")
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(UnstableApi::class)
@Composable
fun ClassicPlayer(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    stateContainer: PlayerStateContainer,
    overlayHandler: PlayerOverlayHandler,
) {

    val device = rememberDeviceInfo()

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
            MiniPlayer(
                position = sliderPosition.toLong(),
                duration = duration,
            )
        }
    ) {

        val coverUrl = mediaMetadata?.coverUrl
        AmbientBackground(
            imageUrl = coverUrl
        )






        val layoutMode = when {
            device.isTablet && device.isLandscape -> PlayerLayoutMode.Tablet
            !device.isTablet && device.isLandscape -> PlayerLayoutMode.ImmersiveLandscape
            else -> PlayerLayoutMode.PhonePortrait
        }

//        Timber.tag("PlayerLayoutMode").d(layoutMode.name)


        when (layoutMode) {
            PlayerLayoutMode.PhonePortrait -> ClassicPhoneLayout(stateContainer, overlayHandler)
            PlayerLayoutMode.Tablet -> ClassicTabletLayout(stateContainer, overlayHandler)
            PlayerLayoutMode.ImmersiveLandscape -> ClassicImmersiveLayout(stateContainer, overlayHandler)
        }


    }
}
