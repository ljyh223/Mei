package com.ljyh.mei.ui.component.player.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.widget.FrameLayout
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
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.ljyh.mei.data.repository.DynamicCover
import com.ljyh.mei.playback.CacheManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

private const val MotionFadeEdgeStart = 0.82f
private const val MotionFadeCenterDrop = 0.09f
private const val MotionFadeWidth = 0.06f
private const val MotionBlurLead = 0.05f
private const val MotionBlurRadiusDp = 22f
private const val MotionSolidFloorBelowCurveDp = 10f
private const val MotionMaskWidth = 192
private const val MotionMaskHeight = 384

private fun curvedMotionFadeStart(x: Float): Float {
    val centeredX = x.coerceIn(0f, 1f) * 2f - 1f
    return MotionFadeEdgeStart + MotionFadeCenterDrop * (1f - centeredX * centeredX)
}

/** The fade starts lower at the center, following the curved transition in the artwork. */
internal fun curvedMotionFadeAlpha(x: Float, y: Float): Float {
    val progress = ((y - curvedMotionFadeStart(x)) / MotionFadeWidth).coerceIn(0f, 1f)
    return progress * progress * (3f - 2f * progress)
}

/** Fade the blurred video in before the color transition begins. */
internal fun curvedMotionBlurAlpha(x: Float, y: Float): Float {
    val progress = ((y - (curvedMotionFadeStart(x) - MotionBlurLead)) / MotionBlurLead)
        .coerceIn(0f, 1f)
    return progress * progress * (3f - 2f * progress)
}

