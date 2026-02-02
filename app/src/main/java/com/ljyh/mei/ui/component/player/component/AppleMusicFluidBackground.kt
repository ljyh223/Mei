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
import androidx.compose.ui.graphics.BlendMode
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
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.random.Random

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AppleMusicFluidBackground(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current

    // 定义初始种子颜色（避免纯灰）
    val defaultColors = remember(isDark) {
        if (isDark) {
            listOf(Color(0xFF2C1E4A), Color(0xFF52154E), Color(0xFF111135), Color(0xFF0F0F1A))
        } else {
            listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC), Color(0xFFE0E0E0), Color(0xFFFFFFFF))
        }
    }

    var fluidColors by remember { mutableStateOf(defaultColors) }

    // 1. 异步取色
    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrEmpty()) return@LaunchedEffect

        // 放在 IO 线程，不阻塞 UI
        withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .size(200) // 稍微大一点点以获得更准确的色调，但仍然很小
                .allowHardware(false)
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.image.toBitmap()
                // 使用增强版取色算法
                val newColors = extractVibrantColorsImproved(bitmap, isDark)
                fluidColors = newColors
            }
        }
    }

    // 2. 动画设置：时间稍微缩短一点，LinearEasing 改为 FastOutSlowIn 让变化更有节奏
    val animSpec = tween<Color>(durationMillis = 1200, easing = FastOutSlowInEasing)
    val c1 by animateColorAsState(fluidColors[0], animSpec, label = "c1")
    val c2 by animateColorAsState(fluidColors[1], animSpec, label = "c2")
    val c3 by animateColorAsState(fluidColors[2], animSpec, label = "c3")
    val c4 by animateColorAsState(fluidColors[3], animSpec, label = "c4")

    // 3. 模糊半径优化：不要过大，保留一点色块的形体感
//    val blurEffect = remember {
//        val radius = with(density) { 100.dp.toPx() } // 从 140 降到 100，避免混成一锅粥
//        RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR)
//            .asComposeRenderEffect()
//    }

    val blurEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        remember {
            val radius = with(density) { 100.dp.toPx() }
            RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR)
                .asComposeRenderEffect()
        }
    } else null

    Box(modifier = modifier.fillMaxSize()) {
        // 背景底色：使用最深的一个颜色作为底，避免漏光
        // 增加 saturation 确保底色也不灰
        val baseColor = if(isDark) c2.darken(0.6f) else Color(0xFFF0F0F0)

        Box(modifier = Modifier
            .fillMaxSize()
            .background(baseColor)
        )

        // 流体层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurEffect != null) {
                        // Android 12+：保留你原来的 RenderEffect 逻辑
                        Modifier.graphicsLayer {
                            renderEffect = blurEffect
                            scaleX = 1.3f
                            scaleY = 1.3f
                        }
                    } else {
                        Modifier.cloudy(radius = 25)
                            .graphicsLayer {
                                scaleX = 1.3f
                                scaleY = 1.3f
                            }
                    }
                )
        ) {
            FluidCanvasImproved(
                colors = listOf(c1, c2, c3, c4),
                isDark = isDark
            )
        }

        // 可选：添加一点噪点纹理，会让渐变看起来更高级（防止色带）
        // 这里为了性能暂时省略，如果需要可以叠加一个半透明的噪点图

        // 遮罩层：加深底部，确保文字可读
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color.Black.copy(alpha = 0.1f),
                        1f to Color.Black.copy(alpha = 0.7f) // 底部加深
                    )
                )
        )
    }
}

@Composable
fun FluidCanvasImproved(colors: List<Color>, isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid")

    // 定义三种不同频率的运动，打破规律感
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse), label = "t1"
    )
    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(23000, easing = LinearEasing), RepeatMode.Reverse), label = "t2"
    )
    val t3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse), label = "t3"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 混合模式：Screen (滤色) 或 Overlay 可以让颜色叠加更亮，避免变灰
        // 注意：Android Canvas 对 BlendMode 支持有限，通常默认 SrcOver 就够了
        // 关键在于画笔的 alpha 和位置

        // 1. 主色块（左上，大范围移动）
        drawCircle(
            color = colors[0].copy(alpha = 0.8f),
            radius = w * 0.7f,
            center = Offset(w * 0.2f + (w * 0.3f * t1), h * 0.3f - (h * 0.1f * t2))
        )

        // 2. 辅助色块（右下，定调色）
        drawCircle(
            color = colors[1].copy(alpha = 0.8f),
            radius = w * 0.8f,
            center = Offset(w * 0.9f - (w * 0.3f * t2), h * 0.8f - (h * 0.2f * t1))
        )

        // 3. 提亮色块（中间偏左，呼吸感）- 这里的颜色最亮
        val scale3 = 0.8f + (0.3f * t3) // 忽大忽小
        drawCircle(
            color = colors[2].copy(alpha = 0.6f),
            radius = w * 0.5f * scale3,
            center = Offset(w * 0.3f, h * 0.5f + (h * 0.2f * t2))
        )

        // 4. 补色/强调色块（右上，游走）- 制造冲突感
        drawCircle(
            color = colors[3].copy(alpha = 0.6f),
            radius = w * 0.6f,
            center = Offset(w * 0.8f, h * 0.2f + (h * 0.4f * t3))
        )
    }
}

