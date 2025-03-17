package com.ljyh.music.ui.component.player.component


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ljyh.music.R
import com.ljyh.music.constants.LyricTextAlignment
import com.ljyh.music.constants.LyricTextAlignmentKey
import com.ljyh.music.constants.LyricTextBoldKey
import com.ljyh.music.constants.LyricTextSize
import com.ljyh.music.constants.LyricTextSizeKey
import com.ljyh.music.constants.PlayerHorizontalPadding
import com.ljyh.music.playback.PlayerConnection
import com.ljyh.music.utils.UnitUtils.dp2px
import com.ljyh.music.utils.rememberEnumPreference
import com.ljyh.music.utils.rememberPreference

@Composable
fun LyricScreen(
    lyricData: LyricData,
    playerConnection: PlayerConnection,
    position: Long,
    switchLyric: () -> Unit
) {
    val state = rememberLazyListState()
    val isUserScrolling by remember { derivedStateOf { state.isScrollInProgress } }
    val currentTextElementHeightPx = remember { mutableIntStateOf(0) }
    val lyricTextAlignment by rememberEnumPreference(
        LyricTextAlignmentKey,
        defaultValue = LyricTextAlignment.Left
    )
    val lyricTextSize by rememberEnumPreference(
        LyricTextSizeKey,
        defaultValue = LyricTextSize.Size24
    )
    val lyricTextBold by rememberPreference(LyricTextBoldKey, defaultValue = true)
    val currentLine = remember(position) {
        lyricData.lyricLine.lastOrNull { line ->
            line.words.any { word ->
                position >= word.startTimeMs &&
                        position <= (word.startTimeMs + word.durationMs)
            } || position >= line.startTimeMs
        }
    }

    val lyricStyleKey = remember(lyricTextSize, lyricTextAlignment, lyricTextBold) {
        "${lyricTextSize}_${lyricTextAlignment}_${lyricTextBold}"
    }


    val currentIndex = remember(currentLine) {
        lyricData.lyricLine.indexOfFirst { it == currentLine }
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = PlayerHorizontalPadding)
    ) {
        val blackItem: (LazyListScope.() -> Unit) = {
            item {
                Box(
                    modifier = Modifier.height(maxHeight / 2)
                ) {}
            }
        }
        val parentWidthDp = maxWidth

        LazyColumn(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(0.dp, 0.dp)
                .graphicsLayer { alpha = 0.99F }
                .drawWithContent {
                    val colors = listOf(
                        Color.Transparent, Color.Black, Color.Black, Color.Black, Color.Black,
                        Color.Black, Color.Black, Color.Black, Color.Transparent
                    )
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(colors),
                        blendMode = BlendMode.DstIn
                    )
                },
            state = state
        ) {
            blackItem()
            items(
                lyricData.lyricLine,
                key = { lyric -> lyric.startTimeMs.toString() + lyric.lyric + lyricStyleKey }) { lyric ->
                val isActiveLine = lyric == currentLine
                LyricLineBate(
                    line = lyric,
                    textSize = when (lyricTextSize) {
                        LyricTextSize.Size18 -> 18
                        LyricTextSize.Size20 -> 20
                        LyricTextSize.Size22 -> 22
                        LyricTextSize.Size24 -> 24
                        LyricTextSize.Size26 -> 26
                        LyricTextSize.Size28 -> 28
                        LyricTextSize.Size30 -> 30
                    },
                    textBold = lyricTextBold,
                    textAlign = lyricTextAlignment,
                    parentWidthDp = parentWidthDp,
                    currentTimeMs = if (isActiveLine) position else -1,
                ) {
                    playerConnection.player.seekTo(lyric.startTimeMs)
                }

            }
            blackItem()
        }
        Icon(
            painter = painterResource(
                when (lyricData.source) {
                    LyricSource.Empty -> R.drawable.cloud
                    LyricSource.NetEaseCloudMusic -> R.drawable.cloud
                    LyricSource.QQMusic -> R.drawable.qq
                }
            ),
            contentDescription = "source",
            modifier = Modifier
                .padding(0.dp, 0.dp)
                .align(Alignment.BottomEnd)
                .size(16.dp)
                .clickable { switchLyric() }
        )



        LaunchedEffect(key1 = position, key2 = currentTextElementHeightPx, key3 = lyricStyleKey) {
            if (!isUserScrolling) { // 只有当用户没有手动滚动时才自动滚动
                val height = (dp2px(maxHeight.value) - currentTextElementHeightPx.intValue) / 2
                state.animateScrollToItem((currentIndex + 1).coerceAtLeast(0), -height.toInt())
            }
        }
    }
}
