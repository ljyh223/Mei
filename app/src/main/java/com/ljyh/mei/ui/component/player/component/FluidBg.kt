package com.ljyh.mei.ui.component.player.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.hypot

fun Modifier.animatedGradient(animating: Boolean = true): Modifier = composed {
    val rotation = remember { Animatable(0f) }

    // 处理动画启停
    LaunchedEffect(animating) {
        if (animating) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 30_000,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotation.snapTo(0f)
        }
    }

    // 使用解构声明优化颜色获取
    val (a1, a2, b1, b2) = with(MaterialTheme.colorScheme) {
        listOf(primaryContainer, secondaryContainer, tertiaryContainer, background)
    }

    drawWithCache {
        // 预计算公共参数
        val rectSize = hypot(size.width, size.height)
        val halfSize = rectSize / 2
        val centerOffset = Offset(
            x = -(rectSize - size.width) / 2,
            y = -(rectSize - size.height) / 2
        )

        // 缓存渐变色
        val brush1 = Brush.linearGradient(
            colors = listOf(
                a1,  // 主色
                a1.copy(alpha = 0.7f),  // 过渡色1
                a2.copy(alpha = 0.8f),  // 过渡色2
                a2  // 对比色
            ),
            start = centerOffset,
            end = centerOffset + Offset(rectSize * 0.7f, rectSize * 0.7f)
        )

        val brush2 = Brush.linearGradient(
            colors = listOf(
                b1,
                b1.copy(alpha = 0.6f),
                b2.copy(alpha = 0.7f),
                b2
            ),
            start = centerOffset + Offset(rectSize, 0f),
            end = centerOffset + Offset(0f, rectSize)
        )

        val maskBrush = Brush.verticalGradient(
            colors = listOf(Color.White, Color.Transparent),
            startY = 0f,
            endY = rectSize
        )

        onDrawBehind {
            // 合并旋转操作
            val currentRotation = rotation.value
            fun DrawScope.drawRotated(degrees: Float, block: DrawScope.() -> Unit) {
                withTransform({ rotate(degrees) }, block)
            }

            drawRotated(currentRotation) {
                drawRect(brush1, topLeft = centerOffset, size = Size(rectSize, rectSize))
            }

            drawRotated(-currentRotation) {
                drawRect(maskBrush, blendMode = BlendMode.DstOut,
                    topLeft = centerOffset, size = Size(rectSize, rectSize))
            }

            drawRotated(currentRotation) {
                drawRect(brush2, blendMode = BlendMode.Softlight,
                    topLeft = centerOffset, size = Size(rectSize, rectSize))
            }
        }
    }
}
