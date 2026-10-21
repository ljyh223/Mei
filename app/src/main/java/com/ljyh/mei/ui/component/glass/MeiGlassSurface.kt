package com.ljyh.mei.ui.component.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import dev.chrisbanes.haze.glass.hazeGlass

val LocalMeiHazeState = staticCompositionLocalOf<HazeState?> { null }
val LocalMeiGlassEnabled = staticCompositionLocalOf { false }

enum class MeiGlassRole(
    val blurDp: Float,
    val refraction: Float,
    /** Fallback surface alpha when the live glass path is unavailable. */
    val lightTint: Float,
    val darkTint: Float,
    /** Tint painted over Haze's captured backdrop. */
    val hazeLightTint: Float,
    val hazeDarkTint: Float,
) {
    Navigation(20f, 0.70f, 0.78f, 0.80f, 0.30f, 0.36f),
    MiniPlayer(24f, 0.75f, 0.72f, 0.76f, 0.26f, 0.32f),
    Group(16f, 0.65f, 0.92f, 0.92f, 0.38f, 0.42f),
    PlayerControl(12f, 0.55f, 0.14f, 0.16f, 0.10f, 0.12f),
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
    val dark = colors.surface.luminance() < 0.5f
    val roundedShape = shape as? RoundedCornerShape
    val fallbackTint = (tintColor ?: colors.surfaceContainerLow).copy(
        alpha = if (role == MeiGlassRole.PlayerControl) {
            if (dark) role.darkTint else role.lightTint
        } else 1f,
    )
    val hazeTint = (tintColor ?: colors.surfaceContainerLow).copy(
        alpha = if (dark) role.hazeDarkTint else role.hazeLightTint,
    )
    val glassStyle = remember(role, dark, hazeTint, roundedShape) {
        roundedShape?.let { resolvedShape ->
            GlassStyle.regular.then {
                shape(resolvedShape)
                tint(hazeTint)
                optics(
                    blurRadius = role.blurDp.dp,
                    refractionStrength = role.refraction,
                    depth = 0.5f,
                )
                specularIntensity(0.45f)
                ambientResponse(0.20f)
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
    Box(
        modifier = modifier
            .clip(shape)
            .then(material),
        content = content,
    )
}
