package com.ljyh.mei.ui.tablet.player

internal data class TabletPlayerLayout(
    val twoPanes: Boolean,
    val nowPlayingWidthDp: Float,
    val artworkSizeDp: Float,
    val paneGapDp: Float,
    val showCompactControls: Boolean,
)

/** Measures the available body after safe-area/header insets, independent of physical orientation. */
internal fun tabletPlayerLayout(widthDp: Float, heightDp: Float, fontScale: Float): TabletPlayerLayout {
    val width = widthDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val height = heightDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val scale = fontScale.takeIf { it.isFinite() && it > 0f }?.coerceAtLeast(1f) ?: 1f
    val gap = 40f
    val twoPanes = width >= 800f * scale + gap
    val nowPlayingWidth = if (twoPanes) (width - gap) / 2f else minOf(width, 460f * scale)
    // Artwork, metadata and controls share one centered width. Short windows scroll as a group.
    val minimumContentWidth = minOf(280f * scale, nowPlayingWidth)
    val artwork = minOf(nowPlayingWidth, 460f * scale,
        (height - 330f * scale).coerceAtLeast(minimumContentWidth))
    return TabletPlayerLayout(twoPanes, nowPlayingWidth, artwork, gap,
        showCompactControls = height >= 460f * scale)
}
