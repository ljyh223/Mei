package com.ljyh.music.ui.component.player

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
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
                angle.value += 1f
                delay(128L)
            }
    }

    // 模糊背景图


    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val blurEffect by remember(blurRadius) {
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
                .placeholderMemoryCacheKey(cover.smallImage()) // 先用小图占位
                .data(cover.middleImage())
                .build(),
            modifier = Modifier
                .fillMaxSize()
                .scale(scale = calculateScaleToFit())
                .graphicsLayer {
                    rotationZ = angle.floatValue
                    renderEffect = blurEffect
                },

            colorFilter = cf,
            contentDescription = null
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(cover.middleImage())
                .transformations(BlurTransformation1(context, 15f, 5f))
                .build(),
            modifier = Modifier
                .fillMaxSize()
                .scale(scale = calculateScaleToFit())
                .graphicsLayer {
                    rotationZ = angle.floatValue
                },
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

