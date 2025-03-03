package com.ljyh.music.ui.component.player

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 单行歌词实现（关键修改）
@Composable
fun LyricLineBate1(
    line: LyricLine,
    currentTimeMs: Long, // -1 表示非当前行
    darkColor: Color = MaterialTheme.colorScheme.primary,
    lightColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    transitionWidth: Float = 0.3f
) {


    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // 使用 remember 缓存计算结果
    val brush = remember(line, currentTimeMs, darkColor, lightColor, transitionWidth, textStyle) {
        if (currentTimeMs >= 0) {
            calculateBrushForLine(
                line,
                currentTimeMs,
                darkColor,
                lightColor,
                transitionWidth,
                textStyle,
                textMeasurer,
                density
            )
        } else {
            Brush.linearGradient(0f to lightColor, 1f to lightColor)
        }
    }

    BasicText(
        text = buildAnnotatedString { line.words.forEach { append(it.text) } },
        style = textStyle.copy(brush = brush),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .padding(8.dp)
    )
}


private fun calculateBrushForLine(
    line: LyricLine,
    currentTimeMs: Long,
    darkColor: Color,
    lightColor: Color,
    transitionWidth: Float,
    textStyle: TextStyle,
    textMeasurer: TextMeasurer,
    density: Density
): Brush {
    // 获取整行文本内容
    val fullText = line.words.joinToString("") { it.text }

    // 测量整行文字总宽度
    val totalWidth = with(density) {
        textMeasurer.measure(
            text = fullText,
            style = textStyle
        ).size.width.toFloat()
    }

    // 边界情况1：当前时间早于该行开始时间
    if (currentTimeMs < line.startTimeMs) {
        return Brush.horizontalGradient(0f to lightColor, 1f to lightColor)
    }

    // 边界情况2：当前时间晚于该行结束时间
    val lineEndTime = line.startTimeMs + line.durationMs
    if (currentTimeMs > lineEndTime) {
        return Brush.horizontalGradient(0f to darkColor, 1f to darkColor)
    }

    // 关键修正：找到最后一个已开始但未结束的词
    var accumulatedWidth = 0f
    var targetWord: LyricWord? = null
    var targetWordStartX = 0f
    var targetWordEndX = 0f

    line.words.forEach { word ->
        val wordStart = word.startTimeMs
        val wordEnd = wordStart + word.durationMs

        // 测量当前词宽度
        val wordWidth = with(density) {
            textMeasurer.measure(
                text = word.text,
                style = textStyle
            ).size.width.toFloat()
        }

        when {
            // 情况1：当前时间在该词时间范围内
            currentTimeMs in wordStart..wordEnd -> {
                targetWord = word
                targetWordStartX = accumulatedWidth
                targetWordEndX = accumulatedWidth + wordWidth
                accumulatedWidth += wordWidth
            }

            // 情况2：当前时间已过该词
            currentTimeMs > wordEnd -> {
                accumulatedWidth += wordWidth // 累加已播放词的完整宽度
            }

            // 情况3：当前时间未到该词
            else -> return@forEach
        }
    }

    return if (targetWord != null) {
        // 计算当前词内进度
        val wordProgress =
            (currentTimeMs - targetWord!!.startTimeMs).toFloat() / targetWord!!.durationMs.toFloat()

        // 计算当前播放位置的精确X坐标
        val currentX = targetWordStartX + (targetWordEndX - targetWordStartX) * wordProgress

        // 计算渐变区域（确保过渡区域完整可见）
        val start = (currentX - totalWidth * transitionWidth / 2).coerceAtLeast(0f)
        val end = (currentX + totalWidth * transitionWidth / 2).coerceAtMost(totalWidth)

        Brush.horizontalGradient(
            0f to darkColor,
            start / totalWidth to darkColor,
            end / totalWidth to lightColor,
            1f to lightColor,
            startX = 0f,
            endX = totalWidth
        )
    } else {
        // 异常情况回退（理论上不会执行）
        Brush.horizontalGradient(0f to darkColor, 1f to lightColor)
    }
}

val textStyle = TextStyle(
    textAlign = TextAlign.Left,
    fontSize = 28.sp,
    fontWeight = FontWeight.W800,
)