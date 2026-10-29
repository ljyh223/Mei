package com.ljyh.mei.ui.tablet

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
internal data class TabletBarLayout(
    val showNavigationLabels: Boolean,
    val navigationWidth: Dp,
    val controlHeight: Dp,
    val playerWidth: Dp,
)

/** Preserve all four destinations and a usable player slot in narrow windows and large text. */
internal fun tabletBarLayout(widthDp: Float, fontScale: Float): TabletBarLayout {
    val scale = fontScale.takeIf { it.isFinite() && it > 0f } ?: 1f
    val labels = widthDp >= 720f && scale <= 1.4f
    val navigationWidth = if (labels) 300.dp else 220.dp
    val availablePlayerWidth = (widthDp - navigationWidth.value - 64f - 24f).coerceAtLeast(0f)
    val preferredPlayerWidth = 340f + (scale - 1f).coerceIn(0f, 1f) * 64f
    return TabletBarLayout(
        showNavigationLabels = labels,
        navigationWidth = navigationWidth,
        controlHeight = maxOf(64.dp, (24f * scale + 24f).dp),
        playerWidth = minOf(availablePlayerWidth, preferredPlayerWidth).dp,
    )
}
