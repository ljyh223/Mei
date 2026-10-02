package com.ljyh.mei.ui.component.player

import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.C
import com.ljyh.mei.constants.DynamicCoverKey
import com.ljyh.mei.data.model.metadata
import com.ljyh.mei.ui.component.player.component.applemusic.AppleMusicPlayer
import com.ljyh.mei.ui.component.player.component.classic.ClassicPlayer
import com.ljyh.mei.ui.component.player.overlay.CommonOverlayHandler
import com.ljyh.mei.ui.component.player.overlay.rememberOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.component.player.state.rememberPlayerStateContainer
import com.ljyh.mei.ui.component.sheet.BottomSheetState
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
    val currentMetadata by stateContainer.mediaMetadata
    val player = playerConnection.player
    val nextIndex = player.nextMediaItemIndex
    val nextItem = if (nextIndex != C.INDEX_UNSET && nextIndex < player.mediaItemCount) {
        player.getMediaItemAt(nextIndex)
    } else null
    val nextMetadata = nextItem?.metadata
    val nextId = nextItem?.mediaId?.toLongOrNull()
    LaunchedEffect(currentMetadata?.id, nextId, dynamicCoverEnabled) {
        playerViewModel.loadDynamicCover(currentMetadata, nextMetadata, nextId, dynamicCoverEnabled)
    }

    // 创建弹窗处理器
    val overlayHandler = rememberOverlayHandler(
        stateContainer = stateContainer,
        playlistViewModel = playlistViewModel,
        navController = navController,
        useInlineQueue = device.isTablet && device.isLandscape,
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (systemBottomInset > 0.dp && !state.isDismissed &&
            bottomNavigationVisibilityProgress < 1f
        ) {
            // The collapsed sheet ends above the gesture inset. Cover the content behind that
            // inset with the same surface as the mini player while the tab bar is hidden.
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(systemBottomInset),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp,
            ) {}
        }

        if (device.isTablet) {
            ClassicPlayer(
                state = state,
                modifier = Modifier.fillMaxSize(),
                stateContainer = stateContainer,
                overlayHandler = overlayHandler,
                collapsedBottomOffset = collapsedBottomOffset,
            )
        } else {
            AppleMusicPlayer(
                state = state,
                modifier = Modifier.fillMaxSize(),
                stateContainer = stateContainer,
                overlayHandler = overlayHandler,
                collapsedBottomOffset = collapsedBottomOffset,
                backdrop = backdrop,
            )
        }
    }

    // 公共的弹窗处理层
    CommonOverlayHandler(
        overlayHandler = overlayHandler,
        stateContainer = stateContainer,
        sheetState = state
    )
}
