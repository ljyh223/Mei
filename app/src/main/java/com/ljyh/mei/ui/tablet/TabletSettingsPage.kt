package com.ljyh.mei.ui.tablet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.LocalMeiShowBackButton
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.setting.PlaySettingScreen
import com.ljyh.mei.ui.screen.setting.SettingsCategories

internal fun settingsCategoryForRoute(route: String): String = when (route) {
    Screen.UsbExclusiveSettings.route, Screen.Equalizer.route, Screen.ReverbSettings.route -> Screen.AudioOutputSettings.route
    Screen.GlassSettings.route -> Screen.AppearanceSettings.route
    Screen.LxSources.route -> Screen.SourceAndLyricsSettings.route
    Screen.DownloadManage.route -> Screen.DownloadSettings.route
    Screen.DeveloperSettings.route, Screen.Log.route -> Screen.About.route
    else -> route
}

/** Category changes replace the detail above the existing settings destination. */
internal fun NavController.selectSettingsCategory(route: String) {
    if (currentDestination?.route == route) return
    if (runCatching { getBackStackEntry(Screen.Setting.route) }.isFailure) return
    popBackStack(Screen.Setting.route, false)
    // The settings root displays playback as its default detail on wide layouts.
    if (route != Screen.PlaySettings.route) navigate(route) { launchSingleTop = true }
}

private val settingsRoutes = setOf(
    Screen.Setting.route, Screen.PlaySettings.route, Screen.AudioOutputSettings.route,
    Screen.LyricsSettings.route, Screen.AppearanceSettings.route, Screen.ContentSettings.route,
    Screen.SourceAndLyricsSettings.route, Screen.DownloadSettings.route, Screen.CacheSettings.route,
    Screen.BetaSettings.route, Screen.About.route, Screen.UsbExclusiveSettings.route,
    Screen.Equalizer.route, Screen.ReverbSettings.route, Screen.GlassSettings.route,
    Screen.LxSources.route, Screen.DownloadManage.route, Screen.DeveloperSettings.route, Screen.Log.route,
)

internal fun NavController.isSettingsDetailTransition(from: String?, to: String?): Boolean =
    from != null && to in settingsRoutes &&
        runCatching { getBackStackEntry(Screen.Setting.route) }.isSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TabletSettingsHome(scrollBehavior: TopAppBarScrollBehavior) {
    PlaySettingScreen(scrollBehavior)
}

/** Keep the settings navigation outside the NavHost so detail transitions cannot recreate it. */
@Composable
internal fun TabletSettingsHost(
    navController: NavHostController,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val hasSettingsParent = runCatching { navController.getBackStackEntry(Screen.Setting.route) }.isSuccess
    val showSettings = route in settingsRoutes && hasSettingsParent
    val category = if (route == Screen.Setting.route) Screen.PlaySettings.route else settingsCategoryForRoute(route.orEmpty())
    val showBack = route != Screen.Setting.route &&
        (route != category || navController.previousBackStackEntry?.destination?.route != Screen.Setting.route)
    TabletSettingsFrame(
        modifier = modifier,
        showSidebar = showSettings,
        sidebar = {
            MeiPinnedPage(title = "设置", contentTopPadding = 12.dp) { paddingValues ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(LocalMeiPageScroll.current!!)
                        .padding(paddingValues).padding(horizontal = meiPageGutter()),
                    verticalArrangement = Arrangement.Top,
                ) {
                    MeiLargeTitle("设置", modifier = Modifier.padding(bottom = 16.dp), startPadding = 0.dp)
                    SettingsCategories(category) { navController.selectSettingsCategory(it.route) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
    ) {
        CompositionLocalProvider(LocalMeiShowBackButton provides (!showSettings || showBack)) {
            content(Modifier.fillMaxSize())
        }
    }
}

@Composable
internal fun TabletSettingsFrame(
    modifier: Modifier = Modifier,
    showSidebar: Boolean,
    sidebar: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.then(
            if (showSidebar) Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            else Modifier,
        ),
    ) {
        if (showSidebar) {
            Box(Modifier.weight(0.42f).fillMaxHeight()) { sidebar() }
            VerticalDivider(
                modifier = Modifier.fillMaxHeight().width(0.5.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
        }
        Box(Modifier.weight(if (showSidebar) 0.58f else 1f).fillMaxHeight()) { content() }
    }
}
