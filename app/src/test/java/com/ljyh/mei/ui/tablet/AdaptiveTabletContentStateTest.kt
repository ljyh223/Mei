package com.ljyh.mei.ui.tablet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1280dp-h800dp", manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class AdaptiveTabletContentStateTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun eachLayoutRetainsItsOwnSavedUiStateAcrossWindowChangesAndRecreation() {
        var width by mutableStateOf(1100.dp)
        var observed = ""
        var edit: (String) -> Unit = {}
        @Composable fun Pane(name: String) {
            var value by rememberSaveable { mutableStateOf(name) }
            val current = value
            BasicText(current)
            SideEffect { observed = current; edit = { value = it } }
        }
        TabletContentTestActivity.content = {
            Box(Modifier.requiredWidth(width).height(600.dp)) {
                AdaptiveTabletContent(compact = { Pane("compact") }, expanded = { Pane("expanded") })
            }
        }
        val activity = Robolectric.buildActivity(TabletContentTestActivity::class.java).setup()
        try {
            compose.waitForIdle(); assertEquals("expanded", observed)
            compose.runOnIdle { edit("expanded draft") }; compose.waitForIdle()
            compose.runOnIdle { width = 700.dp }; compose.waitForIdle(); assertEquals("compact", observed)
            compose.runOnIdle { edit("compact draft") }; compose.waitForIdle()
            compose.runOnIdle { width = 1100.dp }; compose.waitForIdle(); assertEquals("expanded draft", observed)
            activity.recreate(); compose.waitForIdle(); assertEquals("expanded draft", observed)
            compose.runOnIdle { width = 700.dp }; compose.waitForIdle(); assertEquals("compact draft", observed)
        } finally { activity.pause().stop().destroy(); TabletContentTestActivity.content = {} }
    }
}
