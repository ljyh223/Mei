package com.ljyh.mei.ui.component.player.component


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.ljyh.mei.constants.LyricTextAlignment
import com.ljyh.mei.utils.UnitUtils.textDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LyricLineBate(
    line: LyricLine,
    currentTimeMs: Long, // -1 表示非当前行
    textSize: Int,
    textBold: Boolean,
    textAlign: LyricTextAlignment,
    maxWidthDp: Dp, // 从父组件传入的最大宽度
    transitionWidthDp: Dp = 20.dp, // 为渐变过渡提供一个默认值
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val mainTextSize = textSize.textDp
    val translationTextSize = mainTextSize * 0.8f

    val darkMode = isSystemInDarkTheme()
    val mainTextColor = if (darkMode) Color.White else Color.Black
    val baseTextColor = mainTextColor.copy(alpha = 0.3f)


    val textAlignment = when (textAlign) {
        LyricTextAlignment.Left -> TextAlign.Left
        LyricTextAlignment.Center -> TextAlign.Center
        LyricTextAlignment.Right -> TextAlign.Right
    }
    val fontWeight: FontWeight = if (textBold) {
        if (currentTimeMs >= 0) FontWeight.W800 else FontWeight.W600 // 对当前行使用更粗的字重
    } else {
        if (currentTimeMs >= 0) FontWeight.Bold else FontWeight.Normal
    }

    val mainTextStyle = remember(mainTextSize, fontWeight, textAlignment) {
        TextStyle(
            fontSize = mainTextSize,
            color = baseTextColor, // color 会在Canvas中被覆盖，这里不重要
            fontWeight = fontWeight,
            lineHeight = mainTextSize * 1.5f,
            textAlign = textAlignment
        )
    }

    val translationTextStyle =
        remember(translationTextSize, baseTextColor, fontWeight, textAlignment) {
            TextStyle(
                fontSize = translationTextSize,
                color = baseTextColor, // 同上
                fontWeight = fontWeight,
                lineHeight = translationTextSize * 1.5f,
                textAlign = textAlignment
            )
        }

    val paddingY = (textSize * 0.3f).dp
    val textMeasurer = rememberTextMeasurer()

    var mainTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var translationLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = paddingY)
        ) {
            val availableWidthPx = with(density) { maxWidthDp.toPx() }

            LaunchedEffect(line.lyric, mainTextStyle, textAlignment, maxWidthDp) {
                withContext(Dispatchers.Default) {
                    val mainLayoutResult = textMeasurer.measure(
                        text = line.lyric,
                        style = mainTextStyle,
                        constraints = Constraints(maxWidth = availableWidthPx.toInt())
                    )

                    if (line.words.isNotEmpty()) {
                        var currentPos = 0
                        line.wordMeasures = line.words.map { word ->
                            val startPosInStr = line.lyric.indexOf(word.text, currentPos)
                            if (startPosInStr == -1) {
                                return@map WordMeasure(word.text, 0f, 0f)
                            }
                            val endPosInStr = startPosInStr + word.text.length
                            currentPos = endPosInStr
                            val startOffset =
                                mainLayoutResult.getHorizontalPosition(startPosInStr, true)
                            val endOffset =
                                mainLayoutResult.getHorizontalPosition(endPosInStr, true)
                            WordMeasure(word.text, startOffset, endOffset)
                        }
                    }

                    val translationResult = line.translation?.let {
                        textMeasurer.measure(
                            text = it,
                            style = translationTextStyle,
                            constraints = Constraints(maxWidth = availableWidthPx.toInt())
                        )
                    }

                    mainTextLayoutResult = mainLayoutResult
                    translationLayoutResult = translationResult
                    line.textLayoutResult = mainLayoutResult
                    line.translationLayoutResult = translationResult
                }
            }

            if (mainTextLayoutResult == null) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((textSize * 1.8f).dp)
                )
                return@Box
            }

            val layoutResult = mainTextLayoutResult!!
            val canvasHeight = if (line.translation != null && translationLayoutResult != null) {
                with(density) {
                    (layoutResult.size.height + translationLayoutResult!!.size.height).toDp() + paddingY
                }
            } else {
                with(density) { layoutResult.size.height.toDp() }
            }

            // 【核心修改区域】
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
            ) {
                // 1. 绘制底层完整文字 (作为背景)，颜色总是暗色
                drawText(
                    textLayoutResult = layoutResult,
                    color = mainTextColor,
                    alpha = 0.3f // 固定为暗色
                )

                // 2. 如果是当前行，则在上方绘制高亮部分
                if (currentTimeMs >= 0) {

                    // 【新增逻辑】判断是否有逐字歌词
                    if (line.words.isNotEmpty()) {
                        // --- 情况A: 有逐字歌词，执行逐字高亮 ---
                        val sungState = calculateSungState(currentTimeMs, line, layoutResult)

                        if (sungState != null) {
                            val highlightPath = Path().apply {
                                // 添加所有被完全高亮的行
                                for (i in 0 until sungState.fullyHighlightedLines) {
                                    addRect(
                                        Rect(
                                            left = layoutResult.getLineLeft(i),
                                            top = layoutResult.getLineTop(i),
                                            right = layoutResult.getLineRight(i),
                                            bottom = layoutResult.getLineBottom(i)
                                        )
                                    )
                                }
                                // 添加部分高亮的活动行
                                val partialLineWidth = sungState.partialHighlightWidth
                                if (partialLineWidth > 0) {
                                    val lineIndex = sungState.fullyHighlightedLines
                                    if (lineIndex < layoutResult.lineCount) {
                                        val lineLeft = layoutResult.getLineLeft(lineIndex)
                                        val lineTop = layoutResult.getLineTop(lineIndex)
                                        val lineBottom = layoutResult.getLineBottom(lineIndex)
                                        addRect(
                                            Rect(
                                                left = lineLeft,
                                                top = lineTop,
                                                right = lineLeft + partialLineWidth,
                                                bottom = lineBottom
                                            )
                                        )
                                    }
                                }
                            }

                            // 使用 clipPath 将绘制区域限制在高亮路径内
                            clipPath(path = highlightPath) {
                                // 在裁剪区域内，再次绘制高亮文字
                                drawText(
                                    textLayoutResult = layoutResult,
                                    color = mainTextColor,
                                    alpha = 1.0f // 完全不透明
                                )
                            }
                        }
                    } else {
                        // --- 情况B: 没有逐字歌词，直接高亮整行 ---
                        // 简单地在暗色背景上再绘制一遍完整、不透明的亮色文字即可
                        drawText(
                            textLayoutResult = layoutResult,
                            color = mainTextColor,
                            alpha = 1.0f // 完全不透明，实现整行高亮
                        )
                    }
                }

                // 3. 绘制翻译 (始终是暗色)
                if (line.translation != null && translationLayoutResult != null) {
                    drawText(
                        textLayoutResult = translationLayoutResult!!,
                        topLeft = Offset(0f, layoutResult.size.height + paddingY.toPx()),
                        color = mainTextColor,
                        alpha = 0.3f
                    )
                }
            }

        }
    }
}

