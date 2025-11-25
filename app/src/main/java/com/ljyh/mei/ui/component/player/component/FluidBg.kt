package com.ljyh.mei.ui.component.player.component

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun SmoothCoverBackground(
    coverUrl: String?,
    seedColor: Color,
    modifier: Modifier = Modifier
) {
    val isSystemDark = isSystemInDarkTheme()

    val colorScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isSystemDark,
        style = PaletteStyle.TonalSpot // 这种风格颜色更丰富
    )

    AnimatedContent(
        targetState = coverUrl,
        transitionSpec = {
            fadeIn(animationSpec = tween(600)) togetherWith
                    fadeOut(animationSpec = tween(600))
        },
        label = "FluidBgAnim"
    ) { targetUrl ->

        val currentFluidColors = remember(targetUrl, seedColor) {
            if (targetUrl != null) {
                listOf(
                    colorScheme.primary,
                    colorScheme.tertiary, // Tertiary 通常对比度高，很好看
                    colorScheme.secondary
                )
            } else {
                listOf(Color.Blue, Color.Cyan, Color.Magenta)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // 1. 流体背景层
            FluidGradientBackground(
                colors = currentFluidColors,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,       // 顶部保持原色/通透
                            0.5f to Color.Transparent,       // 中间依然通透
                            0.8f to Color.Black.copy(alpha = 0.3f), // 进度条附近开始变暗
                            1.0f to Color.Black.copy(alpha = 0.7f)  // 底部控制栏区域较暗
                        )
                    )
            )

            // 可选：如果是浅色模式，顶部状态栏可能也需要一点点遮罩来让白色文字可见
            if (!isSystemDark) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.2f),
                                0.2f to Color.Transparent
                            )
                        )
                )
            }
        }
    }
}
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun FluidGradientBackground(
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

    val baseColor = if (isDark) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.surface
        // 或者: safeColors[0].copy(alpha = 0.1f).compositeOver(Color.White)
    }


    Box(
        modifier = modifier
            .background(baseColor)
            .graphicsLayer { renderEffect = blurEffect }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val alphaBase = if (isDark) 0.6f else 0.7f
            drawCircle(
                color = safeColors[0].copy(alpha = alphaBase), // 保持高透明度让颜色叠加
                radius = w * 0.6f,
                center = Offset(
                    x = w * 0.2f + (w * 0.6f) * offset1X,
                    y = h * 0.2f + (h * 0.6f) * offset1Y
                )
            )

            drawCircle(
                color = safeColors[1 % safeColors.size].copy(alpha = 0.6f),
                radius = w * 0.5f,
                center = Offset(
                    x = w * 0.1f + (w * 0.7f) * offset2X,
                    y = h * 0.1f + (h * 0.7f) * offset2Y
                )
            )
            drawCircle(
                color  = if(isDark) Color.White.copy(alpha = 0.2f) else safeColors[2 % safeColors.size].copy(alpha = 0.5f),
                radius = w * 0.4f * scale3,
                center = Offset(w * 0.5f, h * 0.5f)
            )
        }
    }
}