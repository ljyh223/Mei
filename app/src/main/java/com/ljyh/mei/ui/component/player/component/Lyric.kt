package com.ljyh.mei.ui.component.player.component


import android.util.Log
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ljyh.mei.R
import com.ljyh.mei.constants.LyricTextAlignment
import com.ljyh.mei.constants.LyricTextAlignmentKey
import com.ljyh.mei.constants.LyricTextBoldKey
import com.ljyh.mei.constants.LyricTextSize
import com.ljyh.mei.constants.LyricTextSizeKey
import com.ljyh.mei.constants.PlayerHorizontalPadding
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.utils.UnitUtils.dp2px
import com.ljyh.mei.utils.rememberEnumPreference
import com.ljyh.mei.utils.rememberPreference
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import kotlinx.coroutines.android.awaitFrame

@Composable
fun LyricScreen(
    lyricData: LyricData,
    playerConnection: PlayerConnection,
    switchLyric: () -> Unit
) {
    val listState = rememberLazyListState()
    var animatedPosition by remember { mutableLongStateOf(0L) }


    LaunchedEffect(playerConnection.isPlaying) {
        if (playerConnection.isPlaying.value) {
            while (true) {
                val elapsed = System.currentTimeMillis() - playerConnection.player.currentPosition
                animatedPosition = (playerConnection.player.currentPosition ).coerceAtMost(
                    playerConnection.player.duration
                )
                awaitFrame()
            }
        } else {
            animatedPosition = playerConnection.player.currentPosition
        }
    }
    Log.d("LyricScreen", "LyricScreen: ${lyricData.lyricLine.lines.size}")
    Log.d("LyricScreen", "LyricSource: ${lyricData.source}")

    if(lyricData.lyricLine.lines.isNotEmpty()){
        KaraokeLyricsView(
            listState = listState,
            lyrics = lyricData.lyricLine,
            currentPosition = animatedPosition,
            onLineClicked = { line ->
                playerConnection.player.seekTo(line.start.toLong())
            },
            onLinePressed = { line ->

            },
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .graphicsLayer {
                    blendMode = BlendMode.Plus
                    compositingStrategy = CompositingStrategy.Offscreen
                },
        )
    }

    BoxWithConstraints {
        val maxWidth = maxWidth



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
    }


}
