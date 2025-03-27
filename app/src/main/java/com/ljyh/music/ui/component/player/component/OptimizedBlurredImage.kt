package com.ljyh.music.ui.component.player.component

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
import com.ljyh.music.constants.DynamicStreamerKey
import com.ljyh.music.ui.component.BlurTransformation1
import com.ljyh.music.ui.component.utils.calculateScaleToFit
import com.ljyh.music.ui.component.utils.imageWithDynamicFilter
import com.ljyh.music.utils.middleImage
import com.ljyh.music.utils.rememberPreference
import com.ljyh.music.utils.smallImage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun OptimizedBlurredImage(
    cover: String,
    isPlaying: Boolean,
    blurRadius: Dp = 50.dp // 动态控制模糊半径
) {
    val context = LocalContext.current
    val angle = remember { mutableFloatStateOf(0f) }
    val isDarkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current
    val cf = remember { imageWithDynamicFilter(isDarkTheme) }
    val dynamicStreamer by rememberPreference(DynamicStreamerKey, defaultValue = true)
    // 动画控制，降低刷新频率
    LaunchedEffect(isPlaying, dynamicStreamer) {
        if (dynamicStreamer)
            while (isPlaying) {
                angle.value += 0.5f
                delay(64L)
            }
    }

    val animatedAngle by animateFloatAsState(
        targetValue = if (isPlaying) angle.floatValue + 360f else angle.floatValue, // 一次完整旋转
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // 模糊背景图
    // 如果当前手机sdk大于Android 12 才使用RenderEffect
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
        val blurEffect = remember(blurRadius) {
            val blurIntensity = with(density) { blurRadius.toPx().coerceIn(1f, 100f) }

            mutableStateOf(
                RenderEffect.createBlurEffect(
                    blurIntensity.roundToInt().toFloat(),
                    blurIntensity.roundToInt().toFloat(),
                    Shader.TileMode.CLAMP
                ).asComposeRenderEffect()
            )
        }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(cover.smallImage())
                .build(),
            modifier = Modifier
                .fillMaxSize()
                .scale(scale = calculateScaleToFit())
                .graphicsLayer {
                    rotationZ = animatedAngle
                    renderEffect = blurEffect.value
                },

            colorFilter = cf,
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .placeholderMemoryCacheKey(cover.smallImage()) // 先用小图占位
                .data(cover.middleImage())
                .transformations(BlurTransformation1(context, 15f, 5f))
                .build(),
            modifier = Modifier
                .fillMaxSize()
                .scale(scale = calculateScaleToFit())
                .graphicsLayer {
                    rotationZ = angle.floatValue
                },
            contentScale = ContentScale.Crop,
            colorFilter = cf,
            contentDescription = null
        )

    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.6f))
    )
}

