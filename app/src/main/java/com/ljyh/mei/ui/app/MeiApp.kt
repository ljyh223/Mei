package com.ljyh.mei.ui.app

import android.app.Activity
import android.app.ActivityManager
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import com.ljyh.mei.constants.DefaultThemeColor
import com.ljyh.mei.constants.DynamicThemeKey
import com.ljyh.mei.constants.LiquidGlassEnabledKey
import com.ljyh.mei.constants.PageTransitionSpeed
import com.ljyh.mei.constants.PageTransitionSpeedKey
import com.ljyh.mei.constants.PageTransitionStyle
import com.ljyh.mei.constants.PageTransitionStyleKey
import com.ljyh.mei.constants.ThemeColorKey
import com.ljyh.mei.data.model.domain.UserData
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.di.repository.ColorRepository
import com.ljyh.mei.ui.component.glass.LocalMeiGlassAppearance
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.LocalAppWindowLayout
import com.ljyh.mei.ui.component.layout.appWindowLayout
import com.ljyh.mei.ui.component.player.state.rememberPlayerConnection
import com.ljyh.mei.ui.component.sheet.expandedAnchor
import com.ljyh.mei.ui.component.utils.rememberBatteryIntensiveFeaturesAllowed
import com.ljyh.mei.ui.local.LocalDatabase
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.local.LocalUserData
import com.ljyh.mei.ui.motion.PageNavigationMotion
import com.ljyh.mei.ui.screen.main.library.LocalLibrarySearchState
import com.ljyh.mei.ui.tablet.TabletApp
import com.ljyh.mei.ui.theme.MusicTheme
import com.ljyh.mei.ui.theme.rememberPlayerThemeColor
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberGlassAppearance
import com.ljyh.mei.utils.preferences.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeiApp(
    database: AppDatabase,
    colorRepository: ColorRepository,
    userData: UserData,
    startInLibrary: Boolean,
) {
    val context = LocalContext.current
    val glassEnabled by rememberPreference(LiquidGlassEnabledKey, true)
    val glassAppearance by rememberGlassAppearance()
    val pageTransitionStyle by rememberEnumPreference(PageTransitionStyleKey, PageTransitionStyle.Slide)
    val pageTransitionSpeed by rememberEnumPreference(PageTransitionSpeedKey, PageTransitionSpeed.Standard)
    val pageMotion = remember(pageTransitionStyle, pageTransitionSpeed) {
        PageNavigationMotion(pageTransitionStyle, pageTransitionSpeed)
    }
    val dynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
    val themeColor by rememberPreference(ThemeColorKey, defaultValue = DefaultThemeColor)
    val staticThemeColor = remember(themeColor) {
        runCatching { Color(android.graphics.Color.parseColor(themeColor)) }
            .getOrDefault(Color(0xFF0061A4))
    }
    val playerConnection = rememberPlayerConnection(context, database)

    val targetThemeColor = rememberPlayerThemeColor(
        context = context,
        playerConnection = playerConnection,
        dynamicTheme = dynamicTheme,
        colorRepository = colorRepository,
    )
    MusicTheme(
        seedColor = if (dynamicTheme) targetThemeColor else staticThemeColor,
        isDark = isSystemInDarkTheme(),
    ) {
        val appState = rememberMeiAppState(playerConnection, startInLibrary)
        val batteryAllowsGlass = rememberBatteryIntensiveFeaturesAllowed()
        val lowRam = remember(context) {
            context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
        }
        val useGlass = glassEnabled && batteryAllowsGlass && !lowRam && Build.VERSION.SDK_INT >= 33
        CompositionLocalProvider(
            LocalDatabase provides database,
            LocalNavController provides appState.navController,
            LocalPlayerConnection provides playerConnection,
            LocalUserData provides userData,
            LocalLibrarySearchState provides appState.librarySearchState,
            LocalMeiGlassEnabled provides useGlass,
            LocalMeiGlassAppearance provides glassAppearance,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val direction = LocalLayoutDirection.current
                val safe = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
                val availableWidth = maxWidth - safe.calculateLeftPadding(direction) - safe.calculateRightPadding(direction)
                val layout = appWindowLayout(availableWidth.value)
                val activity = context as? Activity
                val freeOrientation = layout == AppWindowLayout.Wide ||
                    appState.playerSheetAnchor.intValue == expandedAnchor || activity?.isInMultiWindowMode == true
                val orientation = if (freeOrientation) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                SideEffect {
                    if (activity != null && activity.requestedOrientation != orientation) activity.requestedOrientation = orientation
                }
                CompositionLocalProvider(LocalAppWindowLayout provides layout) {
                    when (layout) {
                        AppWindowLayout.Wide -> TabletApp(appState, pageMotion)
                        AppWindowLayout.Compact -> PhoneApp(appState, pageMotion)
                    }
                }
            }
        }
    }
}
