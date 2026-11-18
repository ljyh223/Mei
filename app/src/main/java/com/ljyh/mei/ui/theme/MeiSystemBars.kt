package com.ljyh.mei.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

/** The player can request dark or light status bar icons for its artwork, or clear the request. */
internal val LocalPlayerLightStatusBar = compositionLocalOf<(Boolean?) -> Unit> { {} }

@Composable
internal fun ProvideMeiSystemBars(darkTheme: Boolean, content: @Composable () -> Unit) {
    val window = (LocalContext.current as? Activity)?.window
    val configuration = LocalConfiguration.current
    var playerLightStatusBar by remember { mutableStateOf<Boolean?>(null) }
    val setPlayerLightStatusBar: (Boolean?) -> Unit = remember { { playerLightStatusBar = it } }
    val lightNavigationBar = !darkTheme
    val lightStatusBar = playerLightStatusBar ?: !darkTheme

    // Edge-to-edge setup can reset the icon appearance on configuration changes.
    DisposableEffect(window, configuration, lightStatusBar, lightNavigationBar) {
        if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = lightStatusBar
                isAppearanceLightNavigationBars = lightNavigationBar
            }
        }
        onDispose {}
    }
    CompositionLocalProvider(LocalPlayerLightStatusBar provides setPlayerLightStatusBar, content = content)
}
