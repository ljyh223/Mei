package com.ljyh.music.ui.component.player


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljyh.music.utils.textDp
import kotlin.math.min


// 单行歌词实现（关键修改）
@Composable
fun LyricLineView(
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

@Composable
fun LyricLineDemo1(
    line: LyricLine,
    currentTimeMs: Long, // -1 表示非当前行
    transitionWidth: Float = 0.3f,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val textSize = 20

    val textMeasurer = rememberTextMeasurer()
    val mainTextSize = textSize.textDp
    val translationTextSize = textSize.textDp * 0.8F
    val darkMode = isSystemInDarkTheme()  // 自动检测当前主题
    val mainTextColor = if (darkMode) Color.White else Color.Black
    val translationTextColor = if (darkMode) Color(0xFFAAAAAA) else Color(0xFF444444)
    val sungTextColor = if (darkMode) Color(0xFFFFC107) else Color(0xFF0084FF) // 唱过的部分，黄色/蓝色
    val textAlpha = animateFloatAsState(if (currentTimeMs >= 0) 1F else 0.32F, label = "").value
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxTextWidth = with(density) { (screenWidth - 32.dp).toPx() }
    val mainTextStyle = TextStyle(
        fontSize = mainTextSize,
        color = mainTextColor,
        fontWeight = FontWeight.W800,
        lineHeight = mainTextSize * 1.5F,

    )
    val mainTextLayoutResult = textMeasurer.measure(
        text = line.lyric,
        style = mainTextStyle,
        constraints = Constraints(maxWidth = maxTextWidth.toInt())
    )
    val translationTextStyle = TextStyle(
        fontSize = translationTextSize,
        color = translationTextColor,
        fontWeight = FontWeight.W800,
        lineHeight = translationTextSize * 1.5F
    )
    val translationLayoutResult = textMeasurer.measure(
        text = line.translation ?: "",
        style = translationTextStyle,
        constraints = Constraints(maxWidth = maxTextWidth.toInt())
    )
    val paddingY = (textSize * 0.3F).dp



    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(0.dp, (textSize * 0.1F).dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color.Transparent),
        elevation = CardDefaults.cardElevation()
    ) {

        val canvasHeight = if (line.translation != null) {
            (mainTextLayoutResult.size.height + translationLayoutResult.size.height + textSize * 0.3F).toInt()
        } else {
            mainTextLayoutResult.size.height
        }
        Canvas(
            modifier = Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .fillMaxWidth()
                .padding(8.dp, paddingY)
                .height(with(density) { canvasHeight.toDp() })

        ) {
            val multiParagraph = mainTextLayoutResult.multiParagraph
            val textHeight = mainTextLayoutResult.size.height.toFloat()

            drawText(textLayoutResult = mainTextLayoutResult, alpha = textAlpha, topLeft = Offset(0f, 0f))
            line.translation?.let {
                drawText(
                    textLayoutResult = translationLayoutResult,
                    topLeft = Offset(0f, textHeight + textSize * 0.3F),
                    alpha = textAlpha,
                )
            }

            if (line.words.isNotEmpty()){
                val textWidth = calculateSungWidth(
                    currentTimeMs,
                    line,
                    textMeasurer,
                    mainTextStyle
                )

                println("lyric width ==> ${mainTextLayoutResult.size.width}")
                println("width ==> $textWidth")
                if (currentTimeMs >= 0 && textWidth > 0f) {

                    val sungWidth = calculateSungWidth(
                        currentTimeMs,
                        line,
                        textMeasurer,
                        mainTextStyle
                    )

                    // 遍历每一行，计算需要填充的部分
                    var remainingSungWidth = sungWidth
                    for (i in 0 until multiParagraph.lineCount) {
                        val lineStartX = multiParagraph.getLineLeft(i)
                        val lineWidth = multiParagraph.getLineWidth(i)
                        val lineTopY = multiParagraph.getLineTop(i)
                        val lineBottomY = multiParagraph.getLineBottom(i)

                        if (remainingSungWidth <= 0) break

                        val highlightWidth = min(remainingSungWidth, lineWidth)
                        drawRect(
                            color = sungTextColor,
                            topLeft = Offset(lineStartX, lineTopY),
                            size = Size(highlightWidth, lineBottomY - lineTopY),
                            blendMode = BlendMode.SrcIn
                        )

                        remainingSungWidth -= highlightWidth
                    }
                }
            }

        }
    }
}


fun calculateSungWidth(
    currentTimeMs: Long,
    line: LyricLine,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle
): Float {
    if (line.words.isEmpty()) return 0f

    val lineStart = line.startTimeMs
    val lineEnd = lineStart + line.durationMs
    val totalText = line.words.joinToString("") { it.text }
    if (currentTimeMs < lineStart) return 0f  // 行未开始
    if (currentTimeMs >= lineEnd) return line.measuredWidth ?: textMeasurer.measure(
        text = totalText,
        style = textStyle
    ).size.width.toFloat().also { line.measuredWidth = it }  // 行已结束，返回总宽度

    var sungWidth = 0f

    for (word in line.words) {
        val wordStart = word.startTimeMs
        val wordEnd = wordStart + word.durationMs
        // 单词未开始或行已结束，跳过
        if (currentTimeMs < wordStart) continue

        // 计算单词在行内的相对开始时间和结束时间
        val wordStartInLine = wordStart - lineStart
        val wordEndInLine = wordStartInLine + word.durationMs
        val elapsedInLine = currentTimeMs - lineStart

        // 测量单词宽度
        val wordWidth = textMeasurer.measure(word.text, textStyle).size.width.toFloat()

        // 判断当前时间在单词时间范围内的位置
        when {
            currentTimeMs >= wordEnd -> {
                sungWidth += wordWidth  // 单词已唱完
            }

            elapsedInLine >= wordStartInLine -> {
                val timeInWord = currentTimeMs - wordStart
                val progress = timeInWord.coerceAtLeast(0).toFloat() / word.durationMs
                sungWidth += wordWidth * progress.coerceIn(0f, 1f)
            }
            // else: 单词未开始，不累加
        }
    }

    return sungWidth
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

@Composable
fun Test() {
}