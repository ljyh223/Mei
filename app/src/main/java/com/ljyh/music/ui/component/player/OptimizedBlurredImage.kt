package com.ljyh.music.ui.component.player

import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import com.ljyh.music.ui.component.BlurTransformation
import com.ljyh.music.ui.component.BlurTransformation1
import com.ljyh.music.ui.component.utils.calculateScaleToFit
import com.ljyh.music.ui.component.utils.imageWithDynamicFilter
import com.ljyh.music.utils.size1600
import com.ljyh.music.utils.smallImage
import com.ljyh.music.utils.toPx
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
    // 使用记忆化缓存模糊效果，避免重复计算
//    val blurEffect by remember(blurRadius) {
//        val blurIntensity = with(density) { blurRadius.toPx().coerceIn(1f, 100f) }
//
//        mutableStateOf(
//            RenderEffect.createBlurEffect(
//                blurIntensity.roundToInt().toFloat(),
//                blurIntensity.roundToInt().toFloat(),
//                Shader.TileMode.CLAMP
//            ).asComposeRenderEffect()
//        )
//    }

    val cf = remember { imageWithDynamicFilter(isDarkTheme) }
    // 动画控制，降低刷新频率
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            angle.value += 1f
            delay(128L)
        }
    }

    // 模糊背景图
//    AsyncImage(
//        model = ImageRequest.Builder(context)
//            .placeholderMemoryCacheKey(cover.smallImage()) // 先用小图占位
//            .data(cover.size1600())
//            .build(),
//        modifier = Modifier
//            .fillMaxSize()
//            .scale(scale = calculateScaleToFit())
//            .graphicsLayer {
//                rotationZ = angle.floatValue
//                renderEffect = blurEffect
//            },
//
//        colorFilter = cf,
//        contentDescription = null
//    )

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(cover.size1600())
            .transformations(BlurTransformation1(context,25f, 10f))
            .build(),
        modifier = Modifier
            .fillMaxSize()
            .scale(scale = calculateScaleToFit()),
        colorFilter = cf,
        contentDescription = null
    )

    // 半透明叠加层，减少模糊计算的需求
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if(isDarkTheme) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.6f))
    )
}

