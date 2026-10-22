package com.ljyh.mei.ui.component.navigation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.MiniPlayerBarHeight

internal fun bottomNavigationContentHeight(fontScale: Float): Dp {
    val scale = if (fontScale.isFinite()) fontScale.coerceAtLeast(1f) else 1f
    return 64.dp + 18.dp * (scale - 1f)
}

/** Align the compact mini player's centre with the navigation/search circles. */
internal fun compactPlayerVerticalDrop(fontScale: Float): Dp =
    bottomNavigationContentHeight(fontScale) / 2 + MiniPlayerBarHeight / 2 + 10.dp
