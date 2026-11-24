package com.ljyh.mei.ui.screen.artist.component

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

/** MusicUI 24B156: the mask is drawn at half width, then expanded over the artwork. */
internal object ArtistBackdropGeometry {
    const val MaxRadius = 160f
    fun scale(scroll: Float): Float = when {
        scroll < 0f -> 1f - scroll * 0.002f
        scroll > 180f -> 1f + (scroll - 180f) * 0.00025f
        else -> 1f
    }
    fun opacity(scroll: Float): Float = (1f - (scroll - 180f).coerceAtLeast(0f) * 0.005f).coerceIn(0f, 1f)
    fun extraBlur(scroll: Float): Float = ((scroll - 180f) / 6f).coerceIn(0f, 100f)
}

/** Only the photographic layer is filtered. Names, buttons and song rows stay sharp. */
@Composable
internal fun Modifier.artistBackdropEffects(scrollPixels: () -> Float): Modifier {
    val filter = remember {
        if (Build.VERSION.SDK_INT >= 33) ArtistVariableBlur() else null
    }
    val scrollBlur = remember { if (Build.VERSION.SDK_INT >= 31) ArtistScrollBlur() else null }
    return graphicsLayer {
        renderEffect = if (Build.VERSION.SDK_INT >= 31) scrollBlur?.effect(
            ArtistBackdropGeometry.extraBlur(scrollPixels() / density) * density,
        )?.asComposeRenderEffect() else null
    }.graphicsLayer {
        val scroll = scrollPixels() / density
        val zoom = ArtistBackdropGeometry.scale(scroll)
        scaleX = zoom
        scaleY = zoom
        translationY = -scrollPixels().coerceAtLeast(0f) * 0.4f
        alpha = ArtistBackdropGeometry.opacity(scroll)
    }.graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithCache {
        val fade = Brush.verticalGradient(
            0f to Color.White, 0.25f to Color.White, 0.65f to Color.Transparent,
            1f to Color.Transparent, endY = size.width,
        )
        onDrawWithContent {
            drawContent()
            // Opacity follows filtering, and the whole result follows the scroll transform.
            drawRect(fade, blendMode = BlendMode.DstIn)
        }
    }.graphicsLayer {
        renderEffect = if (Build.VERSION.SDK_INT >= 33) {
            filter?.effect(size, density)?.asComposeRenderEffect()
        } else null
        clip = true
    }
}

/**
 * Android's approximation of SwiftUI.variableBlur: two separable Gaussian passes with a
 * spatially varying radius, not an opacity blend with a uniformly blurred photograph.
 * There is no glass material, refraction, tint or highlight in this renderer.
 */
@RequiresApi(33)
private class ArtistVariableBlur {
    private val horizontal = RuntimeShader(ArtistVariableBlurShader)
    private val vertical = RuntimeShader(ArtistVariableBlurShader)
    private var previousSize = Size.Zero
    private var previousDensity = 0f
    private var base: RenderEffect? = null

    fun effect(size: Size, density: Float): RenderEffect? {
        if (size.width <= 0f || size.height <= 0f) return null
        if (size != previousSize || density != previousDensity) {
            for ((shader, axis) in listOf(horizontal to floatArrayOf(1f, 0f), vertical to floatArrayOf(0f, 1f))) {
                shader.setFloatUniform("bounds", size.width, size.height)
                shader.setFloatUniform("axis", axis)
                shader.setFloatUniform("maxRadius", ArtistBackdropGeometry.MaxRadius * density)
            }
            base = RenderEffect.createChainEffect(
                RenderEffect.createRuntimeShaderEffect(vertical, "image"),
                RenderEffect.createRuntimeShaderEffect(horizontal, "image"),
            )
            previousSize = size
            previousDensity = density
        }
        return base
    }
}

@RequiresApi(31)
private class ArtistScrollBlur {
    private var previousRadius = -1
    private var output: RenderEffect? = null
    fun effect(extraPixels: Float): RenderEffect? {
        val radius = extraPixels.toInt()
        if (radius != previousRadius) {
            output = if (radius > 0) RenderEffect.createBlurEffect(
                radius.toFloat(), radius.toFloat(), Shader.TileMode.MIRROR,
            ) else null
            previousRadius = radius
        }
        return output
    }
}

// Mask stops recovered from ArtistDetailBackgroundView, not the older MusicCoreUI blur.
// x/2 reproduces MusicUI's half-width mask renderer, hence the broad elliptical clear area.
internal const val ArtistVariableBlurShader = """
    uniform shader image;
    uniform float2 bounds;
    uniform float2 axis;
    uniform float maxRadius;

    float mask(float2 p) {
        float distance = length(float2((p.x - bounds.x * 0.5) * 0.5, p.y)) / bounds.x;
        if (distance <= 0.4) return 0.0;
        if (distance <= 0.5) return mix(0.0, 0.0625, (distance - 0.4) / 0.1);
        if (distance <= 0.75) return mix(0.0625, 0.4, (distance - 0.5) / 0.25);
        return mix(0.4, 0.625, clamp((distance - 0.75) / 0.25, 0.0, 1.0));
    }
    float2 reflected(float2 p) {
        float2 wrapped = mod(p, 2.0 * bounds);
        return clamp(bounds - abs(wrapped - bounds), float2(0.5), bounds - 0.5);
    }
    half4 main(float2 p) {
        float radius = maxRadius * mask(p);
        if (radius < 0.01) return image.eval(p);
        float sigma = radius * 0.57735 + 0.5 * min(radius, 1.0);
        half4 color = half4(0.0);
        float weightSum = 0.0;
        // Bounded quadrature over ±3 sigma. No per-frame CPU bitmap or image request.
        for (int i = -20; i <= 20; ++i) {
            float z = float(i) * 0.15;
            float weight = exp(-0.5 * z * z);
            color += image.eval(reflected(p + axis * (z * sigma))) * weight;
            weightSum += weight;
        }
        return color / weightSum;
    }
"""
