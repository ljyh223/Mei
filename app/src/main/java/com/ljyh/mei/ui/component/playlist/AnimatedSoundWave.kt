package com.ljyh.mei.ui.component.playlist

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun AnimatedSoundWave(
    modifier: Modifier = Modifier,
    barCount: Int = 3,
    barColor: Color = Color.White,
    barWidth: Dp = 6.dp,
    barSpacing: Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sound_wave_transition")

    val barAnimations = List(barCount) { index ->
        val duration = 400 + (index * 100)
        val delay = index * 150

        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, delayMillis = delay),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_animation_$index"
        )
    }

    Canvas(modifier = modifier) {
        val canvasHeight = size.height
        val barWidthPx = barWidth.toPx()
        val barSpacingPx = barSpacing.toPx()
        val totalBarsWidth = barCount * barWidthPx
        val totalSpacingWidth = (barCount - 1).coerceAtLeast(0) * barSpacingPx
        val totalGroupWidth = totalBarsWidth + totalSpacingWidth

        val startXOffset = (size.width - totalGroupWidth) / 2

        for (i in 0 until barCount) {
            val barHeight = canvasHeight * barAnimations[i].value
            val startX = startXOffset + i * (barWidthPx + barSpacingPx)

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x = startX, y = (canvasHeight - barHeight) / 2),
                size = Size(width = barWidthPx, height = barHeight),
                cornerRadius = CornerRadius(x = barWidthPx / 2, y = barWidthPx / 2)
            )
        }
    }
}