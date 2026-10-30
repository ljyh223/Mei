package com.ljyh.mei.ui.component.player

import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.ui.component.player.applemusic.AppleMusicPlayer
import com.ljyh.mei.ui.component.player.classic.ClassicPlayer
import com.ljyh.mei.ui.component.player.state.PlayerViewModel
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel

/** Phone shell adapter; tablet presentation is supplied separately by TabletPlayerHost. */
@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    collapsedBottomOffset: Dp = 0.dp,
    systemBottomInset: Dp = 0.dp,
    bottomNavigationVisibilityProgress: Float = 1f,
    compactProgress: Float = 0f,
    collapsedPlayerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
) {
    val device = rememberDeviceInfo()
    val miniPlayerBottomInset = if (bottomNavigationVisibilityProgress < 1f) systemBottomInset else 0.dp
    PlayerHost(state = state, inlinePanels = false, playerViewModel = playerViewModel, playlistViewModel = playlistViewModel) { container, overlays ->
        if (device.isLandscape) {
            ClassicPlayer(state = state, modifier = modifier, stateContainer = container,
                overlayHandler = overlays, collapsedBottomOffset = collapsedBottomOffset,
                miniPlayerBottomInset = miniPlayerBottomInset, collapsedPlayerContent = collapsedPlayerContent)
        } else {
            AppleMusicPlayer(state = state, modifier = modifier, stateContainer = container,
                overlayHandler = overlays, collapsedBottomOffset = collapsedBottomOffset,
                miniPlayerBottomInset = miniPlayerBottomInset, compactProgress = compactProgress)
        }
    }
}