/**
 * 改进版取色逻辑：
 * 核心目标：拒绝灰色，制造对比
 */
fun extractVibrantColorsImproved(bitmap: Bitmap, isDark: Boolean): List<Color> {
    val palette = Palette.from(bitmap)
        // 增加最大颜色数，以便更容易找到鲜艳颜色
        .maximumColorCount(24)
        .generate()

    // 1. 获取所有可用 Swatch，优先 Vibrant
    val vibrant = palette.vibrantSwatch
    val darkVibrant = palette.darkVibrantSwatch
    val lightVibrant = palette.lightVibrantSwatch
    val dominant = palette.dominantSwatch

    // 2. 确定基准色 (Seed)
    // 如果没有 Vibrant，就用 Dominant；如果 Dominant 也很灰，就强行指定一个深蓝/深紫作为基底
    val seedSwatch = vibrant ?: dominant
    val seedColor = if (seedSwatch != null) {
        Color(seedSwatch.rgb)
    } else {
        // 兜底：如果是黑白封面，给一个高级的深蓝色
        if (isDark) Color(0xFF1A237E) else Color(0xFFE8EAF6)
    }

    // 3. 构建颜色列表
    // 逻辑：
    // Color 1: 主色 (尽可能鲜艳)
    // Color 2: 深色 (用于底部，稳住重心)
    // Color 3: 亮色 (用于中间呼吸光斑)
    // Color 4: 变化色 (色相偏移，制造“生动感”)

    var c1 = vibrant?.rgb?.let { Color(it) } ?: seedColor.boostSaturation(1.5f)
    var c2 = darkVibrant?.rgb?.let { Color(it) } ?: c1.darken(0.4f)
    var c3 = lightVibrant?.rgb?.let { Color(it) } ?: c1.lighten(0.3f)

    // 4. 强制增强饱和度 (关键步骤)
    // 检测颜色是否太灰，如果太灰，极大提升饱和度
    if (c1.isGrayscale()) c1 = c1.boostSaturation(3.0f).forceHueIfGray()
    if (c2.isGrayscale()) c2 = c2.boostSaturation(2.0f).forceHueIfGray()

    // 对所有颜色统一加一次滤镜
    c1 = c1.boostSaturation(1.3f)
    c2 = c2.boostSaturation(1.3f) // 深色稍微少加一点
    c3 = c3.boostSaturation(1.3f).lighten(0.1f)

    // 5. 生成第 4 个颜色：偏移色相
    // 取主色，旋转 45~60 度，得到邻近色；或者旋转 180 度得到互补色
    // 这里选择旋转 40 度，产生丰富的渐变层次，不至于太突兀
    val c4 = c1.shiftHue(40f).lighten(0.1f)

    return listOf(c1, c2, c3, c4)
}

// --- 颜色处理工具扩展 ---

fun Color.isGrayscale(threshold: Float = 0.15f): Boolean {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    return hsl[1] < threshold // 饱和度低于 15% 视为灰色
}

fun Color.forceHueIfGray(): Color {
    // 如果仍然是纯灰（比如 saturation=0 怎么提升都没用），强制赋予一个冷色调
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    if (hsl[1] < 0.05f) {
        hsl[0] = 240f // 强制蓝色
        hsl[1] = 0.5f // 强制 50% 饱和度
    }
    return Color(ColorUtils.HSLToColor(hsl))
}

fun Color.boostSaturation(multiplier: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    // 稍微增加一点亮度，防止增加饱和度后颜色变黑
    if (hsl[2] < 0.2f) hsl[2] = 0.2f

    hsl[1] = (hsl[1] * multiplier).coerceIn(0.2f, 1f) // 保证至少有 0.2 的饱和度
    return Color(ColorUtils.HSLToColor(hsl))
}

// 保持你原有的 helper extensions
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