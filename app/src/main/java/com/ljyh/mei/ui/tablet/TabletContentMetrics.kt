package com.ljyh.mei.ui.tablet

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

internal data class TabletContentMetrics(val twoPanes: Boolean, val sideWidthDp: Float)

/** A readable 280dp information column (+ gutters), a 480dp content column and 24dp gap. */
internal fun tabletContentMetrics(
    widthDp: Float,
    fontScale: Float,
    sideWidthFraction: Float = 0.34f,
    maxSideWidthDp: Float = 420f,
): TabletContentMetrics {
    val scale = fontScale.takeIf { it.isFinite() }?.coerceAtLeast(1f) ?: 1f
    val sideMinimum = 336f * scale
    val contentMinimum = 480f * scale
    val enabled = widthDp.isFinite() && widthDp >= sideMinimum + contentMinimum + 24f
    val availableWidth = widthDp.takeIf { it.isFinite() } ?: 0f
    val sideMaximum = minOf(maxSideWidthDp * scale,
        (availableWidth - contentMinimum - 24f).coerceAtLeast(sideMinimum))
        .coerceAtLeast(sideMinimum)
    val sideWidth = (availableWidth * sideWidthFraction).coerceIn(sideMinimum, sideMaximum)
    return TabletContentMetrics(enabled, sideWidth)
}

@Composable
internal fun AdaptiveTabletContent(
    sideWidthFraction: Float = 0.34f,
    maxSideWidthDp: Float = 420f,
    compact: @Composable () -> Unit,
    expanded: @Composable (TabletContentMetrics) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val metrics = tabletContentMetrics(
            maxWidth.value, LocalDensity.current.fontScale, sideWidthFraction, maxSideWidthDp,
        )
        val saved = rememberSaveableStateHolder()
        saved.SaveableStateProvider(if (metrics.twoPanes) "expanded" else "compact") {
            if (metrics.twoPanes) expanded(metrics) else compact()
        }
    }
}

internal fun tabletArtworkColumns(widthDp: Float, fontScale: Float, gutterDp: Float): Int {
    val scale = fontScale.takeIf { it.isFinite() }?.coerceAtLeast(1f) ?: 1f
    return ((widthDp - gutterDp * 2f + 16f) / (156f * scale + 16f)).toInt().coerceIn(1, 6)
}
