package com.ljyh.mei.ui.component.player.component

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AppleMusicFluidBackground(
    imageUrl: String?, // 传入 URL 即可
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current

    // 默认兜底颜色 (深灰/黑)
    val defaultColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFE0E0E0)

    // 状态：当前使用的 4 个流体颜色
    var fluidColors by remember { mutableStateOf(List(4) { defaultColor }) }

    // 1. 使用 Coil 3 异步加载图片并提取颜色
    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrEmpty()) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .size(128) // 【关键优化】只加载极小的缩略图，取色够用了，性能极快
                .allowHardware(false) // 【必须】Palette 无法读取硬件位图的像素，必须关掉
                .build()

            val result = loader.execute(request)

            if (result is SuccessResult) {
                val bitmap = result.image.toBitmap()
                val newColors = extractVibrantColors(bitmap, isDark)
                fluidColors = newColors
            }
        }
    }

    // 2. 颜色平滑过渡动画 (防止切歌时背景闪烁)
    val animSpec = tween<Color>(durationMillis = 1500, easing = LinearEasing)
    val color1 by animateColorAsState(fluidColors[0], animSpec, label = "c1")
    val color2 by animateColorAsState(fluidColors[1], animSpec, label = "c2")
    val color3 by animateColorAsState(fluidColors[2], animSpec, label = "c3")
    val color4 by animateColorAsState(fluidColors[3], animSpec, label = "c4")

    // 3. 极致模糊设置
    val blurEffect = remember {
        val radius = with(density) { 140.dp.toPx() } // 半径越大越不像“球”，越像“光”
        RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR)
            .asComposeRenderEffect()
    }

    // 4. 渲染层
    // 背景底色选择颜色组里最深的一个，保证深邃感
    val baseBackground = if(isDark) color2.darken(0.5f) else Color(0xFFF2F2F7)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackground)
    ) {
        // --- 流体层 (GPU 加速) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    renderEffect = blurEffect
                    // 稍微放大一点，防止模糊导致边缘出现黑边
                    scaleX = 1.2f
                    scaleY = 1.2f
                    alpha = if(isDark) 0.9f else 1f
                }
        ) {
            FluidCanvas(
                colors = listOf(color1, color2, color3, color4),
                isDark = isDark
            )
        }

        // --- 遮罩层 (Scrim) ---
        // 保证底部的播放控制按钮可见
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.5f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.6f)
                    )
                )
        )
    }
}

@Composable
fun FluidCanvas(colors: List<Color>, isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_movement")

    // 定义三个巨大的“光斑”运动轨迹
    // 这种缓慢的、非同步的移动是产生“高级感”的关键
    val move1 by infiniteTransition.animateFloat(
        initialValue = -0.1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse), label = "m1"
    )
    val move2 by infiniteTransition.animateFloat(
        initialValue = 1.1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "m2"
    )
    val scaleCenter by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse), label = "scale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // 关键：半径要是屏幕宽度的 80%~100%，让球体互相融合
        val radius = max(w, h) * 0.9f

        // 1. 顶部主光斑 (通常是 Vibrant)
        drawCircle(
            color = colors[0].copy(alpha = if(isDark) 0.7f else 0.5f),
            radius = radius,
            center = Offset(w * 0.5f + w * move1, h * 0.2f)
        )

        // 2. 底部深色光斑 (Dark Vibrant)
        drawCircle(
            color = colors[1].copy(alpha = if(isDark) 0.8f else 0.5f),
            radius = radius * 1.1f,
            center = Offset(w * 0.2f, h * 0.9f)
        )

        // 3. 侧边强调色 (Light/Muted - 那抹绿色/补色)
        drawCircle(
            color = colors[3].copy(alpha = if(isDark) 0.6f else 0.4f),
            radius = radius * 0.8f,
            center = Offset(w * move2, h * 0.5f)
        )

        // 4. 屏幕中央的高光呼吸 (Light Vibrant)
        drawCircle(
            color = colors[2].copy(alpha = 0.5f),
            radius = radius * 0.6f * scaleCenter,
            center = Offset(w * 0.5f, h * 0.5f)
        )
    }
}

// --- 核心色彩算法 ---

/**
 * 提取并计算“Vibrant”风格的颜色组
 */
fun extractVibrantColors(bitmap: Bitmap, isDark: Boolean): List<Color> {
    val palette = Palette.from(bitmap).generate()

    // 1. 优先提取 Palette 识别到的鲜艳颜色
    val vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) }
    val darkVibrant = palette.darkVibrantSwatch?.rgb?.let { Color(it) }
    val lightVibrant = palette.lightVibrantSwatch?.rgb?.let { Color(it) }
    val muted = palette.mutedSwatch?.rgb?.let { Color(it) }
    val dominant = palette.dominantSwatch?.rgb?.let { Color(it) }

    // 确定一个种子颜色 (如果没有 Vibrant，就用 Dominant，还不行就默认)
    val seed = vibrant ?: dominant ?: if(isDark) Color(0xFF1E1E1E) else Color.White

    // 2. 智能填充逻辑 (解决纯色封面问题)
    // 如果提取不到某些特定的颜色，我们根据种子颜色“算”出来，而不是留空
    val c1 = vibrant ?: seed.saturation(1.5f) // 主色：加饱和
    val c2 = darkVibrant ?: seed.darken(0.4f) // 深色：压暗
    val c3 = lightVibrant ?: seed.lighten(0.3f).shiftHue(15f) // 高光：提亮+微偏色
    // c4 是关键：如果有 Muted (比如红色封面里的一抹绿)，就用它；
    // 如果没有，就计算一个互补色或者大角度偏色，制造反差感
    val c4 = muted ?: seed.shiftHue(180f).copy(alpha = 0.6f)

    // 3. 统一滤镜处理：让所有颜色都鲜艳一点，通透一点
    val colors = listOf(c1, c2, c3, c4).map {
        it.saturation(1.4f) // 全局提升 40% 饱和度
    }

    return colors
}

// --- Color Extensions (直接复制到底部) ---

fun Color.saturation(multiplier: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[1] = (hsl[1] * multiplier).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

fun Color.darken(factor: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[2] = (hsl[2] * (1f - factor)).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

fun Color.lighten(factor: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[2] = (hsl[2] + (1f - hsl[2]) * factor).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

fun Color.shiftHue(amount: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    hsl[0] = (hsl[0] + amount).mod(360f)
    return Color(ColorUtils.HSLToColor(hsl))
}