package com.ljyh.mei.ui.component.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
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

enum class MeiGlassRole(
    val blurDp: Float,
    val lightTint: Float,
    val darkTint: Float,
) {
    Navigation(20f, 0.78f, 0.80f),
    MiniPlayer(24f, 0.72f, 0.76f),
    Search(24f, 0.78f, 0.80f),
    Group(16f, 0.92f, 0.92f),
    ToolbarControl(16f, 0.74f, 0.74f),
    PlayerControl(12f, 0.14f, 0.16f),
}

/** The source layer contains page content only, never a surface that samples itself. */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun MeiGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    role: MeiGlassRole = MeiGlassRole.Group,
    hazeState: HazeState? = LocalMeiHazeState.current,
    tintColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val appearance = LocalMeiGlassAppearance.current.normalized()
    val dark = colors.surface.luminance() < 0.5f
    val roundedShape = shape as? RoundedCornerShape
    // Without live shader glass the material still honours the opacity setting, so the
    // appearance sliders keep having a visible effect on devices where Haze cannot run.
    val fallbackTint = (tintColor ?: colors.surfaceContainerLow).copy(
        alpha = if (role == MeiGlassRole.PlayerControl) {
            if (dark) role.darkTint else role.lightTint
        } else {
            appearance.opacityFor(role)
        },
    )
    val readingTint = (tintColor ?: colors.surfaceContainerLow).copy(alpha = appearance.opacityFor(role))
    val glassStyle = remember(role, appearance, roundedShape) {
        roundedShape?.let { resolvedShape ->
            GlassStyle.regular.then {
                shape(resolvedShape)
                tint(Color.Transparent)
                val toolbarControl = role == MeiGlassRole.ToolbarControl
                // Floating chrome keeps the centre diffused; refraction belongs at the rim.
                val edgeWidth = when (role) {
                    MeiGlassRole.ToolbarControl -> 2.dp
                    MeiGlassRole.Navigation, MeiGlassRole.Search -> 3.dp
                    MeiGlassRole.MiniPlayer -> 4.dp
                    else -> 0.dp
                }
                val edgeGlass = edgeWidth > 0.dp
                val refractionScale = when {
                    toolbarControl || role == MeiGlassRole.Search -> 0.4f
                    edgeGlass -> 0.6f
                    else -> 1f
                }
                optics(
                    blurRadius = (appearance.blur * role.blurDp / 20f).dp,
                    refractionStrength = appearance.refraction * refractionScale,
                    refractionHeightFraction = if (toolbarControl) 0.08f else if (edgeGlass) 0.12f else 0.25f,
                    refractionDisplacement = if (edgeGlass) edgeWidth else 15.dp,
                    refractionProfile = if (edgeGlass) RefractionProfile.Edge(edgeWidth) else RefractionProfile.Surface,
                    refractionDetailIntensity = if (edgeGlass) 0f else 0.76f,
                    depth = if (edgeGlass) 1f else 0.5f,
                )
                specularIntensity(appearance.highlight * when { toolbarControl -> 0.5f; edgeGlass -> 0.6f; else -> 1f })
                ambientResponse(when { toolbarControl -> 0.03f; edgeGlass -> 0.04f; else -> 0.12f })
                if (edgeGlass) {
                    edgeShadow(Color.Transparent)
                    whitePoint(0f)
                    contrast(0f)
                }
                chromaMultiplier(appearance.saturation)
                chromaticAberrationStrength(0f)
                contentNormalBlend(0f)
                edgeSoftness(0.5.dp)
            }
        }
    }
    val live = LocalMeiGlassEnabled.current && hazeState != null && glassStyle != null &&
        Build.VERSION.SDK_INT >= 33
    val material = if (live) {
        Modifier.hazeGlass(
            input = HazeInput.Sources(hazeState),
            style = glassStyle,
            performanceMode = HazePerformanceMode.Default,
            expandLayerBounds = true,
        )
    } else {
        Modifier.background(fallbackTint, shape)
    }
    Box(modifier = modifier.clip(shape)) {
        // Optical effects and their highlights are painted behind the readable foreground.
        Box(modifier = Modifier.matchParentSize().then(material))
        if (live) Box(Modifier.matchParentSize().background(readingTint, RectangleShape))
        content()
    }
}
