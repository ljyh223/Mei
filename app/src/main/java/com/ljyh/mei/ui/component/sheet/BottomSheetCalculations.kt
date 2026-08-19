package com.ljyh.mei.ui.component.sheet

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class BottomSheetTarget { Dismissed, Collapsed, Expanded }

internal fun resolveFlingTarget(
    value: Dp,
    velocity: Float,
    dismissedBound: Dp,
    collapsedBound: Dp,
    expandedBound: Dp,
    canDismiss: Boolean,
    velocityThreshold: Float = 250f,
): BottomSheetTarget {
    if (velocity > velocityThreshold) return BottomSheetTarget.Expanded
    if (velocity < -velocityThreshold) {
        return if (value < collapsedBound && canDismiss) {
            BottomSheetTarget.Dismissed
        } else {
            BottomSheetTarget.Collapsed
        }
    }

    val dismissedCollapsedMidpoint = dismissedBound + (collapsedBound - dismissedBound) / 2f
    val collapsedExpandedMidpoint = collapsedBound + (expandedBound - collapsedBound) / 2f
    return when {
        value < dismissedCollapsedMidpoint && canDismiss -> BottomSheetTarget.Dismissed
        value < collapsedExpandedMidpoint -> BottomSheetTarget.Collapsed
        else -> BottomSheetTarget.Expanded
    }
}

internal data class BottomSheetMorphLayout(
    val width: Dp,
    val height: Dp,
    val cornerRadius: Dp,
    val effectiveBottomMargin: Dp,
    val backgroundAlpha: Float,
)

internal fun resolveMorphLayout(
    maxWidth: Dp,
    expandedHeight: Dp,
    spec: BottomSheetMorphSpec,
    progress: Float,
    revealProgress: Float,
): BottomSheetMorphLayout {
    val fraction = progress.coerceIn(0f, 1f)
    val collapsedAvailableWidth =
        (maxWidth - spec.collapsedHorizontalMargin * 2).coerceAtLeast(0.dp)
    val collapsedWidth = spec.collapsedMaxWidth
        ?.let(collapsedAvailableWidth::coerceAtMost)
        ?: collapsedAvailableWidth
    val expandedWidth =
        (maxWidth - spec.expandedHorizontalMargin * 2).coerceAtLeast(0.dp)
    val bottomMargin = lerp(
        spec.collapsedBottomMargin,
        spec.expandedBottomMargin,
        fraction,
    )
    return BottomSheetMorphLayout(
        width = lerp(collapsedWidth, expandedWidth, fraction),
        height = lerp(spec.collapsedHeight, expandedHeight, fraction),
        cornerRadius = lerp(spec.collapsedCornerRadius, spec.expandedCornerRadius, fraction),
        effectiveBottomMargin = bottomMargin * revealProgress.coerceIn(0f, 1f),
        backgroundAlpha = 1f - (fraction * 4f).coerceAtMost(1f),
    )
}

internal fun lerp(start: Dp, stop: Dp, fraction: Float): Dp =
    Dp((1f - fraction) * start.value + fraction * stop.value)
