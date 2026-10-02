package com.ljyh.mei.ui.component.player.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil3.compose.AsyncImage
import com.ljyh.mei.data.repository.DynamicCover
import com.ljyh.mei.playback.CacheManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Keeps the static image visible until a video frame is actually rendered. */
@OptIn(UnstableApi::class)
@Composable
fun DynamicCoverView(
    imageUrl: String,
    cover: DynamicCover?,
    playing: Boolean,
    onPlaybackError: (PlaybackException) -> Unit = {},
    onFrameSample: ((Bitmap) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var firstFrame by remember(cover?.cacheKey, cover?.url) { mutableStateOf(false) }
    var failed by remember(cover?.cacheKey, cover?.url) { mutableStateOf(false) }
    var videoDimensions by remember(cover?.cacheKey, cover?.url) {
        mutableStateOf(Triple(0, 0, 1f))
    }
    var textureView by remember(cover?.cacheKey, cover?.url) {
        mutableStateOf<CenterCropTextureView?>(null)
    }
    val currentFrameSample by rememberUpdatedState(onFrameSample)
    val player = remember(cover?.cacheKey, cover?.url) {
        cover?.let {
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(CacheManager.getCacheDataSourceFactory(context)))
                .build().apply {
                    volume = 0f
                    repeatMode = Player.REPEAT_MODE_ONE
                    val item = MediaItem.Builder().setUri(it.url).apply {
                        if (it.url.contains(".m3u8", ignoreCase = true)) {
                            setMimeType(MimeTypes.APPLICATION_M3U8)
                        } else {
                            setCustomCacheKey(it.cacheKey)
                        }
                    }.build()
                    setMediaItem(item)
                    prepare()
                }
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrame = true }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoDimensions = Triple(videoSize.width, videoSize.height, videoSize.pixelWidthHeightRatio)
            }
            override fun onPlayerError(error: PlaybackException) {
                failed = true
                onPlaybackError(error)
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
            player?.release()
        }
    }
    LaunchedEffect(player, playing, failed) {
        player?.playWhenReady = playing && !failed
    }
    LaunchedEffect(player, textureView, playing, firstFrame, failed, onFrameSample != null) {
        val view = textureView
        if (player != null && view != null && playing && firstFrame && !failed && onFrameSample != null) {
            while (isActive) {
                // Collapse the last three rows into one horizontal color strip. This keeps the
                // video's edge colors without projecting unrelated shapes into the controls.
                if (view.isAvailable) {
                    runCatching {
                        view.getBitmap(16, 16)?.let { snapshot ->
                            val pixels = IntArray(16 * 3)
                            snapshot.getPixels(pixels, 0, 16, 0, 13, 16, 3)
                            snapshot.recycle()
                            val edge = IntArray(16) { x ->
                                val a = pixels[x]
                                val b = pixels[16 + x]
                                val c = pixels[32 + x]
                                android.graphics.Color.rgb(
                                    (android.graphics.Color.red(a) + android.graphics.Color.red(b) + android.graphics.Color.red(c)) / 3,
                                    (android.graphics.Color.green(a) + android.graphics.Color.green(b) + android.graphics.Color.green(c)) / 3,
                                    (android.graphics.Color.blue(a) + android.graphics.Color.blue(b) + android.graphics.Color.blue(c)) / 3
                                )
                            }
                            Bitmap.createBitmap(edge, 16, 1, Bitmap.Config.ARGB_8888)
                        }
                    }.getOrNull()?.let { currentFrameSample?.invoke(it) }
                }
                delay(33)
            }
        }
    }
    val background = remember(cover?.palette?.bgColor) {
        runCatching { Color(android.graphics.Color.parseColor("#${cover?.palette?.bgColor}")) }
            .getOrDefault(Color.Transparent)
    }
    Box(modifier.background(background)) {
        AsyncImage(
            model = cover?.previewUrl ?: imageUrl,
            contentDescription = "专辑封面",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (player != null && !failed && cover != null) {
            // A TextureView must belong to exactly one cover. Reusing it across rapid song changes
            // can briefly expose the previous video's frame and its crop transform.
            key(cover.cacheKey, cover.url) {
                AndroidView(
                    factory = { viewContext ->
                        CenterCropTextureView(viewContext).also {
                            textureView = it
                            player.setVideoTextureView(it)
                        }
                    },
                    update = { view ->
                        view.setVideoDimensions(videoDimensions.first, videoDimensions.second, videoDimensions.third)
                    },
                    modifier = Modifier.fillMaxSize().alpha(
                        if (firstFrame && videoDimensions.first > 0 && videoDimensions.second > 0) 1f else 0f
                    )
                )
            }
        }
    }
}

private class CenterCropTextureView(context: Context) : TextureView(context) {
    private var videoWidth = 0
    private var videoHeight = 0
    private var pixelRatio = 1f

    fun setVideoDimensions(width: Int, height: Int, ratio: Float) {
        videoWidth = width
        videoHeight = height
        pixelRatio = ratio
        updateTransform()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateTransform()
    }

    private fun updateTransform() {
        if (width <= 0 || height <= 0 || videoWidth <= 0 || videoHeight <= 0 || pixelRatio <= 0f) {
            setTransform(Matrix())
            return
        }
        val videoRatio = videoWidth * pixelRatio / videoHeight
        val viewRatio = width.toFloat() / height
        val scaleX = if (videoRatio > viewRatio) videoRatio / viewRatio else 1f
        val scaleY = if (videoRatio < viewRatio) viewRatio / videoRatio else 1f
        setTransform(Matrix().apply { setScale(scaleX, scaleY, width / 2f, height / 2f) })
    }
}
