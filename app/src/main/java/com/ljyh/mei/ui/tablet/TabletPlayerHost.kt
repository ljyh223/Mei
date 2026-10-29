package com.ljyh.mei.ui.tablet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.ljyh.mei.ui.component.player.BottomSheetPlayer
import com.ljyh.mei.ui.component.sheet.BottomSheetState

/** Preserve player state between opens without placing an invisible mobile hit area over the dock. */
@Composable
internal fun TabletPlayerHost(state: BottomSheetState) {
    BottomSheetPlayer(
        state = state,
        collapsedPlayerContent = {},
        modifier = Modifier.layout { measurable, constraints ->
            val player = measurable.measure(constraints)
            layout(player.width, player.height) {
                if (state.progress > 0f) player.place(0, 0)
            }
        },
    )
}
