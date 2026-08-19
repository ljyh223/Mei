package com.ljyh.mei.ui.component.player

import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
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

@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
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

    // 创建弹窗处理器
    val overlayHandler = rememberOverlayHandler(
        stateContainer = stateContainer,
        playlistViewModel = playlistViewModel,
        navController = navController
    )

    if (device.isTablet) {
        ClassicPlayer(
            state = state,
            modifier = modifier,
            stateContainer = stateContainer,
            overlayHandler = overlayHandler,
        )
    } else {
        AppleMusicPlayer(
            state = state,
            modifier = modifier,
            stateContainer = stateContainer,
            overlayHandler = overlayHandler,
        )
    }

    // 公共的弹窗处理层
    CommonOverlayHandler(
        overlayHandler = overlayHandler,
        stateContainer = stateContainer,
        sheetState = state
    )
}
