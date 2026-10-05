package com.ljyh.mei.ui.component.player

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.C
import com.ljyh.mei.constants.DynamicCoverKey
import com.ljyh.mei.constants.AppleMotionEnglishTitlesOnlyKey
import com.ljyh.mei.constants.KeepPlayerScreenOnKey
import com.ljyh.mei.data.model.metadata
import com.ljyh.mei.ui.component.player.component.applemusic.AppleMusicPlayer
import com.ljyh.mei.ui.component.player.component.classic.ClassicPlayer
import com.ljyh.mei.ui.component.player.overlay.CommonOverlayHandler
import com.ljyh.mei.ui.component.player.overlay.rememberOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.component.player.state.rememberPlayerStateContainer
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.utils.rememberBatteryIntensiveFeaturesAllowed
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.kyant.backdrop.Backdrop
import com.ljyh.mei.utils.rememberPreference

@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    collapsedBottomOffset: Dp = 0.dp,
    systemBottomInset: Dp = 0.dp,
    bottomNavigationVisibilityProgress: Float = 1f,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val navController = LocalNavController.current
    val device = rememberDeviceInfo()

    // 创建公共状态容器
    val stateContainer = rememberPlayerStateContainer(
        playerViewModel = playerViewModel,
        playerConnection = playerConnection
    )
    val dynamicCoverEnabled by rememberPreference(DynamicCoverKey, defaultValue = false)
    val appleEnglishTitlesOnly by rememberPreference(AppleMotionEnglishTitlesOnlyKey, defaultValue = false)
    val keepPlayerScreenOn by rememberPreference(KeepPlayerScreenOnKey, defaultValue = false)
    val batteryIntensiveFeaturesAllowed = rememberBatteryIntensiveFeaturesAllowed()
    val activity = LocalContext.current as? Activity
    val shouldKeepScreenOn = keepPlayerScreenOn && batteryIntensiveFeaturesAllowed &&
        !state.isCollapsed && state.progress > 0f
    DisposableEffect(activity, shouldKeepScreenOn) {
        if (shouldKeepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (shouldKeepScreenOn) {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    val currentMetadata by stateContainer.mediaMetadata
    val player = playerConnection.player
    val nextIndex = player.nextMediaItemIndex
    val nextItem = if (nextIndex != C.INDEX_UNSET && nextIndex < player.mediaItemCount) {
        player.getMediaItemAt(nextIndex)
    } else null
    val nextMetadata = nextItem?.metadata
    val nextId = nextItem?.mediaId?.toLongOrNull()
    val shouldLoadDynamicCover = dynamicCoverEnabled && batteryIntensiveFeaturesAllowed
    LaunchedEffect(currentMetadata?.id, nextId, nextMetadata?.album?.title,
        shouldLoadDynamicCover, appleEnglishTitlesOnly) {
        playerViewModel.loadDynamicCover(
            currentMetadata, nextMetadata, nextId, shouldLoadDynamicCover, appleEnglishTitlesOnly
        )
    }

    // 创建弹窗处理器
    val overlayHandler = rememberOverlayHandler(
        stateContainer = stateContainer,
        playlistViewModel = playlistViewModel,
        navController = navController,
        useInlineQueue = device.isTablet && device.isLandscape,
    )

    val miniPlayerBottomInset = if (bottomNavigationVisibilityProgress < 1f) {
        systemBottomInset
    } else {
        0.dp
    }
    if (device.isTablet) {
        ClassicPlayer(
            state = state,
            modifier = modifier,
            stateContainer = stateContainer,
            overlayHandler = overlayHandler,
            collapsedBottomOffset = collapsedBottomOffset,
            miniPlayerBottomInset = miniPlayerBottomInset,
        )
    } else {
        AppleMusicPlayer(
            state = state,
            modifier = modifier,
            stateContainer = stateContainer,
            overlayHandler = overlayHandler,
            collapsedBottomOffset = collapsedBottomOffset,
            miniPlayerBottomInset = miniPlayerBottomInset,
            backdrop = backdrop,
        )
    }

    // 公共的弹窗处理层
    CommonOverlayHandler(
        overlayHandler = overlayHandler,
        stateContainer = stateContainer,
        sheetState = state
    )
}
