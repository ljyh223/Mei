package com.ljyh.mei.ui.component.player.controls

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.constants.MusicQualityKey
import com.ljyh.mei.ui.component.player.LocalPlayerForegroundColor
import com.ljyh.mei.utils.TimeUtils.makeTimeString
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluidProgressSlider(
    position: Long,
    duration: Long,
    onPositionChange: (Long) -> Unit,
    onPositionPreview: (Long) -> Unit = {},
    onMusicQualityClick: () -> Unit,
    isBuffering: Boolean,
    showMusicQuality: Boolean = true,
    showRemainingTime: Boolean = false,
    modifier: Modifier = Modifier
) {
    val foreground = LocalPlayerForegroundColor.current
    val musicQuality by rememberEnumPreference(MusicQualityKey, MusicQuality.EXHIGH)
    val isDurationValid = remember(duration) { duration > 0 }
    val valueRange = remember(duration) { 0f..(duration.takeIf { it > 0 } ?: 1).toFloat() }

    // 交互状态监听
    val interactionSource = remember { MutableInteractionSource() }
    val isUserDragging by interactionSource.collectIsDraggedAsState()
    val isUserPressed by interactionSource.collectIsPressedAsState()
    val bufferingPhase = rememberBufferingSweep(isBuffering && !isUserDragging)

    // 按住或拖拽时只提高已播放部分的不透明度，轨道高度保持不变。
    val isInteracting = isUserDragging || isUserPressed

    // 内部拖拽位置状态
    var rawDragPosition by remember { mutableFloatStateOf(0f) }
    var hasScrubbed by remember { mutableStateOf(false) }

    // 计算当前显示的值
    val sliderPosition = if (isUserDragging && hasScrubbed) {
        rawDragPosition
    } else {
        position.toFloat()
    }.coerceIn(valueRange)

    val trackHeight = 6.dp
    val activeTrackAlpha by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0.65f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "activeTrackAlpha",
    )

    val textAlpha = 0.6f

    Column(modifier = modifier) {

        // Slider 组件
        // 固定容器高度，按压和拖拽都不改变布局。
        Box(
            modifier = Modifier.height(20.dp), // 容器高度固定，保证触摸区域一致
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = sliderPosition,
                onValueChange = {
                    rawDragPosition = it
                    hasScrubbed = true
                    onPositionPreview(it.roundToLong())
                },
                onValueChangeFinished = {
                    if (isDurationValid) {
                        onPositionChange(rawDragPosition.roundToLong())
                    }
                    hasScrubbed = false
                },
                valueRange = valueRange,
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
                enabled = isDurationValid,

                // 去掉默认的颜色处理，完全在 track/thumb 中自定义
                colors = SliderDefaults.colors(
                    thumbColor = foreground,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                thumb = {},
                track = { sliderState ->
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
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
                            color = foreground.copy(alpha = 0.3f),
                            topLeft = Offset(0f, 0f),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(height / 2, height / 2) // 完全圆角
                        )

                        // 2. 已播放轨道仅在交互时提高不透明度。
                        if (activeWidth > 0) {
                            drawRoundRect(
                                color = foreground.copy(alpha = foreground.alpha * activeTrackAlpha),
                                topLeft = Offset(0f, 0f),
                                size = Size(activeWidth, height),
                                cornerRadius = CornerRadius(height / 2, height / 2)
                            )
                        }
                        bufferingPhase?.let { phase ->
                            drawBufferingSweep(
                                playedFraction = fraction,
                                phase = phase,
                                color = foreground,
                                centerY = centerY,
                                strokeWidth = height,
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
                .padding(top = 4.dp) // Apple Music 的文字离进度条有一点距离
        ) {
            val timeTextStyle = MaterialTheme.typography.labelSmall.copy(
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
                modifier = Modifier.weight(1f),
                text = makeTimeString(sliderPosition.toLong()),
                style = timeTextStyle,
                color = foreground.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )

                // 中间：音质 (复用你原本的逻辑)
            if (showMusicQuality) {
                PlayerQualityLabel(
                    modifier = Modifier.weight(1f),
                    quality = musicQuality,
                    textStyle = timeTextStyle,
                    color = foreground.copy(alpha = 0.8f),
                    onClick = onMusicQualityClick,
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Text(
                modifier = Modifier.weight(1f),
                text = if (duration > 0) {
                    if (showRemainingTime) {
                        "-" + makeTimeString(
                            (duration - sliderPosition.toLong()).coerceAtLeast(0L)
                        )
                    } else {
                        makeTimeString(duration)
                    }
                } else "-:--",
                style = timeTextStyle,
                color = foreground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
    }
}
