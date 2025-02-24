package com.ljyh.music.ui.component.player

import android.graphics.RenderEffect
import android.graphics.Shader
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.music.ui.component.utils.calculateScaleToFit
import com.ljyh.music.utils.size1600
import com.ljyh.music.utils.smallImage
import com.ljyh.music.utils.toPx
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun OptimizedBlurredImage(
    cover: String,
    isPlaying: Boolean,
    blurRadius: Dp = 50.dp // 动态控制模糊半径
) {
    val context = LocalContext.current
    val angle = remember { mutableFloatStateOf(0f) }

    // 使用记忆化缓存模糊效果，避免重复计算
    val blurEffect by remember(blurRadius) {
        mutableStateOf(
            RenderEffect.createBlurEffect(
                blurRadius.toPx(context), blurRadius.toPx(context), Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        )
    }

    // 动画控制，降低刷新频率
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            angle.value += 1f
            delay(32L) // 每 32ms 更新一次 (30FPS)，减少动画更新频率
        }
    }

    // 模糊背景图
    AsyncImage(
        model = cover.size1600(),
        modifier = Modifier
            .fillMaxSize()
            .scale(scale = calculateScaleToFit())
            .graphicsLayer {
                rotationZ = angle.floatValue
                renderEffect = blurEffect
            },
        contentDescription = null
    )

    // 半透明叠加层，减少模糊计算的需求
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f))
    )
}

