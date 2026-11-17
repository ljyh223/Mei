package com.ljyh.mei.ui.tablet

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding

internal data class TabletContentMetrics(val twoPanes: Boolean, val sideWidthDp: Float)

/** A readable 280dp information column (+ gutters), a 480dp content column and 24dp gap. */
internal fun tabletContentMetrics(
    widthDp: Float,
    fontScale: Float,
    sideWidthFraction: Float = 0.34f,
    maxSideWidthDp: Float = 420f,
    minSideWidthDp: Float = 336f,
    minContentWidthDp: Float = 480f,
): TabletContentMetrics {
    val scale = fontScale.takeIf { it.isFinite() }?.coerceAtLeast(1f) ?: 1f
    val sideMinimum = minSideWidthDp * scale
    val contentMinimum = minContentWidthDp * scale
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
    minSideWidthDp: Float = 336f,
    minContentWidthDp: Float = 480f,
    compact: @Composable () -> Unit,
    expanded: @Composable (TabletContentMetrics) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val insets = meiPageHorizontalPadding(gutter = 0.dp)
        val direction = LocalLayoutDirection.current
        val width = maxWidth - insets.calculateStartPadding(direction) - insets.calculateEndPadding(direction)
        val metrics = tabletContentMetrics(
            width.value, LocalDensity.current.fontScale, sideWidthFraction, maxSideWidthDp,
            minSideWidthDp, minContentWidthDp,
        )
        val saved = rememberSaveableStateHolder()
        saved.SaveableStateProvider(if (metrics.twoPanes) "expanded" else "compact") {
            if (metrics.twoPanes) expanded(metrics) else compact()
        }
    }
}

internal fun tabletArtworkColumns(
    widthDp: Float, fontScale: Float, gutterDp: Float, artworkWidthRatio: Float = 1f,
    minArtworkWidthDp: Float = 156f,
): Int {
    val scale = fontScale.takeIf { it.isFinite() }?.coerceAtLeast(1f) ?: 1f
    return ((widthDp - gutterDp * 2f + 16f) / (minArtworkWidthDp * scale * artworkWidthRatio + 16f)).toInt().coerceIn(1, 6)
}
