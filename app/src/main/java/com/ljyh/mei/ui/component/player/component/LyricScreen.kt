package com.ljyh.mei.ui.component.player.component


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljyh.mei.R
import com.ljyh.mei.constants.AccompanimentLyricTextBoldKey
import com.ljyh.mei.constants.AccompanimentLyricTextSizeKey
import com.ljyh.mei.constants.LyricTextSize
import com.ljyh.mei.constants.NormalLyricTextBoldKey
import com.ljyh.mei.constants.NormalLyricTextSizeKey
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.utils.rememberEnumPreference
import com.ljyh.mei.utils.rememberPreference
import com.ljyh.mei.utils.setClipboard
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import com.mocharealm.accompanist.lyrics.ui.theme.SFPro
import kotlinx.coroutines.android.awaitFrame

@Composable
fun LyricScreen(
    lyricData: LyricData,
    modifier: Modifier = Modifier,
    playerConnection: PlayerConnection,
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var animatedPosition by remember { mutableLongStateOf(0L) }
    val (normalLyricTextSize, _) = rememberEnumPreference(
        NormalLyricTextSizeKey,
        LyricTextSize.Size32
    )
    val (normalLyricTextBold, _) = rememberPreference(NormalLyricTextBoldKey, true)

    val (accompanimentLyricTextSize, _) = rememberEnumPreference(
        AccompanimentLyricTextSizeKey,
        LyricTextSize.Size20
    )
    val (accompanimentLyricTextBold, _) = rememberPreference(AccompanimentLyricTextBoldKey, true)

    LaunchedEffect(playerConnection.isPlaying) {
        if (playerConnection.isPlaying.value) {
            while (true) {
                animatedPosition = (playerConnection.player.currentPosition).coerceAtMost(
                    playerConnection.player.duration
                )
                awaitFrame()
            }
        } else {
            animatedPosition = playerConnection.player.currentPosition
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (lyricData.lyricLine.lines.isNotEmpty()) {
                KaraokeLyricsView(
                    listState = listState,
                    lyrics = lyricData.lyricLine,
                    currentPosition = animatedPosition,
                    onLineClicked = { line ->
                        playerConnection.player.seekTo(line.start.toLong())
                    },
                    onLinePressed = { line ->
                        val result = when (line) {
                            is KaraokeLine -> {
                                "${line.syllables.joinToString("") { it.content }}\n${line.translation}"
                            }
                            is SyncedLine -> {
                                "${line.content}\n${line.translation}"
                            }
                            else -> {
                                Toast.makeText(context, "未知的歌词类型", Toast.LENGTH_SHORT).show()
                                null
                            }
                        }

                        result?.let {
                            try {
                                setClipboard(context, it, "lyric")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "复制失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(PaddingValues(bottom = 48.dp, top = 16.dp))
                        .graphicsLayer {
                            blendMode = BlendMode.Plus
                            compositingStrategy = CompositingStrategy.Offscreen
                        },
                    normalLineTextStyle = LocalTextStyle.current.copy(
                        fontSize = normalLyricTextSize.text.sp,
                        fontWeight = if (normalLyricTextBold) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = SFPro(),
                        textMotion = TextMotion.Animated,
                    ),
                    accompanimentLineTextStyle = LocalTextStyle.current.copy(
                        fontSize = accompanimentLyricTextSize.text.sp,
                        fontWeight = if (accompanimentLyricTextBold) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = SFPro(),
                        textMotion = TextMotion.Animated,
                    )
                )

                LyricSourceBadge(
                    source = lyricData.source,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                )
            }
        }
    }
}

@Composable
private fun LyricSourceBadge(
    source: LyricSource,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            painter = painterResource(
                when (source) {
                    LyricSource.Empty -> R.drawable.empty
                    LyricSource.NetEaseCloudMusic -> R.drawable.netease
                    LyricSource.QQMusic -> R.drawable.qq
                    LyricSource.AM -> R.drawable.am
                }
            ),
            modifier = Modifier.size(16.dp),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f)
        )
    }
}
