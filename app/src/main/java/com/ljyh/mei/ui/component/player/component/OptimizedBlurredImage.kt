package com.ljyh.mei.ui.component.player.component

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ljyh.mei.constants.DynamicStreamerKey
import com.ljyh.mei.ui.component.BackgroundVisualState
import com.ljyh.mei.ui.component.FlowingLightBackground
import com.ljyh.mei.utils.rememberPreference

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun OptimizedBlurredImage(
    cover: String,
    isPlaying: Boolean
) {
    val isDarkTheme = isSystemInDarkTheme()
    val dynamicStreamer by rememberPreference(DynamicStreamerKey, defaultValue = true)
    val rotation = remember { Animatable(0f) }

    val colorScheme = MaterialTheme.colorScheme
    val fluidColors = remember(colorScheme) {
        listOf(
            colorScheme.primary,      // 核心色
            colorScheme.tertiary,     // 通常是对比色，很鲜艳
            colorScheme.secondary,    // 辅助色
            // colorScheme.error      // 有时候 error 色也是个很好的点缀（通常是红色/橘色）
        )
    }


    LaunchedEffect(isPlaying, dynamicStreamer) {
        if (dynamicStreamer && isPlaying) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 30_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotation.stop()
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            FluidGradientBackground(
                colors = fluidColors,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            FlowingLightBackground(
                state = BackgroundVisualState(
                    cover, true
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
        val scrimBrush = remember(isDarkTheme) {
            if (!isDarkTheme) {
                Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.05f), // 顶部极淡，几乎没有
                    0.4f to Color.Transparent,               // 中间完全透出漂亮的颜色
                    0.7f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.3f)   // 底部稍微压一点点，防止白色按钮看不见
                )
            } else {
                // 【深色模式策略】：沉浸为主
                Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.3f),  // 顶部适中
                    0.4f to Color.Transparent,
                    0.6f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.7f)   // 底部较深
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimBrush)
        )

    }
}