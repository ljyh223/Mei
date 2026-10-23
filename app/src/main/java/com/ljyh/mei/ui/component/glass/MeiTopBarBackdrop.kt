package com.ljyh.mei.ui.component.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

private val TopBarTransitionExtension = 34.dp
private const val TopBarTransitionStart = 0.42f

/**
 * The toolbar keeps its measured height. Only this non-interactive background extends below it,
 * progressively returning the sampled page to its original detail and colour.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
internal fun MeiTopBarBackdrop(
    state: HazeState?,
    modifier: Modifier = Modifier,
    progress: Float = 1f,
) {
    val live = LocalMeiGlassEnabled.current && state != null && Build.VERSION.SDK_INT >= 33
    val surface = MaterialTheme.colorScheme.surface
    // Live sampling already softens detail. A strong surface scrim washes artwork into a white band.
    // Only the no-blur fallback needs a more opaque background for text legibility.
    val tint = remember(surface, live) {
        val opacity = if (live) 0.12f else 0.90f
        Brush.verticalGradient(
            0f to surface.copy(alpha = opacity),
            TopBarTransitionStart to surface.copy(alpha = opacity * 0.55f),
            1f to surface.copy(alpha = 0f),
        )
    }
    Box(
        modifier.layout { measurable, constraints ->
            // Report the original bar height, so the transition cannot add list padding or move controls.
            val extension = if (constraints.hasBoundedHeight) TopBarTransitionExtension.roundToPx() else 0
            val placeable = measurable.measure(
                if (extension > 0) constraints.copy(
                    minHeight = constraints.minHeight + extension,
                    maxHeight = constraints.maxHeight + extension,
                ) else constraints,
            )
            layout(placeable.width, placeable.height - extension) {
                placeable.place(0, 0)
            }
        }.graphicsLayer {
            alpha = progress.coerceIn(0f, 1f)
        }.clipToBounds(),
    ) {
        if (live) {
            Box(
                Modifier.matchParentSize().hazeGlass(
                    input = HazeInput.Sources(state),
                    style = MeiTopBarBlurStyle,
                    performanceMode = HazePerformanceMode.Default,
                    expandLayerBounds = true,
                ),
            )
        }
        // Independent from the optical effect: preserve the page's hues while reducing detail.
        // Static fallback uses the same gentle scrim without recording or filtering a backdrop.
        Box(Modifier.matchParentSize().background(tint))
    }
}

/** The mask scales Gaussian sampling offsets at each pixel, rather than fading a fixed blur. */
private val TopBarProgressiveBlur = HazeProgressive.forShader { size ->
    LinearGradientShader(
        from = Offset(0f, size.height * TopBarTransitionStart),
        to = Offset(0f, size.height),
        colors = listOf(Color.White, Color.Transparent),
    )
}

/** Plain progressive blur with no glass edge, lighting, refraction or desaturation. */
@OptIn(ExperimentalHazeApi::class)
internal val MeiTopBarBlurStyle = GlassStyle.regular.then {
    shape(RoundedCornerShape(0.dp))
    tint(Color.Transparent)
    backgroundColor(Color.Transparent)
    optics(
        blurRadius = 10.dp,
        refractionStrength = 0f,
        depth = 1f,
        progressive = TopBarProgressiveBlur,
    )
    specularIntensity(0f)
    edgeShadow(Color.Transparent)
    ambientResponse(0f)
    contrast(0f)
    whitePoint(0f)
    chromaMultiplier(1f)
    chromaticAberrationStrength(0f)
    contentNormalBlend(0f)
    edgeSoftness(0.dp)
}
