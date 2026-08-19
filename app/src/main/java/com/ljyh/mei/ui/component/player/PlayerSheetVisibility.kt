package com.ljyh.mei.ui.component.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.component.sheet.BottomSheetState

@Composable
fun SyncPlayerSheetVisibility(
    playerConnection: PlayerConnection?,
    sheetState: BottomSheetState,
) {
    DisposableEffect(playerConnection, sheetState) {
        val player = playerConnection?.player ?: return@DisposableEffect onDispose { }

        fun sync() {
            if (player.currentMediaItem == null) {
                if (!sheetState.isDismissed) sheetState.dismiss()
            } else if (sheetState.isDismissed) {
                sheetState.collapseSoft()
            }
        }

        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = sync()

            override fun onTimelineChanged(timeline: Timeline, reason: Int) = sync()
        }

        player.addListener(listener)
        sync()
        onDispose { player.removeListener(listener) }
    }
}
