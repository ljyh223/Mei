package com.ljyh.mei.ui.component.player

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.C
import com.ljyh.mei.constants.DynamicCoverSources
import com.ljyh.mei.constants.AppleMotionEnglishTitlesOnlyKey
import com.ljyh.mei.constants.KeepPlayerScreenOnKey
import com.ljyh.mei.data.model.domain.metadata
import com.ljyh.mei.ui.component.player.overlay.PlayerOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.component.player.overlay.CommonOverlayHandler
import com.ljyh.mei.ui.component.player.overlay.rememberOverlayHandler
import com.ljyh.mei.ui.component.player.state.rememberPlayerStateContainer
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.utils.rememberBatteryIntensiveFeaturesAllowed
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.ui.component.player.state.PlayerViewModel
import com.ljyh.mei.utils.preferences.rememberPreference
import com.ljyh.mei.utils.preferences.rememberDynamicCoverSettings

/** Shared session owner. Form-factor layouts never create another player, request loop or overlay host. */
@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
internal fun PlayerHost(
    state: BottomSheetState,
    inlinePanels: Boolean,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    content: @Composable (PlayerStateContainer, PlayerOverlayHandler) -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val navController = LocalNavController.current
    val context = LocalContext.current
    LaunchedEffect(playerViewModel, context) {
        playerViewModel.likeFeedback.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 创建公共状态容器
    val stateContainer = rememberPlayerStateContainer(
        playerViewModel = playerViewModel,
        playerConnection = playerConnection
    )
    val dynamicCoverSettings by rememberDynamicCoverSettings()
    val appleEnglishTitlesOnly by rememberPreference(AppleMotionEnglishTitlesOnlyKey, defaultValue = false)
    val keepPlayerScreenOn by rememberPreference(KeepPlayerScreenOnKey, defaultValue = false)
    val batteryIntensiveFeaturesAllowed = rememberBatteryIntensiveFeaturesAllowed()
    val activity = context as? Activity
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
    val activeCoverSources = if (batteryIntensiveFeaturesAllowed) {
        dynamicCoverSettings.activeSources
    } else DynamicCoverSources.Disabled
    LaunchedEffect(currentMetadata?.id, nextId, nextMetadata?.album?.title,
        activeCoverSources, appleEnglishTitlesOnly) {
        playerViewModel.loadDynamicCover(
            currentMetadata, nextMetadata, nextId, activeCoverSources, appleEnglishTitlesOnly
        )
    }

    // 创建弹窗处理器
    val overlayHandler = rememberOverlayHandler(
        stateContainer = stateContainer,
        playlistViewModel = playlistViewModel,
        navController = navController,
        useInlineQueue = inlinePanels,
    )

    content(stateContainer, overlayHandler)
    CommonOverlayHandler(overlayHandler = overlayHandler, stateContainer = stateContainer, sheetState = state)
}
