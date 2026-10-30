package com.ljyh.mei.ui.tablet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.ljyh.mei.ui.component.player.PlayerHost
import com.ljyh.mei.ui.tablet.player.TabletPlayerScreen
import com.ljyh.mei.ui.component.sheet.BottomSheetState

/** Preserve player state between opens without placing an invisible mobile hit area over the dock. */
@Composable
internal fun TabletPlayerHost(state: BottomSheetState) {
    PlayerHost(state = state, inlinePanels = true) { container, overlays ->
        TabletPlayerScreen(
            sheet = state, container = container, overlays = overlays,
            modifier = Modifier.layout { measurable, constraints ->
                val player = measurable.measure(constraints)
                layout(player.width, player.height) {
                    if (state.progress > 0f) player.place(0, 0)
                }
            },
        )
    }
}
