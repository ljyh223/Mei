package com.ljyh.mei.ui.component.player

import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.ui.component.player.applemusic.AppleMusicPlayer
import com.ljyh.mei.ui.component.player.state.PlayerViewModel
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.ui.model.PlayerActionProfile
import com.ljyh.mei.ui.tablet.player.TabletPlayerScreen

/** Phone player presentation; tablet presentation is supplied separately by TabletPlayerHost. */
@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
internal fun BottomSheetPlayer(
    state: BottomSheetState,
    collapsedBottomOffset: Dp = 0.dp,
    systemBottomInset: Dp = 0.dp,
    bottomNavigationVisibilityProgress: Float = 1f,
    compactProgress: Float = 0f,
    wideContent: Boolean = false,
    expansionOrigin: PlayerExpansionOrigin? = null,
    miniArtwork: Painter? = null,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
) {
    val miniPlayerBottomInset = if (bottomNavigationVisibilityProgress < 1f) systemBottomInset else 0.dp
    PlayerHost(
        state = state,
        inlinePanels = wideContent,
        actionProfile = PlayerActionProfile.Phone,
        playerViewModel = playerViewModel,
        playlistViewModel = playlistViewModel,
    ) { container, overlays ->
        if (wideContent) TabletPlayerScreen(
            sheet = state,
            container = container,
            overlays = overlays,
            shortWindowLayout = true,
            expansionOrigin = expansionOrigin,
            miniArtwork = miniArtwork,
            modifier = modifier.layout { measurable, constraints ->
                val player = measurable.measure(constraints)
                layout(player.width, player.height) {
                    if (state.progress > 0f) player.place(0, 0)
                }
            },
        ) else AppleMusicPlayer(
            state = state,
            modifier = modifier,
            stateContainer = container,
            overlayHandler = overlays,
            collapsedBottomOffset = collapsedBottomOffset,
            miniPlayerBottomInset = miniPlayerBottomInset,
            compactProgress = compactProgress,
        )
    }
}
