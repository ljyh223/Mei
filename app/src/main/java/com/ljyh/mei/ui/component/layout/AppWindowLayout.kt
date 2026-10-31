package com.ljyh.mei.ui.component.layout

import androidx.compose.runtime.staticCompositionLocalOf

/** Application chrome capability, not a physical device classification. */
internal enum class AppWindowLayout { Compact, Wide }

/** The wide dock needs 584dp including gutters; 600dp provides its usable minimum. */
internal fun appWindowLayout(availableWidthDp: Float): AppWindowLayout =
    if (availableWidthDp.isFinite() && availableWidthDp >= 600f) AppWindowLayout.Wide else AppWindowLayout.Compact

internal val LocalAppWindowLayout = staticCompositionLocalOf<AppWindowLayout?> { null }
