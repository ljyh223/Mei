package com.ljyh.mei.ui.component.player.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.utils.TimeUtils.makeTimeString
import kotlin.math.PI
import kotlin.math.roundToLong
import kotlin.math.sin


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProgressSlider(
    position: Long,
    duration: Long,
    isPlaying: Boolean, // 新增：需要根据播放状态控制波浪滚动
    onPositionChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDurationValid = remember(duration) { duration > 0 }
    val valueRange = remember(duration) { 0f..(duration.takeIf { it > 0 } ?: 1).toFloat() }

    // 交互状态
    val interactionSource = remember { MutableInteractionSource() }
    val isUserDragging by interactionSource.collectIsDraggedAsState()

    // 内部拖拽位置状态
    var rawDragPosition by remember { mutableFloatStateOf(0f) }

    // 计算当前显示的值
    val sliderPosition = if (isUserDragging) {
        rawDragPosition
    } else {
        position.toFloat()
    }.coerceIn(valueRange)

    // 计算进度百分比 (0.0 - 1.0)
    val progressFraction = if (isDurationValid) {
        (sliderPosition / valueRange.endInclusive).coerceIn(0f, 1f)
    } else 0f

    // 动画：波浪的相位 (Phase) - 让波浪动起来
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (2 * PI).toFloat() else 0f, // 只有播放时才滚动
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // 如果暂停了，我们也可以让波浪慢慢变平，或者保持静止。
    // Android 原生行为是播放时波浪滚动，拖拽时波浪变直。
    // 这里做一个平滑过渡：拖拽时振幅(Amplitude)变小（变直），播放时振幅正常
    val targetAmplitude = if (isUserDragging) 0f else if (isPlaying) 6f else 3f
    val amplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(300),
        label = "amplitude"
    )

    Column(modifier = modifier) {
        // 自定义 Slider
        // 我们利用 Material3 Slider 的逻辑，但是完全重写 Track 和 Thumb
        Slider(
            value = sliderPosition,
            onValueChange = {
                rawDragPosition = it
            },
            onValueChangeFinished = {
                if (isDurationValid) {
                    onPositionChange(rawDragPosition.roundToLong())
                }
            },
            valueRange = valueRange,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp), // 增加一点高度给波浪
            enabled = isDurationValid,

            // 隐藏原本的 Thumb (圆球)，我们自己画在 Track 里，或者不需要 Thumb
            thumb = {
                // 如果你想要一个光标，可以在这里画。
                // 安卓原生波浪条通常末端是一条竖线。
                // 这里我们做一个透明的占位，把绘制逻辑统一放到 track 里
                Spacer(Modifier.size(1.dp))
            },

            // 自定义轨道绘制
            track = { sliderState ->
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp) // 确保有足够空间画波峰波谷
                ) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2

                    // 1. 绘制未播放部分 (Inactive) - 直线
                    // 颜色：白色半透明 (适配所有背景)
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // 2. 绘制已播放部分 (Active) - 波浪线
                    if (progressFraction > 0) {
                        val activeWidth = width * progressFraction

                        // 构建正弦波路径
                        val path = Path()
                        path.moveTo(0f, centerY)

                        // 采样精度：每隔多少像素计算一次正弦值
                        val step = 5f
                        var x = 0f

                        // 频率
                        val frequency = 0.05f

                        while (x < activeWidth) {
                            // y = A * sin(ωx - φ) + k
                            // 减去 phase 是为了让波浪向右移动
                            val y = centerY + amplitude * sin(x * frequency - phase)
                            path.lineTo(x, y)
                            x += step
                        }

                        // 确保最后一点连上
                        val finalY = centerY + amplitude * sin(activeWidth * frequency - phase)
                        path.lineTo(activeWidth, finalY)

                        // 绘制波浪
                        drawPath(
                            path = path,
                            color = Color.White, // 纯白，高亮
                            style = Stroke(
                                width = 3.dp.toPx(), // 稍微粗一点
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // 3. 绘制末端指示器 (光标)
                        // 在波浪结束的位置画一个小圆点或竖线
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(), // 大小
                            center = Offset(activeWidth, finalY),
                            // 加一点阴影让它更明显
                        )
                    }
                }
            }
        )

        // 时间文字行
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp) // 稍微离波浪远一点
        ) {
            // 定义统一的时间文字样式
            val timeTextStyle = MaterialTheme.typography.labelMedium.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f
                )
            )

            Text(
                text = makeTimeString(sliderPosition.toLong()),
                style = timeTextStyle,
                color = Color.White.copy(alpha = 0.9f), // 使用白色，微透
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = if (duration > 0) makeTimeString(duration) else "-:--",
                style = timeTextStyle,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
