package com.ljyh.mei.ui.component.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.RefractionProfile
import dev.chrisbanes.haze.glass.hazeGlass

val LocalMeiHazeState = staticCompositionLocalOf<HazeState?> { null }
val LocalMeiGlassEnabled = staticCompositionLocalOf { false }

@Composable
fun meiGlassContentColor(secondary: Boolean = false): Color =
    LocalMeiGlassAppearance.current.contentColor(
        MaterialTheme.colorScheme, secondary, liveGlass = LocalMeiGlassEnabled.current,
    )

@Composable
fun meiChromeSelectionColor(): Color =
    LocalMeiGlassAppearance.current.selectionColor(MaterialTheme.colorScheme, LocalMeiGlassEnabled.current)

@Composable
fun meiGlassAccentColor(): Color {
    val appearance = LocalMeiGlassAppearance.current
    val colors = MaterialTheme.colorScheme
    val live = LocalMeiGlassEnabled.current
    return remember(appearance, colors, live) { appearance.accentColor(colors, live) }
}

/** Shared chrome: liquid optics when available, otherwise an opaque tonal surface. */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun MeiGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    hazeState: HazeState? = LocalMeiHazeState.current,
    tintColor: Color? = null,
    solidColor: Color? = null,
    shadowElevation: Dp = 2.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val appearance = LocalMeiGlassAppearance.current.normalized()
    val dark = colors.surface.luminance() < 0.5f
    val roundedShape = shape as? RoundedCornerShape
    val opticsAvailable = appearance.usesOptics(LocalMeiGlassEnabled.current && Build.VERSION.SDK_INT >= 33)
    val glassStyle = remember(appearance, roundedShape, dark, opticsAvailable) {
        if (!opticsAvailable) return@remember null
        roundedShape?.let { resolvedShape ->
            // Keep Clear's complete lighting, edge and optical response, rather than drawing
            // a narrow refractive outline over a mostly opaque frosted surface.
            GlassStyle.clear.then {
                shape(resolvedShape)
                tint(Color.Transparent)
                optics(
                    blurRadius = appearance.blur.dp,
                    refractionStrength = appearance.refraction,
                    refractionHeightFraction = 0.35f,
                    refractionDisplacement = 56.dp,
                    refractionProfile = RefractionProfile.Edge(28.dp),
                )
                specularIntensity(appearance.highlight * if (dark) 0.42f / 0.55f else 1f)
                ambientResponse(if (dark) 0.22f else 0.42f)
                edgeShadow(Color.Black.copy(alpha = if (dark) 0.25f else 0.10f))
                // Compress backdrop extremes before the neutral tint, keeping album imagery
                // visible without letting its contrast compete with the foreground.
                whitePoint(if (dark) -0.22f else 0.20f)
                contrast(-0.12f)
                chromaMultiplier(appearance.saturation * if (dark) 1f else 1.05f)
            }
        }
    }
    val live = opticsAvailable && hazeState != null && glassStyle != null
    val tint = if (live) (tintColor ?: colors.surfaceContainerLow).copy(alpha = appearance.tintOpacity(true))
        else (solidColor ?: colors.surfaceContainer).copy(alpha = 1f)
    Box(modifier = modifier.shadow(if (live) 0.dp else shadowElevation, shape, clip = false).clip(shape)) {
        if (live) {
            Box(Modifier.matchParentSize().hazeGlass(
                input = HazeInput.Sources(hazeState),
                style = glassStyle,
                performanceMode = HazePerformanceMode.Default,
                expandLayerBounds = true,
            ))
        }
        // Tint and optical effects never filter the foreground glyphs or text.
        Box(Modifier.matchParentSize().background(tint))
        CompositionLocalProvider(LocalContentColor provides meiGlassContentColor()) { content() }
    }
}
