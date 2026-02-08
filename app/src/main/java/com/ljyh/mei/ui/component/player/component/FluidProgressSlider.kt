package com.ljyh.mei.ui.component.player.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.constants.MusicQualityKey
import com.ljyh.mei.utils.TimeUtils.makeTimeString
import com.ljyh.mei.utils.rememberEnumPreference
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluidProgressSlider(
    position: Long,
    duration: Long,
    onPositionChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val musicQuality by rememberEnumPreference(MusicQualityKey, MusicQuality.EXHIGH)
    val isDurationValid = remember(duration) { duration > 0 }
    val valueRange = remember(duration) { 0f..(duration.takeIf { it > 0 } ?: 1).toFloat() }

    // 交互状态监听
    val interactionSource = remember { MutableInteractionSource() }
    val isUserDragging by interactionSource.collectIsDraggedAsState()
    val isUserPressed by interactionSource.collectIsPressedAsState()

    // Apple Music 的逻辑是：只要按住 (Pressed) 或者 拖拽 (Dragged)，条就会变粗
    val isInteracting = isUserDragging || isUserPressed

    // 内部拖拽位置状态
    var rawDragPosition by remember { mutableFloatStateOf(0f) }

    // 计算当前显示的值
    val sliderPosition = if (isUserDragging) {
        rawDragPosition
    } else {
        position.toFloat()
    }.coerceIn(valueRange)

    // --- Apple Music 风格动画核心 ---

    // 1. 轨道高度动画：平时很细(2dp)，按住时变粗(10dp)
    val trackHeight by animateDpAsState(
        targetValue = if (isInteracting) 10.dp else 2.dp, // 这里的数值决定了“变粗”的程度
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "trackHeight"
    )

    // 2. 滑块(Thumb)大小动画：平时不可见或极小(0dp)，按住时出现(12dp)
    // Apple Music 在静止时其实看不到滑块，或者滑块和轨道一样高。这里我们设定静止时滑块隐藏(0dp)。
    val thumbRadius by animateDpAsState(
        targetValue = if (isInteracting) 8.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // 稍微带点弹性
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumbRadius"
    )

    // 3. 时间文字大小/透明度动画 (可选，Apple Music 有时会在拖拽时让文字变亮)
    val textAlpha = 0.6f // 保持恒定，或者根据 isInteracting 改变

    Column(modifier = modifier) {

        // Slider 组件
        // 注意：Slider 本身的高度必须足够容纳“变粗”后的状态，否则会被裁剪
        Box(
            modifier = Modifier.height(20.dp), // 容器高度固定，保证触摸区域一致
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = sliderPosition,
                onValueChange = { rawDragPosition = it },
                onValueChangeFinished = {
                    if (isDurationValid) {
                        onPositionChange(rawDragPosition.roundToLong())
                    }
                },
                valueRange = valueRange,
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
                enabled = isDurationValid,

                // 去掉默认的颜色处理，完全在 track/thumb 中自定义
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),

                // 自定义 Thumb (圆球)
                thumb = {
                    // 绘制一个白色的圆
                    Canvas(modifier = Modifier.size(thumbRadius * 2)) {
                        drawCircle(
                            color = Color.White,
                            radius = thumbRadius.toPx(),
                            // 可以加一点阴影让它更有立体感
                            // shadow = Shadow(...)
                        )
                    }
                },

                // 自定义 Track (轨道)
                track = { sliderState ->
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(trackHeight) // 轨道高度跟随动画
                    ) {
                        val width = size.width
                        val height = size.height
                        val centerY = height / 2

                        // 计算进度比例
                        val fraction = if (valueRange.endInclusive > 0) {
                            (sliderState.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                        } else 0f

                        val activeWidth = width * fraction

                        // 1. 绘制背景轨道 (Inactive) - 半透明灰色/白色
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.3f),
                            topLeft = Offset(0f, 0f),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(height / 2, height / 2) // 完全圆角
                        )

                        // 2. 绘制已播放轨道 (Active) - 实心白色
                        if (activeWidth > 0) {
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(0f, 0f),
                                size = Size(activeWidth, height),
                                cornerRadius = CornerRadius(height / 2, height / 2)
                            )
                        }
                    }
                }
            )
        }

        // 时间文字行
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp) // Apple Music 的文字离进度条有一点距离
        ) {
            val commonTextStyle = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium, // 字重稍微粗一点点
                fontSize = 12.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.2f),
                    offset = Offset(0f, 1f),
                    blurRadius = 2f
                )
            )

            // 左侧：当前时间
            Text(
                text = makeTimeString(sliderPosition.toLong()),
                style = commonTextStyle,
                color = Color.White.copy(alpha = 0.9f),
            )

            // 中间：音质 (复用你原本的逻辑)
            Text(
                text = musicQuality.explanation,
                style = commonTextStyle.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.6f),
            )

            // 右侧：剩余时间 (Apple Music 风格通常显示剩余时间，即 "-03:45")
            // 这里为了兼容性，我先显示总时长，如果你想改剩余时间，可以改为:
            // "-" + makeTimeString(duration - sliderPosition.toLong())
            Text(
                text = if (duration > 0) makeTimeString(duration) else "-:--",
                style = commonTextStyle,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}