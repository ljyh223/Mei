package com.ljyh.mei.ui.theme

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.core.view.WindowCompat
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
class MeiSystemBarsTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun themeControlsBothBarsAndPlayerOnlyOverridesStatusBar() {
        var darkTheme by mutableStateOf(false)
        var playerLightStatusBar by mutableStateOf<Boolean?>(null)
        TabletContentTestActivity.content = {
            ProvideMeiSystemBars(darkTheme) {
                val setPlayerLightStatusBar = LocalPlayerLightStatusBar.current
                val playerAppearance = playerLightStatusBar
                SideEffect { setPlayerLightStatusBar(playerAppearance) }
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            val window = activity.get().window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            fun assertBars(lightStatusBar: Boolean, lightNavigationBar: Boolean) {
                compose.runOnIdle {
                    assertEquals(lightStatusBar, insetsController.isAppearanceLightStatusBars)
                    assertEquals(lightNavigationBar, insetsController.isAppearanceLightNavigationBars)
                }
            }

            assertBars(lightStatusBar = true, lightNavigationBar = true)
            compose.runOnIdle { darkTheme = true }
            assertBars(lightStatusBar = false, lightNavigationBar = false)
            compose.runOnIdle { playerLightStatusBar = true }
            assertBars(lightStatusBar = true, lightNavigationBar = false)
            compose.runOnIdle { playerLightStatusBar = null }
            assertBars(lightStatusBar = false, lightNavigationBar = false)
        } finally {
            activity.pause().stop().destroy()
            TabletContentTestActivity.content = {}
        }
    }
}
