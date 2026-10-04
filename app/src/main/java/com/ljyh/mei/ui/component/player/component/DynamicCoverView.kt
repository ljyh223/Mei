package com.ljyh.mei.ui.component.player.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.widget.FrameLayout
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.graphics.toArgb
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
    bottomFadeColor: Color? = null,
    fitVideoWidth: Boolean = false,
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
        if (player != null && view != null && firstFrame && !failed && onFrameSample != null) {
            do {
                if (view.isAvailable) {
                    runCatching {
                        // A small frame supplies both the edge color and the soft transition.
                        view.getBitmap(32, 48)
                    }.getOrNull()?.let { currentFrameSample?.invoke(it) }
                }
                if (!playing) break
                delay(66)
            } while (isActive)
        }
    }
    val background = remember(cover?.palette?.bgColor) {
        runCatching { Color(android.graphics.Color.parseColor("#${cover?.palette?.bgColor}")) }
            .getOrDefault(Color.Transparent)
    }
    val showVideo = player != null && !failed && firstFrame &&
        videoDimensions.first > 0 && videoDimensions.second > 0
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
                        FrameLayout(viewContext).also { frame ->
                            CenterCropTextureView(viewContext).also {
                                textureView = it
                                player.setVideoTextureView(it)
                                frame.addView(it, FrameLayout.LayoutParams(-1, -1))
                            }
                            frame.addView(BottomFadeView(viewContext), FrameLayout.LayoutParams(-1, -1))
                        }
                    },
                    update = { frame ->
                        (frame.getChildAt(0) as CenterCropTextureView).setVideoDimensions(
                            videoDimensions.first, videoDimensions.second, videoDimensions.third,
                            fitVideoWidth,
                        )
                        (frame.getChildAt(1) as BottomFadeView).fadeColor = bottomFadeColor?.toArgb()
                    },
                    modifier = Modifier.fillMaxSize().alpha(if (showVideo) 1f else 0f)
                )
            }
        }
        if (bottomFadeColor != null && !showVideo) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0.64f to Color.Transparent,
                        0.72f to bottomFadeColor.copy(alpha = 0.02f),
                        0.82f to bottomFadeColor.copy(alpha = 0.16f),
                        0.92f to bottomFadeColor.copy(alpha = 0.55f),
                        1f to bottomFadeColor,
                    )
                )
            )
        }
    }
}

/** Draws above TextureView in the same native view hierarchy, so its bottom edge cannot bypass the fade. */
private class BottomFadeView(context: Context) : View(context) {
    var fadeColor: Int? = null
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val color = fadeColor ?: return
        val rgb = color and 0x00ffffff
        paint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                rgb,
                rgb,
                rgb or (0x05 shl 24),
                rgb or (0x29 shl 24),
                rgb or (0x8c shl 24),
                color,
            ),
            floatArrayOf(0f, 0.64f, 0.72f, 0.82f, 0.92f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, height * 0.64f, width.toFloat(), height.toFloat(), paint)
    }
}

private class CenterCropTextureView(context: Context) : TextureView(context) {
    private var videoWidth = 0
    private var videoHeight = 0
    private var pixelRatio = 1f
    private var fitWidth = false

    fun setVideoDimensions(width: Int, height: Int, ratio: Float, fitWidth: Boolean) {
        videoWidth = width
        videoHeight = height
        pixelRatio = ratio
        this.fitWidth = fitWidth
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
        if (fitWidth) {
            // TextureView initially stretches the decoded frame to its bounds. Correct only its
            // height, anchored at the top, so the video keeps its aspect ratio and both sides.
            setTransform(Matrix().apply {
                setScale(1f, viewRatio / videoRatio, width / 2f, 0f)
            })
            return
        }
        val scaleX = if (videoRatio > viewRatio) videoRatio / viewRatio else 1f
        val scaleY = if (videoRatio < viewRatio) viewRatio / videoRatio else 1f
        setTransform(Matrix().apply { setScale(scaleX, scaleY, width / 2f, height / 2f) })
    }
}
