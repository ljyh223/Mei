package com.ljyh.mei.ui.component.utils


import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.min

@Stable
data class DeviceInfo(
    val isTablet: Boolean,
    val isLandscape: Boolean,
    val isPortrait: Boolean,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val smallestWidthDp: Int
)

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun rememberDeviceInfo(): DeviceInfo {
    val configuration = LocalConfiguration.current

    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val smallestWidth = min(screenWidth, screenHeight)

    return DeviceInfo(
        isTablet = smallestWidth >= 600,
        isLandscape = screenWidth > screenHeight,
        isPortrait = screenHeight >= screenWidth,
        screenWidthDp = screenWidth,
        screenHeightDp = screenHeight,
        smallestWidthDp = smallestWidth
    )
}
