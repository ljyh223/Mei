package com.ljyh.mei.ui.screen.playlist.layout

internal enum class PlaylistLayout { SinglePane, TwoPane }

internal data class PlaylistLayoutMetrics(val layout: PlaylistLayout, val detailWidthDp: Float = 0f)

/** Choose using the space available to the panes, after system insets and phone navigation. */
internal fun playlistLayoutMetrics(
    availableWidthDp: Float,
    fontScale: Float = 1f,
    compact: Boolean = false,
): PlaylistLayoutMetrics {
    val scale = fontScale.takeIf { it.isFinite() && it > 0f }?.coerceAtLeast(1f) ?: 1f
    val detailMinimum = if (compact) 224f * scale + 40f else 280f * scale + 56f
    val tracksMinimum = if (compact) 296f * scale else 480f * scale
    val body = availableWidthDp - 24f
    if (!body.isFinite() || body < detailMinimum + tracksMinimum) return PlaylistLayoutMetrics(PlaylistLayout.SinglePane)
    val details = (body * 0.4f).coerceIn(detailMinimum, 360f * scale + 56f).coerceAtMost(body - tracksMinimum)
    return PlaylistLayoutMetrics(PlaylistLayout.TwoPane, details)
}
