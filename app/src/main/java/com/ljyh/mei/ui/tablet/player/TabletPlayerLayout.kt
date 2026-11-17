package com.ljyh.mei.ui.tablet.player

internal data class TabletPlayerLayout(
    val twoPanes: Boolean,
    val nowPlayingWidthDp: Float,
    val artworkSizeDp: Float,
    val paneGapDp: Float,
    val showCompactControls: Boolean,
    val fitShortWindow: Boolean,
)

/** Measures the available body after safe-area/header insets, independent of physical orientation. */
internal fun tabletPlayerLayout(
    widthDp: Float,
    heightDp: Float,
    fontScale: Float,
    shortWindowLayout: Boolean = false,
): TabletPlayerLayout {
    val width = widthDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val height = heightDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val scale = fontScale.takeIf { it.isFinite() && it > 0f }?.coerceAtLeast(1f) ?: 1f
    val gap = if (shortWindowLayout) 24f else 40f
    val minimumPaneWidth = if (shortWindowLayout) 280f else 400f
    val twoPanes = width >= minimumPaneWidth * 2 * scale + gap
    val nowPlayingWidth = if (twoPanes) (width - gap) / 2f else minOf(width, 460f * scale)
    // Artwork, metadata and controls share one centered width. Short windows scroll as a group.
    val minimumContentWidth = minOf(280f * scale, nowPlayingWidth)
    val fitShortWindow = shortWindowLayout || (twoPanes && height < 560f * scale)
    val artwork = when {
        shortWindowLayout && twoPanes -> minOf(nowPlayingWidth, height)
        fitShortWindow -> minOf(nowPlayingWidth, (height - 240f * scale).coerceIn(48f, 240f))
        else -> minOf(nowPlayingWidth, 460f * scale, (height - 330f * scale).coerceAtLeast(minimumContentWidth))
    }
    return TabletPlayerLayout(twoPanes, nowPlayingWidth, artwork, gap,
        showCompactControls = height >= 460f * scale, fitShortWindow = fitShortWindow)
}
