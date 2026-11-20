package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.LocalMeiPageTitlePlacement
import com.ljyh.mei.ui.component.glass.LocalMeiShowBackButton
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.component.glass.MeiPinnedLazyPage
import com.ljyh.mei.ui.component.glass.MeiPageTitlePlacement
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.screen.Screen

internal fun settingsCategoryForRoute(route: String): String = when (route) {
    Screen.UsbExclusiveSettings.route, Screen.Equalizer.route, Screen.ReverbSettings.route -> Screen.AudioOutputSettings.route
    Screen.GlassSettings.route, Screen.ArtworkSettings.route, Screen.PlayerAppearanceSettings.route,
    Screen.AnimationSettings.route, Screen.BackgroundAdvancedSettings.route -> Screen.AppearanceSettings.route
    Screen.LxSources.route, Screen.MusicFreePlugins.route -> Screen.SourceAndLyricsSettings.route
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
    Screen.ArtworkSettings.route, Screen.PlayerAppearanceSettings.route,
    Screen.AnimationSettings.route, Screen.BackgroundAdvancedSettings.route,
    Screen.LxSources.route, Screen.MusicFreePlugins.route, Screen.DownloadManage.route, Screen.DeveloperSettings.route, Screen.Log.route,
)

internal fun NavController.isSettingsDetailTransition(from: String?, to: String?): Boolean =
    from != null && to in settingsRoutes &&
        runCatching { getBackStackEntry(Screen.Setting.route) }.isSuccess

internal fun canShowPhoneSettingsSplit(contentWidth: Dp, fontScale: Float): Boolean =
    contentWidth >= 500.dp * fontScale.coerceAtLeast(1f)

internal fun phoneSettingsSidebarWidth(contentWidth: Dp, fontScale: Float): Dp {
    val scale = fontScale.coerceAtLeast(1f)
    val detailMinimum = 280.dp * scale
    return minOf(contentWidth * 0.44f, 300.dp * scale, contentWidth - detailMinimum).coerceAtLeast(0.dp)
}

/** Keep the settings navigation outside the NavHost so detail transitions cannot recreate it. */
@Composable
internal fun SplitSettingsHost(
    navController: NavHostController,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val hasSettingsParent = runCatching { navController.getBackStackEntry(Screen.Setting.route) }.isSuccess
    val showSettings = route in settingsRoutes && hasSettingsParent
    val compact = LocalPhoneLandscape.current
    val category = if (route == Screen.Setting.route) Screen.PlaySettings.route else settingsCategoryForRoute(route.orEmpty())
    val showBack = route != Screen.Setting.route &&
        (route != category || navController.previousBackStackEntry?.destination?.route != Screen.Setting.route)
    SplitSettingsFrame(
        modifier = modifier,
        showSidebar = showSettings,
        sidebar = {
            if (compact) {
                CompositionLocalProvider(LocalMeiPageTitlePlacement provides MeiPageTitlePlacement.Inline) {
                    MeiPinnedLazyPage(title = "设置", horizontalContentPadding = meiPageGutter()) {
                        item {
                            SettingsCategories(category, compact = true) { navController.selectSettingsCategory(it.route) }
                        }
                    }
                }
            } else {
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
            }
        },
    ) {
        val placement = when {
            !showSettings || !compact -> MeiPageTitlePlacement.Default
            showBack -> MeiPageTitlePlacement.Toolbar
            else -> MeiPageTitlePlacement.Inline
        }
        CompositionLocalProvider(
            LocalMeiShowBackButton provides (!showSettings || showBack),
            LocalMeiPageTitlePlacement provides placement,
        ) {
            content(Modifier.fillMaxSize())
        }
    }
}

@Composable
internal fun SplitSettingsFrame(
    modifier: Modifier = Modifier,
    showSidebar: Boolean,
    sidebar: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val sideInset = LocalPageStartInset.current
    val compact = LocalPhoneLandscape.current
    val frameInsets = if (showSidebar) {
        Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(start = sideInset)
    } else Modifier
    BoxWithConstraints(modifier.then(frameInsets)) {
        val separatorWidth = 0.5.dp
        val phoneSidebarWidth = phoneSettingsSidebarWidth(maxWidth - separatorWidth, LocalDensity.current.fontScale)
        Row(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalPageStartInset provides if (showSidebar) 0.dp else sideInset) {
                if (showSidebar) {
                    val sidebarWidth = if (compact) Modifier.width(phoneSidebarWidth) else Modifier.weight(0.42f)
                    Box(sidebarWidth.fillMaxHeight()) { sidebar() }
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight().width(separatorWidth).then(
                            if (compact) Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical))
                            else Modifier,
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }
                Box(Modifier.weight(if (showSidebar && !compact) 0.58f else 1f).fillMaxHeight()) { content() }
            }
        }
    }
}
