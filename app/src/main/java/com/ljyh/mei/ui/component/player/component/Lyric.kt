package com.ljyh.mei.ui.component.player.component


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.ljyh.mei.playback.PlayerConnection
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import kotlinx.coroutines.android.awaitFrame

@Composable
fun LyricScreen(
    lyricData: LyricData,
    playerConnection: PlayerConnection,
) {
    val listState = rememberLazyListState()
    var animatedPosition by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerConnection.isPlaying) {
        if (playerConnection.isPlaying.value) {
            while (true) {
                // val elapsed = System.currentTimeMillis() - playerConnection.player.currentPosition
                animatedPosition = (playerConnection.player.currentPosition ).coerceAtMost(
                    playerConnection.player.duration
                )
                awaitFrame()
            }
        } else {
            animatedPosition = playerConnection.player.currentPosition
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {

        // 歌词内容区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if(lyricData.lyricLine.lines.isNotEmpty()){
                KaraokeLyricsView(
                    listState = listState,
                    lyrics = lyricData.lyricLine,
                    currentPosition = animatedPosition,
                    onLineClicked = { line ->
                        playerConnection.player.seekTo(line.start.toLong())
                    },
                    onLinePressed = { line ->
                        // 按下
                    },
                    modifier = Modifier
                        .graphicsLayer {
                            blendMode = BlendMode.Plus
                            compositingStrategy = CompositingStrategy.Offscreen
                        },
                )
            }

        }
    }
}