/** Keeps the static image visible until a video frame is actually rendered. */
@OptIn(UnstableApi::class)
@Composable
fun DynamicCoverView(
    imageUrl: String,
    cover: DynamicCover?,
    playing: Boolean,
    onPlaybackError: (PlaybackException) -> Unit = {},
    onFrameSample: ((Bitmap) -> Unit)? = null,
    onPreviewSample: ((Bitmap) -> Unit)? = null,
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
    var blurredFrameView by remember(cover?.cacheKey, cover?.url) {
        mutableStateOf<BlurredFrameView?>(null)
    }
    val currentFrameSample by rememberUpdatedState(onFrameSample)
    val currentPreviewSample by rememberUpdatedState(onPreviewSample)
    val currentTextureView by rememberUpdatedState(textureView)
    val previewModel = remember(context, cover?.previewUrl, imageUrl, fitVideoWidth) {
        val url = cover?.previewUrl ?: imageUrl
        if (fitVideoWidth && cover?.previewUrl != null) {
            ImageRequest.Builder(context).data(url).allowHardware(false).build()
        } else url
    }
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
            override fun onRenderedFirstFrame() {
                firstFrame = true
                // A cached video can render before the sampling coroutine is launched.
                currentTextureView?.takeIf { it.isAvailable }?.let { view ->
                    captureMotionSample(view, fitVideoWidth)?.let { currentFrameSample?.invoke(it) }
                }
            }
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
                    captureMotionSample(view, fitVideoWidth)
                        ?.let { currentFrameSample?.invoke(it) }
                }
                if (!playing) break
                delay((if (fitVideoWidth) 42 else 66).milliseconds)
            } while (isActive)
        }
    }
    LaunchedEffect(player, textureView, blurredFrameView, playing, firstFrame, failed) {
        val source = textureView
        val target = blurredFrameView
        if (player != null && source != null && target != null && firstFrame && !failed) {
            do {
                target.captureFrom(source)
                if (!playing) break
                delay(100.milliseconds)
            } while (isActive)
        }
    }
    val showVideo = player != null && !failed && firstFrame &&
        videoDimensions.first > 0 && videoDimensions.second > 0
    Box(modifier.background(bottomFadeColor ?: Color.Transparent)) {
        AsyncImage(
            model = previewModel,
            contentDescription = "专辑封面",
            contentScale = ContentScale.Crop,
            onSuccess = { success ->
                if (fitVideoWidth && cover?.previewUrl != null && currentPreviewSample != null) {
                    val source = success.result.image.toBitmap()
                    val view = currentTextureView
                    val sampleWidth = motionSampleWidth(view?.width ?: 0)
                    val sampleHeight = motionSampleHeight(view?.height ?: 0)
                    val sample = Bitmap.createScaledBitmap(source, sampleWidth, sampleHeight, true)
                    currentPreviewSample?.invoke(
                        if (sample === source) sample.copy(Bitmap.Config.ARGB_8888, false) else sample
                    )
                }
            },
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
                            if (fitVideoWidth && bottomFadeColor != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                BlurredFrameView(viewContext).also {
                                    blurredFrameView = it
                                    frame.addView(it, FrameLayout.LayoutParams(-1, -1))
                                }
                            }
                        }
                    },
                    update = { frame ->
                        (frame.getChildAt(0) as CenterCropTextureView).setVideoDimensions(
                            videoDimensions.first, videoDimensions.second, videoDimensions.third,
                            fitVideoWidth,
                        )
                        (frame.getChildAt(1) as BottomFadeView).fadeColor =
                            bottomFadeColor?.toArgb()
                    },
                    modifier = Modifier.fillMaxSize().alpha(if (showVideo) 1f else 0f)
                )
            }
        }
        if (bottomFadeColor != null && !showVideo) {
            AndroidView(
                factory = ::BottomFadeView,
                update = { it.fadeColor = bottomFadeColor.toArgb() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun motionSampleWidth(viewWidth: Int): Int =
    if (viewWidth > 0) (kotlin.math.ceil(viewWidth / 80.0).toInt() * 10).coerceAtLeast(2)
    else 50

private fun motionSampleHeight(viewHeight: Int): Int =
    if (viewHeight > 0) (viewHeight / 16).coerceAtLeast(2) else 32

private fun captureMotionSample(view: TextureView, fitVideoWidth: Boolean): Bitmap? =
    runCatching {
        view.getBitmap(
            if (fitVideoWidth) motionSampleWidth(view.width) else 16,
            if (fitVideoWidth) motionSampleHeight(view.height) else 16,
        )
    }.getOrNull()

/** Blurs a sampled copy of the video only inside the curved transition band. */
private class BlurredFrameView(context: Context) : View(context) {
    private val frameBuffers = Array(2) {
        Bitmap.createBitmap(MotionMaskWidth, 256, Bitmap.Config.ARGB_8888)
    }
    private val mask = Bitmap.createBitmap(MotionMaskWidth, MotionMaskHeight, Bitmap.Config.ARGB_8888)
    private val maskPixels = IntArray(MotionMaskWidth * MotionMaskHeight)
    private val destination = Rect()
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var frameBitmap: Bitmap? = null
    private var nextBuffer = 0

    fun captureFrom(source: TextureView) {
        if (!source.isAvailable) return
        val buffer = frameBuffers[nextBuffer]
        runCatching { source.getBitmap(buffer) }.onSuccess {
            frameBitmap = buffer
            nextBuffer = 1 - nextBuffer
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        destination.set(0, 0, w, h)
        if (h <= 0) return
        val solidFloor = (MotionFadeEdgeStart + MotionFadeCenterDrop + MotionFadeWidth +
            MotionSolidFloorBelowCurveDp * resources.displayMetrics.density / h).coerceAtMost(1f)
        for (index in maskPixels.indices) {
            val x = (index % MotionMaskWidth).toFloat() / (MotionMaskWidth - 1)
            val y = (index / MotionMaskWidth).toFloat() / (MotionMaskHeight - 1)
            val fallStart = curvedMotionFadeStart(x) + MotionFadeWidth * 0.4f
            val fallProgress = ((y - fallStart) / (solidFloor - fallStart)).coerceIn(0f, 1f)
            val fall = 1f - fallProgress * fallProgress * (3f - 2f * fallProgress)
            val alpha = curvedMotionBlurAlpha(x, y) * fall
            maskPixels[index] = ((alpha * 255f).toInt() shl 24) or 0x00ffffff
        }
        mask.setPixels(maskPixels, 0, MotionMaskWidth, 0, 0, MotionMaskWidth, MotionMaskHeight)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val radius = MotionBlurRadiusDp * resources.displayMetrics.density
            setRenderEffect(
                RenderEffect.createBlendModeEffect(
                    RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP),
                    RenderEffect.createBitmapEffect(mask, null, destination),
                    BlendMode.DST_IN,
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = frameBitmap ?: return
        canvas.drawBitmap(bitmap, null, destination, framePaint)
    }
}

/** Draws above TextureView in the same native view hierarchy, so its bottom edge cannot bypass the fade. */
private class BottomFadeView(context: Context) : View(context) {

    private val mask = Bitmap.createBitmap(MotionMaskWidth, MotionMaskHeight, Bitmap.Config.ARGB_8888)
    private val maskAlpha = IntArray(MotionMaskWidth * MotionMaskHeight) { index ->
        val x = (index % MotionMaskWidth).toFloat() / (MotionMaskWidth - 1)
        val y = (index / MotionMaskWidth).toFloat() / (MotionMaskHeight - 1)
        (curvedMotionFadeAlpha(x, y) * 255f).toInt()
    }
    private val pixels = IntArray(maskAlpha.size)
    private val destination = Rect()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    var fadeColor: Int? = null
        set(value) {
            if (field != value) {
                field = value
                value?.let { color ->
                    val rgb = color and 0x00ffffff
                    for (index in pixels.indices) {
                        pixels[index] = (maskAlpha[index] shl 24) or rgb
                    }
                    mask.setPixels(pixels, 0, MotionMaskWidth, 0, 0, MotionMaskWidth, MotionMaskHeight)
                }
                invalidate()
            }
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        destination.set(0, 0, w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (fadeColor != null) canvas.drawBitmap(mask, null, destination, paint)
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
