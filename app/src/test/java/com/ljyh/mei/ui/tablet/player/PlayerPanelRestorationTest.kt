package com.ljyh.mei.ui.tablet.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import com.ljyh.mei.ui.component.player.overlay.PlayerPanel
import com.ljyh.mei.ui.component.player.overlay.rememberPlayerPanelState
import com.ljyh.mei.ui.tablet.TabletContentTestActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class PlayerPanelRestorationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun selectedPanelRestoresAfterActivityRecreation() {
        var panel: MutableState<PlayerPanel>? = null
        TabletContentTestActivity.content = { MaterialTheme { val current = rememberPlayerPanelState(); SideEffect { panel = current } } }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle()
            compose.runOnIdle { panel!!.value = PlayerPanel.Comments }
            compose.waitForIdle()
            activity.recreate()
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(PlayerPanel.Comments, panel!!.value) }
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
