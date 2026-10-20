package com.ljyh.mei.ui.component.player.component

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
import com.ljyh.mei.constants.CoverBackgroundConfidenceKey
import com.ljyh.mei.constants.CoverBackgroundToleranceKey
import com.ljyh.mei.constants.CoverSingleEdgeMatchKey
import com.ljyh.mei.constants.CoverSoftEdgeMatchKey
import com.ljyh.mei.constants.CoverSolidEdgeMatchKey
import com.ljyh.mei.utils.image.CoverBackgroundConfig
import com.ljyh.mei.utils.image.CoverExtensionMode
import com.ljyh.mei.utils.image.analyzeCoverBackground
import com.ljyh.mei.utils.image.buildCoverEdgeTexture
import com.ljyh.mei.utils.image.sizedNeteaseImage
import com.ljyh.mei.utils.preferences.rememberPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val AnalysisSize = 128

internal class CoverEdgeArtwork(
    val texture: Bitmap,
    val feather: ImageBitmap,
    val controlsColor: Color,
    val mode: CoverExtensionMode,
)

private object CoverEdgeDecisions {
    private data class Key(val url: String, val config: CoverBackgroundConfig)
    private val values = LinkedHashMap<Key, CoverEdgeArtwork?>(64, 0.75f, true)

    fun get(url: String, config: CoverBackgroundConfig): Pair<Boolean, CoverEdgeArtwork?> = synchronized(values) {
        val key = Key(url, config)
        values.containsKey(key) to values[key]
    }

    fun put(url: String, config: CoverBackgroundConfig, artwork: CoverEdgeArtwork?) = synchronized(values) {
        values[Key(url, config)] = artwork
        if (values.size > 64) values.remove(values.keys.first())
    }
}

/** Classifies a cover once, then keeps its small edge textures for later player openings. */
@Composable
internal fun rememberCoverEdgeArtwork(imageUrl: String?): CoverEdgeArtwork? {
    val context = LocalContext.current
    val defaults = remember { CoverBackgroundConfig() }
    val (tolerance) = rememberPreference(CoverBackgroundToleranceKey, defaults.colorTolerance)
    val (backgroundConfidence) = rememberPreference(CoverBackgroundConfidenceKey, defaults.minimumBackgroundConfidence)
    val (solidEdgeMatch) = rememberPreference(CoverSolidEdgeMatchKey, defaults.solidEdgeMatch)
    val (softEdgeMatch) = rememberPreference(CoverSoftEdgeMatchKey, defaults.softEdgeMatch)
    val (singleEdgeMatch) = rememberPreference(CoverSingleEdgeMatchKey, defaults.minimumSingleEdgeMatch)
    val config = CoverBackgroundConfig(
        colorTolerance = tolerance,
        minimumBackgroundConfidence = backgroundConfidence,
        solidEdgeMatch = solidEdgeMatch,
        softEdgeMatch = softEdgeMatch,
        minimumSingleEdgeMatch = singleEdgeMatch,
    )
    val artwork by produceState<CoverEdgeArtwork?>(null, imageUrl, config) {
        if (imageUrl.isNullOrBlank()) return@produceState
        val (cached, decision) = CoverEdgeDecisions.get(imageUrl, config)
        if (cached) {
            value = decision
            return@produceState
        }
        try {
            val result = withContext(Dispatchers.IO) {
                context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(imageUrl.sizedNeteaseImage(256))
                        .size(256)
                        .allowHardware(false)
                        .build()
                ) as? SuccessResult
            } ?: return@produceState
            val analyzed = withContext(Dispatchers.Default) {
                val original = result.image.toBitmap()
                if (abs(original.width - original.height) > max(original.width, original.height) * 0.05f) {
                    return@withContext null
                }
                val reduced = Bitmap.createScaledBitmap(original, AnalysisSize, AnalysisSize, true)
                try {
                    val pixels = IntArray(AnalysisSize * AnalysisSize)
                    reduced.getPixels(pixels, 0, AnalysisSize, 0, 0, AnalysisSize, AnalysisSize)
                    val analysis = analyzeCoverBackground(pixels, AnalysisSize, AnalysisSize, config)
                    if (analysis == null || !analysis.isSolidBackground) {
                        return@withContext null
                    }
                    val textures = buildCoverEdgeTexture(
                        pixels, AnalysisSize, AnalysisSize, analysis.color, analysis.mode,
                    )
                    CoverEdgeArtwork(
                        texture = Bitmap.createBitmap(
                            textures.extension, AnalysisSize, AnalysisSize, Bitmap.Config.ARGB_8888,
                        ),
                        feather = Bitmap.createBitmap(
                            textures.feather, AnalysisSize, AnalysisSize, Bitmap.Config.ARGB_8888,
                        ).asImageBitmap(),
                        controlsColor = Color(textures.controlsColor),
                        mode = analysis.mode,
                    )
                } finally {
                    if (reduced !== original) reduced.recycle()
                }
            }
            CoverEdgeDecisions.put(imageUrl, config, analyzed)
            value = analyzed
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // An unsuccessful load is retried on the next opening.
        }
    }
    return artwork
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
