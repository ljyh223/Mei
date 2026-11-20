package com.ljyh.mei.ui.tablet

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import com.ljyh.mei.ui.app.MeiPageContent
import com.ljyh.mei.ui.app.SharedMeiPageContent
import com.ljyh.mei.ui.app.SplitSettingsPageContent
import com.ljyh.mei.ui.component.navigation.selectMainDestination
import com.ljyh.mei.ui.screen.Index
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.navigationBuilder
import com.ljyh.mei.ui.screen.setting.canShowPhoneSettingsSplit
import com.ljyh.mei.ui.screen.setting.phoneSettingsSidebarWidth
import com.ljyh.mei.ui.screen.setting.selectSettingsCategory
import com.ljyh.mei.ui.screen.setting.settingsCategoryForRoute
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class TabletSettingsNavigationTest {
    private val scrollBehavior = object : TopAppBarScrollBehavior {
        override val state: TopAppBarState get() = error("Pages are not composed")
        override val isPinned = true
        override val snapAnimationSpec: AnimationSpec<Float>? = null
        override val flingAnimationSpec: DecayAnimationSpec<Float>? = null
        override val nestedScrollConnection = object : NestedScrollConnection {}
    }

    private fun NavHostController.installPages(pages: MeiPageContent) {
        graph = createGraph(startDestination = Screen.Home.route) {
            navigationBuilder(this@installPages, scrollBehavior, pages)
        }
    }

    private fun controller() = NavHostController(RuntimeEnvironment.getApplication()).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        setViewModelStore(ViewModelStore())
        installPages(TabletPageContent)
        navigate(Screen.Setting.route)
    }

    @Test
    fun enteringTabletSettingsAndSwitchingDetailsAreImmediate() {
        val nav = controller()
        assertTrue(TabletPageContent.useImmediateTransition(nav, Screen.Setting.route, Screen.AppearanceSettings.route))
        assertTrue(TabletPageContent.useImmediateTransition(nav, Screen.AppearanceSettings.route, Screen.GlassSettings.route))
        assertFalse(SharedMeiPageContent.useImmediateTransition(nav, Screen.Setting.route, Screen.AppearanceSettings.route))
        assertFalse(TabletPageContent.useImmediateTransition(nav, Screen.Setting.route, Screen.Home.route))
        assertTrue(TabletPageContent.useImmediateTransition(nav, Screen.Home.route, Screen.Setting.route))
        assertTrue(TabletPageContent.useImmediateTransition(nav, Screen.Library.route, Screen.AppearanceSettings.route))
        assertFalse(TabletPageContent.useImmediateTransition(nav, Screen.Home.route, Screen.Library.route))
        assertFalse(TabletPageContent.useImmediateTransition(nav, null, Screen.Setting.route))
        assertTrue(nav.popBackStack())
        assertFalse(TabletPageContent.useImmediateTransition(nav, Screen.About.route, Screen.Log.route))
    }

    @Test
    fun returningToSavedSettingsDetailFromAnotherTabIsImmediate() {
        val nav = controller()
        nav.selectSettingsCategory(Screen.AppearanceSettings.route)
        nav.selectMainDestination(Index.Library)
        assertEquals(Screen.Library.route, nav.currentDestination!!.route)
        nav.selectMainDestination(Index.Setting)
        assertEquals(Screen.AppearanceSettings.route, nav.currentDestination!!.route)
        assertTrue(TabletPageContent.useImmediateTransition(nav, Screen.Library.route, nav.currentDestination!!.route))
    }

    @Test
    fun switchingCategoriesRetainsRootAndDiscardsPreviousDetailStack() {
        val nav = controller()
        val root = nav.currentBackStackEntry!!
        nav.selectSettingsCategory(Screen.AudioOutputSettings.route)
        val audio = nav.currentBackStackEntry!!
        nav.navigate(Screen.Equalizer.route)
        assertEquals(Screen.AudioOutputSettings.route, settingsCategoryForRoute(nav.currentDestination!!.route!!))
        assertTrue(nav.popBackStack())
        assertSame(audio, nav.currentBackStackEntry)
        nav.navigate(Screen.UsbExclusiveSettings.route)
        nav.selectSettingsCategory(Screen.AppearanceSettings.route)
        assertSame(root, nav.getBackStackEntry(Screen.Setting.route))
        assertEquals(Screen.AppearanceSettings.route, nav.currentDestination!!.route)
        assertTrue(nav.popBackStack())
        assertSame(root, nav.currentBackStackEntry)
    }

    @Test
    fun playbackDefaultAndWindowChangeKeepMainNavigationEntry() {
        val nav = controller()
        val root = nav.currentBackStackEntry!!
        nav.selectSettingsCategory(Screen.ContentSettings.route)
        val detail = nav.currentBackStackEntry!!
        detail.savedStateHandle["marker"] = 42
        nav.installPages(SharedMeiPageContent)
        assertSame(detail, nav.currentBackStackEntry)
        nav.installPages(SplitSettingsPageContent)
        assertSame(detail, nav.currentBackStackEntry)
        assertTrue(SplitSettingsPageContent.useImmediateTransition(nav, Screen.Setting.route, Screen.ContentSettings.route))
        nav.installPages(TabletPageContent)
        assertSame(detail, nav.currentBackStackEntry)
        assertEquals(42, nav.currentBackStackEntry!!.savedStateHandle.get<Int>("marker"))
        nav.selectSettingsCategory(Screen.PlaySettings.route)
        assertSame(root, nav.currentBackStackEntry)
        assertEquals(Screen.Setting.route, nav.currentDestination!!.route)
    }

    @Test
    fun appearanceChildrenStayInTheSameCategoryWithoutDetailAnimations() {
        val nav = controller()
        nav.selectSettingsCategory(Screen.AppearanceSettings.route)
        val appearance = nav.currentBackStackEntry!!
        for (page in listOf(Screen.ArtworkSettings, Screen.PlayerAppearanceSettings,
            Screen.AnimationSettings, Screen.BackgroundAdvancedSettings)) {
            assertEquals(Screen.AppearanceSettings.route, settingsCategoryForRoute(page.route))
            assertTrue(TabletPageContent.useImmediateTransition(nav, Screen.AppearanceSettings.route, page.route))
            assertFalse(SharedMeiPageContent.useImmediateTransition(nav, Screen.AppearanceSettings.route, page.route))
            nav.navigate(page.route)
            assertEquals(page.route, nav.currentDestination!!.route)
            assertTrue(nav.popBackStack())
            assertSame(appearance, nav.currentBackStackEntry)
        }
    }

    @Test
    fun deepAppearanceSettingsRestoreAcrossTabsAndReturnThroughTheirParents() {
        val nav = controller()
        val root = nav.currentBackStackEntry!!
        nav.selectSettingsCategory(Screen.AppearanceSettings.route)
        val appearance = nav.currentBackStackEntry!!
        nav.navigate(Screen.PlayerAppearanceSettings.route)
        val playerAppearance = nav.currentBackStackEntry!!
        nav.navigate(Screen.BackgroundAdvancedSettings.route)
        val advanced = nav.currentBackStackEntry!!
        advanced.savedStateHandle["marker"] = 42
        nav.selectMainDestination(Index.Library)
        nav.selectMainDestination(Index.Setting)
        assertEquals(advanced.id, nav.currentBackStackEntry!!.id)
        assertEquals(42, nav.currentBackStackEntry!!.savedStateHandle.get<Int>("marker"))
        assertEquals(Screen.AppearanceSettings.route, settingsCategoryForRoute(nav.currentDestination!!.route!!))
        assertTrue(nav.popBackStack())
        assertEquals(playerAppearance.id, nav.currentBackStackEntry!!.id)
        assertTrue(nav.popBackStack())
        assertEquals(appearance.id, nav.currentBackStackEntry!!.id)
        nav.navigate(Screen.ArtworkSettings.route)
        nav.selectSettingsCategory(Screen.CacheSettings.route)
        assertEquals(root.id, nav.getBackStackEntry(Screen.Setting.route).id)
        assertEquals(Screen.CacheSettings.route, nav.currentDestination!!.route)
        assertTrue(nav.popBackStack())
        assertEquals(root.id, nav.currentBackStackEntry!!.id)
    }

    @Test
    fun phoneSettingsSplitsOnlyWhenBothPanesFitAtTheCurrentFontScale() {
        assertFalse(canShowPhoneSettingsSplit(499.dp, 1f))
        assertTrue(canShowPhoneSettingsSplit(500.dp, 1f))
        assertFalse(canShowPhoneSettingsSplit(600.dp, 1.3f))
        assertTrue(canShowPhoneSettingsSplit(660.dp, 1.3f))
        assertEquals(220f, phoneSettingsSidebarWidth(500.dp, 1f).value, 0.01f)
        assertEquals(264f, phoneSettingsSidebarWidth(600.dp, 1f).value, 0.01f)
        assertEquals(300.dp, phoneSettingsSidebarWidth(850.dp, 1f))
    }
}
