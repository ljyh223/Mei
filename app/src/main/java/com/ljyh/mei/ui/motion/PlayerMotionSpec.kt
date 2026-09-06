package com.ljyh.mei.ui.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween as composeTween

/** Maps one section of the player sheet's normalized progress to a local 0..1 value. */
data class MotionProgressWindow(
    val start: Float,
    val end: Float,
) {
    init {
        require(start < end) { "Motion progress window must have a positive range" }
    }

    fun transform(progress: Float): Float =
        ((progress - start) / (end - start)).coerceIn(0f, 1f)

    fun reverse(progress: Float): Float = 1f - transform(progress)
}

/**
 * Motion tokens shared by the player sheet and the app chrome surrounding it.
 *
 * Geometry continues to derive directly from [com.ljyh.mei.ui.component.sheet.BottomSheetState]
 * progress. These tokens only coordinate timing, easing, and staged visibility.
 */
object PlayerMotionSpec {
    const val ChromeDurationMillis = 240
    const val CollapseForNavigationDurationMillis = 280
    const val ContainerEnterDurationMillis = 180
    const val ContainerExitDurationMillis = 120
    const val InitialCoverEnterDurationMillis = 120
    const val CoverSwapDurationMillis = 400
    const val ClassicCoverSwapDurationMillis = 500
    const val CoverSwapInitialScale = 0.92f
    const val CoverShadowStartProgress = 0.8f

    val BackgroundReveal = MotionProgressWindow(start = 0.12f, end = 0.40f)
    val DefaultContainerReveal = MotionProgressWindow(start = 0.15f, end = 1f)
    val MiniPlayerExit = MotionProgressWindow(start = 0f, end = 0.18f)
    val MorphMiniPlayerExit = MotionProgressWindow(start = 0f, end = 0.25f)
    val ExpandedContentReveal = MotionProgressWindow(start = 0.25f, end = 0.50f)
    val ExpandedUiReveal = MotionProgressWindow(start = 0.42f, end = 0.70f)
    val LyricHeaderReveal = MotionProgressWindow(start = 0.40f, end = 1f)

    val SheetSettleSpring: AnimationSpec<Float> = spring(
        stiffness = Spring.StiffnessMediumLow,
    )
    val SheetDismissSpring: AnimationSpec<Float> = spring()
    val LyricModeSpring: SpringSpec<Float> = spring(
        stiffness = Spring.StiffnessLow,
    )
    val CoverPlayStateSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )

    fun <T> tween(
        durationMillis: Int = ChromeDurationMillis,
        easing: Easing = FastOutSlowInEasing,
    ): TweenSpec<T> = composeTween(
        durationMillis = durationMillis,
        easing = easing,
    )
}
