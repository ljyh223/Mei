package com.ljyh.mei.ui.component.player.component

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import com.ljyh.mei.constants.DynamicStreamerKey
import com.ljyh.mei.ui.component.BlurTransformation1
import com.ljyh.mei.ui.component.utils.calculateScaleToFit
import com.ljyh.mei.ui.component.utils.imageWithDynamicFilter
import com.ljyh.mei.utils.middleImage
import com.ljyh.mei.utils.rememberPreference
import com.ljyh.mei.utils.smallImage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import com.ljyh.mei.ui.component.BackgroundVisualState
import com.ljyh.mei.ui.component.FlowingLightBackground
import kotlin.math.min
import kotlin.math.sqrt

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