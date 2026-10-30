package com.ljyh.mei.ui.tablet.search

internal data class TabletSearchLayout(
    val twoPanes: Boolean,
    val primaryWidthDp: Float,
    val songColumns: Boolean,
    val artworkColumns: Int,
    val artistColumns: Int,
)

/** Uses the search viewport, including its gutters, rather than a physical-device flag. */
internal fun tabletSearchLayout(widthDp: Float, fontScale: Float, gutterDp: Float): TabletSearchLayout {
    val width = widthDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val scale = fontScale.takeIf { it.isFinite() && it > 0f }?.coerceAtLeast(1f) ?: 1f
    val gutter = gutterDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 28f
    val primaryMinimum = 480f * scale + 2f * gutter
    val secondaryMinimum = 280f * scale + 2f * gutter
    val body = (width - 2f * gutter).coerceAtLeast(0f)
    val twoPanes = width >= primaryMinimum + secondaryMinimum + 24f
    val paneWidth = if (twoPanes) (width - 24f).times(0.62f)
        .coerceIn(primaryMinimum, width - 24f - secondaryMinimum) else width
    return TabletSearchLayout(
        twoPanes = twoPanes,
        primaryWidthDp = paneWidth,
        songColumns = body >= 760f * scale,
        artworkColumns = ((body + 24f) / (156f * scale + 24f)).toInt().coerceIn(1, 6),
        artistColumns = ((body + 24f) / (176f * scale + 24f)).toInt().coerceIn(1, 5),
    )
}
