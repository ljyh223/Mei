package com.ljyh.mei.ui.component.player.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Delays the indicator so normal, short transitions do not flash. */
@Composable
internal fun rememberBufferingSweep(isBuffering: Boolean): Float? {
    val sweep = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(isBuffering) {
        if (!isBuffering) {
            visible = false
            sweep.snapTo(0f)
            return@LaunchedEffect
        }
        delay(400)
        visible = true
        while (isActive) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(1300, easing = LinearEasing))
        }
    }

    return sweep.value.takeIf { isBuffering && visible }
}

/** A soft highlight travels only over the unplayed section of either progress style. */
internal fun DrawScope.drawBufferingSweep(
    playedFraction: Float,
    phase: Float,
    color: Color,
    centerY: Float,
    strokeWidth: Float,
) {
    val playedX = size.width * playedFraction.coerceIn(0f, 1f)
    val remaining = size.width - playedX
    if (remaining <= 0f) return

    val segmentWidth = minOf(64.dp.toPx(), size.width * 0.22f)
    val segmentStart = playedX - segmentWidth + (remaining + segmentWidth) * phase
    val visibleStart = maxOf(playedX, segmentStart)
    val visibleEnd = minOf(size.width, segmentStart + segmentWidth)
    if (visibleEnd <= visibleStart) return

    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, color.copy(alpha = 0.72f), Color.Transparent),
            startX = segmentStart,
            endX = segmentStart + segmentWidth,
        ),
        start = Offset(visibleStart, centerY),
        end = Offset(visibleEnd, centerY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}
