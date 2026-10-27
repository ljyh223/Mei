package com.ljyh.mei.ui.component.player.cover

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.ljyh.mei.utils.image.AlbumBackgroundAnalysis
import com.ljyh.mei.utils.image.ArtworkSize
import com.ljyh.mei.utils.image.CoverBackgroundConfig
import com.ljyh.mei.utils.image.CoverExtensionMode
import com.ljyh.mei.utils.image.analyzeCoverBackground
import com.ljyh.mei.utils.image.buildCoverEdgeTexture
import com.ljyh.mei.utils.preferences.rememberCoverBackgroundSettings
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val AnalysisSize = 128
private const val TextureSize = 64

internal data class CoverEdgeResult(
    val url: String,
    val config: CoverBackgroundConfig,
    val artwork: CoverEdgeArtwork? = null,
    val analysis: AlbumBackgroundAnalysis? = null,
    val failure: String? = null,
)

internal class CoverEdgeArtwork(
    val texture: Bitmap,
    val feather: ImageBitmap,
    val controlsColor: Color,
    val mode: CoverExtensionMode,
)

private object CoverEdgeDecisions {
    private data class Key(val url: String, val config: CoverBackgroundConfig)
    private val values = LinkedHashMap<Key, CoverEdgeResult>(32, 0.75f, true)

    fun get(url: String, config: CoverBackgroundConfig): CoverEdgeResult? = synchronized(values) {
        values[Key(url, config)]
    }

    fun put(result: CoverEdgeResult) = synchronized(values) {
        values[Key(result.url, result.config)] = result
        if (values.size > 32) values.remove(values.keys.first())
    }
}

/** The player and settings share the same classification, configuration key and cached pixels. */
@Composable
internal fun rememberCoverEdgeArtwork(imageUrl: String?): CoverEdgeArtwork? {
    val settings by rememberCoverBackgroundSettings()
    return rememberCoverEdgeResult(imageUrl, settings.config)?.artwork
}

@Composable
internal fun rememberCoverEdgeResult(imageUrl: String?, config: CoverBackgroundConfig): CoverEdgeResult? {
    val context = LocalContext.current
    val result by produceState<CoverEdgeResult?>(null, imageUrl, config) {
        if (imageUrl.isNullOrBlank()) return@produceState
        val cached = CoverEdgeDecisions.get(imageUrl, config)
        if (cached != null) {
            value = cached
            return@produceState
        }
        try {
            val loaded = withContext(Dispatchers.IO) {
                context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .size(ArtworkSize.Small.pixels)
                        .allowHardware(false)
                        .build(),
                ) as? SuccessResult
            }
            if (loaded == null) {
                value = CoverEdgeResult(imageUrl, config, failure = "暂时无法读取封面，请稍后重试")
                return@produceState
            }
            val analyzed = withContext(Dispatchers.Default) {
                val original = loaded.image.toBitmap()
                if (abs(original.width - original.height) > max(original.width, original.height) * 0.05f) {
                    return@withContext CoverEdgeResult(imageUrl, config, failure = "仅分析接近正方形的静态封面")
                }
                val reduced = Bitmap.createScaledBitmap(original, AnalysisSize, AnalysisSize, true)
                try {
                    val pixels = IntArray(AnalysisSize * AnalysisSize)
                    reduced.getPixels(pixels, 0, AnalysisSize, 0, 0, AnalysisSize, AnalysisSize)
                    val analysis = analyzeCoverBackground(pixels, AnalysisSize, AnalysisSize, config)
                    if (analysis == null) {
                        return@withContext CoverEdgeResult(imageUrl, config, failure = "封面没有足够的有效边缘像素")
                    }
                    if (!analysis.isSolidBackground || analysis.mode == CoverExtensionMode.NONE) {
                        return@withContext CoverEdgeResult(imageUrl, config, analysis = analysis)
                    }
                    val texturePixels = IntArray(TextureSize * TextureSize)
                    val textureBitmap = Bitmap.createScaledBitmap(reduced, TextureSize, TextureSize, true)
                    try {
                        textureBitmap.getPixels(texturePixels, 0, TextureSize, 0, 0, TextureSize, TextureSize)
                    } finally {
                        if (textureBitmap !== reduced) textureBitmap.recycle()
                    }
                    val textures = buildCoverEdgeTexture(
                        texturePixels, TextureSize, TextureSize, analysis.color, analysis.mode, analysis.sideMatches,
                    )
                    CoverEdgeResult(
                        url = imageUrl,
                        config = config,
                        analysis = analysis,
                        artwork = CoverEdgeArtwork(
                            texture = Bitmap.createBitmap(
                                textures.extension, TextureSize, TextureSize, Bitmap.Config.ARGB_8888,
                            ),
                            feather = Bitmap.createBitmap(
                                textures.feather, TextureSize, TextureSize, Bitmap.Config.ARGB_8888,
                            ).asImageBitmap(),
                            controlsColor = Color(textures.controlsColor),
                            mode = analysis.mode,
                        ),
                    )
                } finally {
                    if (reduced !== original) reduced.recycle()
                }
            }
            if (analyzed.failure == null) CoverEdgeDecisions.put(analyzed)
            value = analyzed
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            value = CoverEdgeResult(imageUrl, config, failure = "暂时无法分析封面，请稍后重试")
        }
    }
    // Invalidate the previous song's result immediately while the new request is still starting.
    return result?.takeIf { it.url == imageUrl && it.config == config }
}

/** CLAMP extends the sampled background color beyond the square cover. */
@Composable
internal fun CoverEdgeExtensionBackdrop(
    artwork: CoverEdgeArtwork,
    alpha: Float,
    coverLeft: Float,
    coverTop: Float,
    coverSize: Float,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0f || coverSize <= 0f) return
    val shader = remember(artwork.texture) {
        BitmapShader(artwork.texture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    val paint = remember(shader) { Paint(Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader } }
    Canvas(modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.coerceIn(0f, 1f) }) {
        val matrix = Matrix().apply {
            setScale(coverSize / artwork.texture.width, coverSize / artwork.texture.height)
            postTranslate(coverLeft, coverTop)
        }
        shader.setLocalMatrix(matrix)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        }
    }
}

/** Softens the seam between the displayed image and the sampled background color. */
internal fun Modifier.coverEdgeFeather(artwork: CoverEdgeArtwork?, alpha: Float): Modifier {
    if (artwork == null || alpha <= 0f) return this
    return drawWithContent {
        drawContent()
        drawImage(
            image = artwork.feather,
            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
            alpha = alpha.coerceIn(0f, 1f),
        )
    }
}
