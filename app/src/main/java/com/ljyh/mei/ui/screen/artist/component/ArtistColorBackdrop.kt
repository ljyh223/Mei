package com.ljyh.mei.ui.screen.artist.component

import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity

private const val BackdropScale = 0.1f
private const val BackdropBlurRadius = 200f
private val ImageContrast = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
    0.5f, 0f, 0f, 0f, 63.75f,
    0f, 0.5f, 0f, 0f, 63.75f,
    0f, 0f, 0.5f, 0f, 63.75f,
    0f, 0f, 0f, 1f, 0f,
)))

/** A photographic color field, never a glass container or a second image request. */
@Composable
internal fun ArtistColorBackdrop(painter: Painter) {
    val density = LocalDensity.current.density
    val background = MaterialTheme.colorScheme.background
    val effect = remember(density) {
        if (Build.VERSION.SDK_INT >= 31) artistColorBackdropEffect(density).asComposeRenderEffect() else null
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(background)) {
        // Sample at 1/10 resolution. Multiplying the Gaussian radius by the same scale
        // avoids filtering a full tablet-sized texture on every frame. This approximates
        // MusicUI's backdrop material; it does not reproduce Apple's renderer kernel.
        if (effect != null) {
            Canvas(Modifier.align(Alignment.Center).size(maxWidth * BackdropScale, maxHeight * BackdropScale)
                .graphicsLayer {
                    scaleX = 1f / BackdropScale
                    scaleY = 1f / BackdropScale
                    renderEffect = effect
                    clip = true
                }) {
                val side = maxOf(size.width, size.height) * 2f
                withTransform({ translate((size.width - side) / 2f, (size.height - side) / 2f) }) {
                    with(painter) { draw(Size(side, side), colorFilter = ImageContrast) }
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color.White.copy(alpha = 0.08f))
                drawRect(Color.Black.copy(alpha = 0.35f))
                // Keep the song catalogue on its contrast-safe page palette. The portrait,
                // wordmark and controls are drawn separately, above this photographic field.
                drawRect(Brush.verticalGradient(
                    0f to Color.Transparent, 0.3f to background.copy(alpha = 0.4f),
                    0.7f to background, 1f to background,
                ))
            }
        }
        // Older Android versions retain the page palette and sharp foreground avatar;
        // an unblurred enlarged face must never leak through as the fallback background.
    }
}

@RequiresApi(31)
internal fun artistColorBackdropEffect(density: Float): RenderEffect {
    val radius = BackdropBlurRadius * BackdropScale * density
    val saturation = android.graphics.ColorMatrix().apply { setSaturation(4f) }
    return RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(saturation),
        RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR))
}
