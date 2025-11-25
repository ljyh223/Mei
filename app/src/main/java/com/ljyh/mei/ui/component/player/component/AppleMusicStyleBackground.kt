package com.ljyh.mei.ui.component.player.component
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import android.graphics.RenderEffect // 核心 API
import androidx.compose.ui.unit.dp

// 假设这是你从 MaterialKolor 获取到的颜色模型
data class BackgroundColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val surface: Color
)

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AppleMusicStyleBackground(
    colors: BackgroundColors,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // 动画状态：控制色块的缓慢移动
    // Apple Music 的背景通常是极慢的呼吸感，不需要太快的旋转
    val infiniteTransition = rememberInfiniteTransition(label = "background_anim")

    // 1. 定义几个运动的参数 (0f 到 2PI)
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "t1"
    )
    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Restart),
        label = "t2"
    )

    // 2. 创建 RenderEffect (核心优化点)
    // 这种模糊是硬件加速的，比 ScriptIntrinsicBlur 快得多
    val blurEffect = remember {
        val blurRadius = with(density) { 100.dp.toPx() } // 极大的模糊半径
        RenderEffect.createBlurEffect(
            blurRadius,
            blurRadius,
            Shader.TileMode.MIRROR
        ).asComposeRenderEffect()
    }

    Box(
        modifier = modifier
            .background(colors.surface) // 底色
            .graphicsLayer {
                // 将模糊应用到整个绘制层
                renderEffect = blurEffect
                // 稍微放大一点，避免边缘模糊导致的透明
                scaleX = 1.2f
                scaleY = 1.2f
            }
    ) {
        // 在 Canvas 上绘制 3 个游走的巨大色块
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 计算色块位置 (使用三角函数做椭圆/像8字形的运动)
            val offset1 = Offset(
                x = w * 0.2f + (w * 0.3f) * cos(t1),
                y = h * 0.3f + (h * 0.2f) * sin(t1)
            )

            val offset2 = Offset(
                x = w * 0.8f - (w * 0.3f) * cos(t2),
                y = h * 0.5f + (h * 0.3f) * sin(t2 * 0.8f)
            )

            val offset3 = Offset(
                x = w * 0.5f + (w * 0.3f) * sin(t1 + t2),
                y = h * 0.8f + (h * 0.2f) * cos(t2)
            )

            // 绘制 Primary 色块
            drawCircle(
                color = colors.primary.copy(alpha = 0.8f),
                radius = w * 0.6f, // 半径要很大，占据屏幕一半以上
                center = offset1
            )

            // 绘制 Secondary 色块
            drawCircle(
                color = colors.secondary.copy(alpha = 0.8f),
                radius = w * 0.5f,
                center = offset2
            )

            // 绘制 Tertiary 色块
            drawCircle(
                color = colors.tertiary.copy(alpha = 0.8f),
                radius = w * 0.7f,
                center = offset3
            )
        }
    }

    // 进阶技巧：叠加一层噪点 (Noise)
    // Apple Music 为了防止色带(Banding)并增加质感，会有一层淡淡的噪点
    // 如果追求极致还原，可以加一张半透明的噪点图覆盖在上面
}