package com.ljyh.music.ui.component.player


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.music.constants.LyricTextAlignment
import com.ljyh.music.constants.PlayerHorizontalPadding
import com.ljyh.music.utils.textDp
import kotlin.math.min


@Composable
fun LyricLineBate(
    line: LyricLine,
    currentTimeMs: Long, // -1 表示非当前行
    textSize:Int,
    textBold:Boolean,
    textAlign: LyricTextAlignment,
    parentWidthDp: Dp,
    transitionWidth: Float = 0.3f,
    onClick: () -> Unit
) {
    val density = LocalDensity.current

    val textMeasurer = rememberTextMeasurer()
    val mainTextSize = textSize.textDp
    val translationTextSize = textSize.textDp * 0.8F


    val darkMode = isSystemInDarkTheme()  // 自动检测当前主题
    val mainTextColor = if (darkMode) Color.White else Color.Black
    val translationTextColor = if (darkMode) Color(0xFFAAAAAA) else Color(0xFF444444)
    val sungTextColor = if (darkMode) Color(0xFFFFC107) else Color(0xFF0084FF) // 唱过的部分，黄色/蓝色

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxTextWidth = with(density) { (screenWidth - PlayerHorizontalPadding * 2 - 16.dp).toPx() }
    val textAlpha = animateFloatAsState(if (currentTimeMs >= 0) 1F else 0.32F, label = "").value
    val textAlignment=when(textAlign){
        LyricTextAlignment.Left -> TextAlign.Left
        LyricTextAlignment.Center -> TextAlign.Center
        LyricTextAlignment.Right -> TextAlign.Right
    }
    val fontWeight:FontWeight = if(textBold){
        if (currentTimeMs>0) FontWeight.W800 else FontWeight.W600
    }else{
        if (currentTimeMs>0) FontWeight.Bold else FontWeight.Normal
    }
    val mainTextStyle = TextStyle(
        fontSize = mainTextSize,
        color = mainTextColor,
        fontWeight = fontWeight,
        lineHeight = mainTextSize * 1.5F,
        textAlign = textAlignment
        )
    val mainTextLayoutResult = textMeasurer.measure(
        text = line.lyric,
        style = mainTextStyle,
        constraints = Constraints(maxWidth = maxTextWidth.toInt())
    )
    val translationTextStyle = TextStyle(
        fontSize = translationTextSize,
        color = translationTextColor,
        fontWeight = fontWeight,
        lineHeight = translationTextSize * 1.5F,
        textAlign = textAlignment
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

            val textOffsetX = when (textAlignment) {
                TextAlign.Left -> 0f
                TextAlign.Center -> (size.width - mainTextLayoutResult.size.width) / 2
                TextAlign.Right -> size.width - mainTextLayoutResult.size.width
                else -> 0f
            }

            drawText(
                textLayoutResult = mainTextLayoutResult,
                alpha = textAlpha,
                topLeft = Offset(textOffsetX, 0f)
            )

            line.translation?.let {
                val translationOffsetX = when (textAlignment) {
                    TextAlign.Left -> 0f
                    TextAlign.Center -> (size.width - translationLayoutResult.size.width) / 2
                    TextAlign.Right -> size.width - translationLayoutResult.size.width
                    else -> 0f
                }
                drawText(
                    textLayoutResult = translationLayoutResult,
                    topLeft = Offset(translationOffsetX, textHeight + textSize * 0.3F),
                    alpha = textAlpha,
                )
            }

            if (line.words.isNotEmpty()) {
                val textWidth = calculateSungWidth(
                    currentTimeMs,
                    line,
                    textMeasurer,
                    mainTextStyle
                )

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
                        val lineStartX = when (textAlignment) {
                            TextAlign.Left -> multiParagraph.getLineLeft(i)
                            TextAlign.Center -> (size.width - multiParagraph.getLineWidth(i)) / 2
                            TextAlign.Right -> size.width - multiParagraph.getLineWidth(i)
                            else -> multiParagraph.getLineLeft(i)
                        }
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
    // 计算所有单词的最晚结束时间
    val maxWordEnd = line.words.maxOfOrNull { it.startTimeMs + it.durationMs } ?: lineEnd

    if (currentTimeMs < lineStart) return 0f  // 行未开始
    // 若当前时间超过所有单词结束时间或行结束时间，返回总宽度
    if (currentTimeMs >= maxWordEnd || currentTimeMs >= lineEnd) {
        return line.measuredWidth ?: textMeasurer.measure(
            text = totalText,
            style = textStyle
        ).size.width.toFloat().also { line.measuredWidth = it }
    }

    var sungWidth = 0f

    for (word in line.words) {
        val wordStart = word.startTimeMs
        val wordEnd = wordStart + word.durationMs
        // 单词未开始，跳过
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



@Composable
fun Test() {
}