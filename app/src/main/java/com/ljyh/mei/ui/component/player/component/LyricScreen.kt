package com.ljyh.mei.ui.component.player.component


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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ljyh.mei.R
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import kotlinx.coroutines.android.awaitFrame

@Composable
fun LyricScreen(
    lyricData: LyricData,
    modifier: Modifier = Modifier,
    playerConnection: PlayerConnection,
) {
    val listState = rememberLazyListState()
    var animatedPosition by remember { mutableLongStateOf(0L) }

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
        // 歌词内容区域
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
                    onLinePressed = {

                    },
                    modifier = Modifier
                        .padding(PaddingValues(bottom = 48.dp, top = 16.dp))
                        .graphicsLayer {
                            blendMode = BlendMode.Plus
                            compositingStrategy = CompositingStrategy.Offscreen
                        },
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
