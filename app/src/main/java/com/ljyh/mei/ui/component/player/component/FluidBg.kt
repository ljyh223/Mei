package com.ljyh.mei.ui.component.player.component

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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



@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun FluidGradientBackground(
    // 传入从 MaterialKolor 或 Palette 获取的高饱和度颜色
    // 建议顺序：Vibrant, DarkVibrant, LightVibrant
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    // 如果颜色不够，用默认补齐，防止崩溃
    val safeColors = remember(colors) {
        if (colors.isEmpty()) listOf(Color.Blue, Color.Cyan, Color.Magenta)
        else if (colors.size < 3) colors + colors
        else colors
    }

    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()

    // 定义动画：这里使用三个独立的 InfiniteTransition 来控制三个球
    // 使用不同的时长和缓动，制造出“永远不重复”的无序流动感
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_anim")

    // 球1的运动轨迹 (X 和 Y 使用不同周期，形成不规则椭圆)
    val offset1X by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse), label = "x1"
    )
    val offset1Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Reverse), label = "y1"
    )

    // 球2的运动轨迹
    val offset2X by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse), label = "x2"
    )
    val offset2Y by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(21000, easing = LinearEasing), RepeatMode.Reverse), label = "y2"
    )

    // 球3的运动轨迹（呼吸感：大小变化）
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "s3"
    )

    // 核心魔法：RenderEffect
    // 只有 API 31+ 支持。这个模糊半径一定要大！100dp 起步。
    val blurEffect = remember {
        val radius = with(density) { 100.dp.toPx() }
        RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR)
            .asComposeRenderEffect()
    }

    val baseColor = if (isDark) Color.Black else MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .background(baseColor)
            .graphicsLayer { renderEffect = blurEffect }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val alphaBase = if (isDark) 0.6f else 0.8f
            // 绘制球体 1 (Primary Color)
            drawCircle(
                color = safeColors[0].copy(alpha = alphaBase), // 保持高透明度让颜色叠加
                radius = w * 0.6f,
                center = Offset(
                    x = w * 0.2f + (w * 0.6f) * offset1X,
                    y = h * 0.2f + (h * 0.6f) * offset1Y
                )
            )

            // 绘制球体 2 (Secondary Color)
            drawCircle(
                color = safeColors[1 % safeColors.size].copy(alpha = 0.7f),
                radius = w * 0.5f,
                center = Offset(
                    x = w * 0.1f + (w * 0.7f) * offset2X,
                    y = h * 0.1f + (h * 0.7f) * offset2Y
                )
            )

            // 绘制球体 3 (Tertiary Color - 负责提亮)
            // 这个球通常放在中间或角落，做呼吸效果
            drawCircle(
                color = safeColors[2 % safeColors.size].copy(alpha = alphaBase),
                radius = w * 0.4f * scale3,
                center = Offset(w * 0.5f, h * 0.5f)
            )
        }
    }

    // 3. 噪点滤镜 (可选，增加质感，减少色带断层)
    // 如果不想加图片，可以忽略，但加上会更有 Apple Music 的那种磨砂感

//    Box(modifier = Modifier.fillMaxSize().background(
//        brush = Brush.radialGradient(
//             // 这里可以用一张带噪点的透明 PNG 做 tile
//        )
//    ))

}