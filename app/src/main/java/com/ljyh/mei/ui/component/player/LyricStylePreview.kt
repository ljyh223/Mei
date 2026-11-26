package com.ljyh.mei.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.composable.list.rememberLyricsLazyListState
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.LyricsAnchor
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.LyricsFade
import kotlinx.coroutines.isActive

private const val PreviewLineDurationMs = 4_000
private val PreviewLyrics = SyncedLyrics(
    lines = listOf(
        Triple("そんな気がした", "流露出如此情感", "so n na ki ga shi ta"),
        Triple("なんでだろう？", "究竟是为何？", "na n de da ro u?"),
        Triple("ただただ不思議で", "为此感到不可思议", "ta da ta da fu shi gi de"),
    ).mapIndexed { index, (text, translation, phonetic) ->
        val start = index * PreviewLineDurationMs
        KaraokeLine.MainKaraokeLine(
            syllables = text.mapIndexed { character, content ->
                KaraokeSyllable(
                    content = content.toString(),
                    start = start + character * PreviewLineDurationMs / text.length,
                    end = start + (character + 1) * PreviewLineDurationMs / text.length,
                )
            },
            translation = translation,
            phonetic = phonetic,
            alignment = KaraokeAlignment.Start,
            start = start,
            end = start + PreviewLineDurationMs,
        )
    },
)

/** An independent clock exercises the player's renderer without seeking or starting audio. */
@Composable
internal fun LyricStylePreview(modifier: Modifier = Modifier) {
    val style = rememberPlayerLyricStyle()
    val position = rememberSaveable { mutableIntStateOf(600) }
    var playing by rememberSaveable { mutableStateOf(true) }
    val currentPosition = remember { { position.intValue } }
    val listState = rememberLyricsLazyListState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(playing, lifecycle) {
        if (!playing) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var previousFrame = 0L
            while (isActive) withFrameNanos { frame ->
                if (previousFrame != 0L) {
                    position.intValue = (position.intValue + ((frame - previousFrame) / 1_000_000).toInt()) %
                        (PreviewLyrics.lines.size * PreviewLineDurationMs)
                }
                previousFrame = frame
            }
        }
    }
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .background(Brush.linearGradient(listOf(Color(0xFF29434B), Color(0xFF18202B)))),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp)) {
            Text("样式预览", color = Color.White, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).padding(vertical = 16.dp))
            IconButton(onClick = { playing = !playing }) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "暂停样式预览" else "播放样式预览", tint = Color.White)
            }
        }
        Box(Modifier.fillMaxWidth().height(200.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))) {
            KaraokeLyricsView(
                listState = listState,
                lyrics = PreviewLyrics,
                currentPosition = currentPosition,
                onLineClicked = { position.intValue = it.start },
                onLinePressed = {},
                modifier = Modifier.fillMaxWidth(),
                normalLineTextStyle = style.main.copy(color = Color.White),
                translationTextStyle = style.translation.copy(color = Color.White),
                accompanimentLineTextStyle = style.translation.copy(color = Color.White),
                phoneticTextStyle = style.phonetic.copy(color = Color.White),
                showTranslation = style.showTranslation,
                showPhonetic = style.showPhonetic,
                anchor = LyricsAnchor.Fixed(16.dp),
                topFade = LyricsFade.Fixed(8.dp),
                bottomFade = LyricsFade.Fraction(0.15f),
            )
        }
    }
}