/**
 * 修复版本的计算函数，解决最后一个单词/汉字不会被高亮的问题。
 * 它通过在字符坐标之间进行线性插值，来同时解决多行换行和平滑过渡的问题。
 *
 * @return SungState 描述了有多少行被完全点亮，以及活动行的点亮宽度。
 */
private fun calculateSungState(
    currentTimeMs: Long,
    line: LyricLine,
    layoutResult: TextLayoutResult
): SungState? {
    if (line.words.isEmpty() || line.lyric.isEmpty()) return null

    val lineStart = line.startTimeMs
    val lineEnd = line.startTimeMs + line.durationMs

    // Case 1: 整行未开始
    if (currentTimeMs < lineStart) return SungState(0, 0f)

    // Case 2: 整行已结束 - 修复：应该高亮到最后一行的末尾
    if (currentTimeMs >= lineEnd) {
        val lastLine = layoutResult.lineCount - 1
        val lastLineRight = layoutResult.getLineRight(lastLine)
        val lastLineLeft = layoutResult.getLineLeft(lastLine)
        return SungState(lastLine, lastLineRight - lastLineLeft)
    }

    // --- 核心计算逻辑 ---

    var wordStartCharIndex = 0
    var activeWord: LyricWord? = null
    var progressInWord = 0f
    var allWordsFinished = true

    // 找到当前活动的单词和在其中的进度
    for (word in line.words) {
        val wordEnd = word.startTimeMs + word.durationMs
        if (currentTimeMs < word.startTimeMs) {
            // 时间还未到这个词，停在上个词的末尾
            allWordsFinished = false
            break
        }
        if (currentTimeMs < wordEnd) {
            // 时间正在这个词中间
            activeWord = word
            progressInWord =
                (currentTimeMs - word.startTimeMs).toFloat() / word.durationMs.coerceAtLeast(1)
            allWordsFinished = false
            break
        }
        // 如果这个词已经唱完，累加其字符长度以用于下次搜索
        val foundIndex = line.lyric.indexOf(word.text, wordStartCharIndex)
        wordStartCharIndex =
            if (foundIndex != -1) foundIndex + word.text.length else wordStartCharIndex + word.text.length
    }

    // 计算总的 "浮点字符索引"
    val totalCharOffset: Float
    if (activeWord != null) {
        val activeWordStart = line.lyric.indexOf(
            activeWord.text,
            (wordStartCharIndex - activeWord.text.length).coerceAtLeast(0)
        )
        if (activeWordStart != -1) {
            totalCharOffset = activeWordStart + (activeWord.text.length * progressInWord)
        } else {
            // 兜底：如果找不到词，就停在之前的位置
            totalCharOffset = wordStartCharIndex.toFloat()
        }
    } else if (allWordsFinished) {
        // 所有词都唱完了，但行还没结束 - 修复：应该高亮到文本末尾
        totalCharOffset = line.lyric.length.toFloat()
    } else {
        // 还没开始唱任何词
        totalCharOffset = 0f
    }

    // 修复：处理到达文本末尾的情况
    if (totalCharOffset >= line.lyric.length) {
        val lastLine = layoutResult.lineCount - 1
        val lastLineRight = layoutResult.getLineRight(lastLine)
        val lastLineLeft = layoutResult.getLineLeft(lastLine)
        return SungState(lastLine, lastLineRight - lastLineLeft)
    }

    // 根据浮点索引，进行插值计算
    val floorIndex = totalCharOffset.toInt().coerceIn(0, line.lyric.length - 1)
    val ceilIndex = (floorIndex + 1).coerceAtMost(line.lyric.length)
    val fraction = totalCharOffset - floorIndex

    val floorLine = layoutResult.getLineForOffset(floorIndex)
    val floorX = layoutResult.getHorizontalPosition(floorIndex, true)

    // 修复：如果已经到最后一个字符，需要考虑字符的宽度
    if (floorIndex >= line.lyric.length - 1) {
        // 获取最后一个字符的右边界位置
        val lastCharX = layoutResult.getHorizontalPosition(line.lyric.length, true)
        return SungState(floorLine, lastCharX - layoutResult.getLineLeft(floorLine))
    }

    val ceilLine = layoutResult.getLineForOffset(ceilIndex)
    val ceilX = layoutResult.getHorizontalPosition(ceilIndex, true)

    if (floorLine == ceilLine) {
        // Case 3: 平滑过渡在同一行内
        val interpolatedX = lerp(floorX, ceilX, fraction)
        return SungState(floorLine, interpolatedX - layoutResult.getLineLeft(floorLine))
    } else {
        // Case 4: 平滑过渡跨越了换行符
        // 在这种情况下，当前行（floorLine）被完全高亮
        return SungState(floorLine + 1, 0f)
    }
}
