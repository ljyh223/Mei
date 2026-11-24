package com.ljyh.mei.ui.screen.artist.component

import android.animation.ValueAnimator
import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ljyh.mei.ui.component.player.cover.createArtworkVideoPlayer
import kotlinx.coroutines.delay

/** Page-local decorative playback. Static artwork remains underneath until a real frame arrives. */
@OptIn(UnstableApi::class)
@Composable
internal fun ArtistMotionBackdrop(
    url: String,
    artistId: String,
    scrollPixels: () -> Float,
    onReadyChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    playerFactory: (Context, String, String) -> ExoPlayer? = { context, video, key ->
        createArtworkVideoPlayer(context, video, key)
    },
) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val density = LocalDensity.current.density
    val visible by remember(scrollPixels, density) { derivedStateOf { scrollPixels() / density < 380f } }
    var motionAllowed by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    LaunchedEffect(lifecycle) { motionAllowed = ValueAnimator.areAnimatorsEnabled() }
    val playing = lifecycle.isAtLeast(Lifecycle.State.RESUMED) && visible && motionAllowed
    var started by remember(url) { mutableStateOf(false) }
    var failed by remember(url) { mutableStateOf(false) }
    LaunchedEffect(playing) { if (playing) started = true }
    val report by rememberUpdatedState(onReadyChanged)
    if (started && !failed) {
        key(url) {
            ActiveArtistMotionBackdrop(url, "apple-artist:$artistId:$url", playing, scrollPixels,
                onReadyChanged = { report(it) }, onFailure = { failed = true }, modifier, playerFactory)
        }
    } else LaunchedEffect(url, failed) { report(false) }
}

@OptIn(UnstableApi::class)
@Composable
private fun ActiveArtistMotionBackdrop(
    url: String,
    cacheKey: String,
    playing: Boolean,
    scrollPixels: () -> Float,
    onReadyChanged: (Boolean) -> Unit,
    onFailure: () -> Unit,
    modifier: Modifier,
    playerFactory: (Context, String, String) -> ExoPlayer?,
) {
    val context = LocalContext.current
    val player = remember(url) { runCatching { playerFactory(context, url, cacheKey) }.getOrNull() }
    var firstFrame by remember(url) { mutableStateOf(false) }
    var dimensions by remember(url) { mutableStateOf(Triple(0, 0, 1f)) }
    val report by rememberUpdatedState(onReadyChanged)
    val fail by rememberUpdatedState(onFailure)
    val ready = firstFrame && dimensions.first > 0 && dimensions.second > 0
    LaunchedEffect(ready) { report(ready) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrame = true }
            override fun onSurfaceSizeChanged(width: Int, height: Int) {
                // A detached/recreated texture no longer contains the previously reported frame.
                if (width <= 0 || height <= 0) firstFrame = false
            }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                dimensions = Triple(videoSize.width, videoSize.height, videoSize.pixelWidthHeightRatio)
            }
            override fun onPlayerError(error: PlaybackException) { fail() }
        }
        if (player == null) fail() else {
            player.addListener(listener)
            runCatching { player.prepare() }.onFailure { fail() }
        }
        onDispose {
            player?.removeListener(listener)
            player?.clearVideoSurface()
            player?.release()
        }
    }
    LaunchedEffect(player, playing) { player?.playWhenReady = playing }
    // A stalled manifest or unsupported stream must not leave an invisible decoder running.
    LaunchedEffect(playing, ready) {
        if (playing && !ready) { delay(12_000); fail() }
    }
    if (player != null) AndroidView(
        factory = { viewContext -> ArtistVideoBackdropView(viewContext).also { player.setVideoTextureView(it.texture) } },
        update = {
            it.texture.setVideoDimensions(dimensions.first, dimensions.second, dimensions.third, false)
            it.updatePresentation(ready, scrollPixels())
        },
        modifier = modifier,
    )
}
