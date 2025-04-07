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

import androidx.compose.animation.core.*
import androidx.compose.runtime.*

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun OptimizedBlurredImage(
    cover: String,
    isPlaying: Boolean,
    blurRadius: Dp = 50.dp
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current
    val cf = remember { imageWithDynamicFilter(isDarkTheme) }
    val dynamicStreamer by rememberPreference(DynamicStreamerKey, defaultValue = true)
    val rotation = remember { Animatable(0f) }

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
            rotation.stop() // 停止动画
        }
    }

    // 模糊背景图
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
                    rotationZ = rotation.value
                    renderEffect = blurEffect.value
                },
            colorFilter = cf,
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
    } else {
        // ... (旧的 AsyncImage 实现)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .placeholderMemoryCacheKey(cover.smallImage())
                .data(cover.middleImage())
                .transformations(BlurTransformation1(context, 15f, 5f))
                .build(),
            modifier = Modifier
                .fillMaxSize()
                .scale(scale = calculateScaleToFit())
                .graphicsLayer {
                    rotationZ = rotation.value
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
