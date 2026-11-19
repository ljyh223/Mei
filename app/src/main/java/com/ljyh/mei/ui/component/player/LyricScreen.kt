package com.ljyh.mei.ui.component.player

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.R
import com.ljyh.mei.constants.SeekOnLyricTapKey
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.playback.SmoothPlaybackPosition
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.utils.preferences.rememberPreference
import com.ljyh.mei.utils.system.setClipboard
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.list.rememberLyricsLazyListState
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.LyricsAnchor
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.LyricsFade
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun LyricScreen(
    lyricData: LyricData,
    modifier: Modifier = Modifier,
    playerConnection: PlayerConnection,
    onClick: (LyricSource) -> Unit,
    onLongClick: (LyricSource) -> Unit,
    onStyleClick: () -> Unit,
    controlsVisible: Boolean,
    onToggleControls: (Boolean) -> Unit,
    anchor: LyricsAnchor = LyricsAnchor.Fixed(40.dp),
    bottomFade: LyricsFade = LyricsFade.Fraction(0.5f),
    lyricsEndPadding: Dp = 0.dp,
    lyricsVerticalPadding: Dp = 8.dp,
    onBackToControls: (() -> Unit)? = null,
    styleAtEnd: Boolean = false,
) {
    val context = LocalContext.current
    val playbackPosition = remember(playerConnection.player) {
        mutableIntStateOf(playerConnection.player.currentPosition.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())
    }
    val playbackClock = remember(playerConnection.player) { SmoothPlaybackPosition() }
    val currentPosition = remember(playbackPosition) { { playbackPosition.intValue } }
    DisposableEffect(playerConnection.player, playbackClock) {
        val player = playerConnection.player
        val listener = object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                playbackClock.reset()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    val initialLineIndex = remember(lyricData.lyricLine) {
        val position = playerConnection.player.currentPosition
        lyricData.lyricLine.lines.indexOfLast { it.start.toLong() <= position }.coerceAtLeast(0)
    }
    val listState = key(lyricData.lyricLine) {
        rememberLyricsLazyListState(initialFirstVisibleItemIndex = initialLineIndex)
    }
    var positioningInitialLine by remember(lyricData.lyricLine) { mutableStateOf(true) }
    LaunchedEffect(listState, lyricData.lyricLine) {
        // The lyric view is recreated when cover mode ends. Snap to the current line before
        // enabling the normal follow animation so it does not travel down from line zero.
        if (lyricData.lyricLine.lines.isNotEmpty()) listState.scrollToItem(initialLineIndex)
        positioningInitialLine = false
    }
    val style = rememberPlayerLyricStyle()
    val (seekOnLyricTap, _) = rememberPreference(SeekOnLyricTapKey, true)

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000.milliseconds)
            onToggleControls(false)
        }
    }


    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    if (delta < -10) {
                        onToggleControls(false)
                    } else if (delta > 10) {
                        onToggleControls(true)
                    }
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(playerConnection.player) {
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                val duration = playerConnection.player.duration
                    .takeIf { it > 0L }
                    ?.coerceAtMost(Int.MAX_VALUE.toLong())
                    ?: Int.MAX_VALUE.toLong()
                val player = playerConnection.player
                playbackPosition.intValue = playbackClock.sample(
                    rawPositionMs = player.currentPosition,
                    frameTimeNanos = frameTimeNanos,
                    isPlaying = player.isPlaying,
                    speed = player.playbackParameters.speed,
                    durationMs = duration,
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggleControls(true) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (lyricData.lyricLine.lines.isNotEmpty()) {
                key(System.identityHashCode(lyricData.lyricLine)) {
                    KaraokeLyricsView(
                        anchor = anchor,
                        bottomFade = bottomFade,
                        listState = listState,
                        lyrics = lyricData.lyricLine,
                        currentPosition = currentPosition,
                        onLineClicked = { line ->
                            if (seekOnLyricTap) {
                                playbackClock.reset()
                                playbackPosition.intValue = line.start
                                playerConnection.player.seekTo(line.start.toLong())
                            }
                            onToggleControls(true)
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
                                    Toast.makeText(context, "未知的歌词类型", Toast.LENGTH_SHORT)
                                        .show()
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
                            .padding(vertical = lyricsVerticalPadding)
                            .padding(end = lyricsEndPadding)
                            .graphicsLayer {
                                blendMode = BlendMode.Plus
                                compositingStrategy = CompositingStrategy.Offscreen
                            },
                        normalLineTextStyle = style.main,
                        translationTextStyle = style.translation,
                        accompanimentLineTextStyle = style.translation,
                        phoneticTextStyle = style.phonetic,
                        showPhonetic = style.showPhonetic,
                        showTranslation = style.showTranslation,
                        scrollAnimationSpec = if (positioningInitialLine) snap() else
                            tween(650, easing = FastOutSlowInEasing),
                    )
                }

            }
            LyricPanelActions(
                source = lyricData.source.takeIf { lyricData.lyricLine.lines.isNotEmpty() },
                onClick = onClick,
                onLongClick = onLongClick,
                onBackToControls = onBackToControls,
                onStyleClick = onStyleClick.takeIf { styleAtEnd },
                modifier = Modifier.align(Alignment.BottomEnd).padding(start = 8.dp),
            )
            if (!styleAtEnd) Box(Modifier.align(Alignment.BottomStart)) {
                LyricStyleAction(onStyleClick)
            }
        }
    }
}

@Composable
internal fun LyricPanelActions(
    source: LyricSource?,
    onClick: (LyricSource) -> Unit,
    onLongClick: (LyricSource) -> Unit,
    onBackToControls: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onStyleClick: (() -> Unit)? = null,
) {
    if (source == null && onBackToControls == null && onStyleClick == null) return
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (onStyleClick != null) LyricStyleAction(onStyleClick)
        if (onBackToControls != null) {
            LyricActionBadge(
                painter = rememberVectorPainter(Icons.Rounded.SwapHoriz),
                description = "切换到播放控制",
                onClick = onBackToControls,
            )
        }
        if (source != null) {
            val icon = when (source) {
                LyricSource.Empty, LyricSource.Loading -> R.drawable.empty
                LyricSource.NetEaseCloudMusic -> R.drawable.netease
                LyricSource.QQMusic -> R.drawable.qq
                LyricSource.AM -> R.drawable.am
            }
            LyricActionBadge(
                painter = painterResource(icon),
                description = "选择歌词版本",
                onClick = { onClick(source) },
                onLongClick = { onLongClick(source) },
            )
        }
    }
}

@Composable
private fun LyricStyleAction(onClick: () -> Unit) {
    LyricActionBadge(
        painter = rememberVectorPainter(Icons.Rounded.Tune),
        description = "调整歌词样式",
        onClick = onClick,
    )
}

@Composable
private fun LyricActionBadge(
    painter: Painter,
    description: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val foreground = LocalPlayerForegroundColor.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(foreground.copy(alpha = 0.2f))
            .border(0.5.dp, foreground.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick),
    ) {
        Icon(
            painter = painter,
            modifier = Modifier.size(16.dp),
            contentDescription = description,
            tint = foreground.copy(alpha = 0.6f),
        )
    }
}
