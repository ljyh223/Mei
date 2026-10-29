package com.ljyh.mei.ui.screen.playlist.layout

internal enum class PlaylistLayout { SinglePane, TwoPane }

internal data class PlaylistLayoutMetrics(val layout: PlaylistLayout, val detailWidthDp: Float = 0f)

/** Details contain their existing 28dp insets; reserve 280dp content, 24dp gap and a 480dp song pane. */
internal fun playlistLayoutMetrics(availableWidthDp: Float, fontScale: Float = 1f): PlaylistLayoutMetrics {
    val scale = fontScale.takeIf { it.isFinite() && it > 0f }?.coerceAtLeast(1f) ?: 1f
    val detailMinimum = 280f * scale + 56f
    val tracksMinimum = 480f * scale
    val body = availableWidthDp - 24f
    if (!body.isFinite() || body < detailMinimum + tracksMinimum) return PlaylistLayoutMetrics(PlaylistLayout.SinglePane)
    val details = (body * 0.4f).coerceIn(detailMinimum, 360f * scale + 56f).coerceAtMost(body - tracksMinimum)
    return PlaylistLayoutMetrics(PlaylistLayout.TwoPane, details)
}
