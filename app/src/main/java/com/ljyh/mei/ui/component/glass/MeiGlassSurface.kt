package com.ljyh.mei.ui.component.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

val LocalMeiBackdrop = staticCompositionLocalOf<Backdrop?> { null }
val LocalMeiGlassEnabled = staticCompositionLocalOf { false }

enum class MeiGlassRole(val blurDp: Float, val lightTint: Float, val darkTint: Float) {
    Navigation(20f, 0.78f, 0.80f),
    MiniPlayer(24f, 0.72f, 0.76f),
    Group(16f, 0.92f, 0.92f),
    PlayerControl(12f, 0.14f, 0.16f),
}

/** The source layer contains page content only, never a surface that samples itself. */
@Composable
fun MeiGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    role: MeiGlassRole = MeiGlassRole.Group,
    backdrop: Backdrop? = LocalMeiBackdrop.current,
    tintColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val live = LocalMeiGlassEnabled.current && backdrop != null && Build.VERSION.SDK_INT >= 31
    val tint = (tintColor ?: colors.surfaceContainerLow).copy(
        alpha = if (live || role == MeiGlassRole.PlayerControl) { if (dark) role.darkTint else role.lightTint } else 1f,
    )
    val material = if (live) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                blur(role.blurDp.dp.toPx())
                lens(refractionHeight = 7.dp.toPx(), refractionAmount = 10.dp.toPx())
            },
            highlight = null,
            shadow = null,
            onDrawSurface = { drawRect(tint) },
        )
    } else Modifier.background(tint, shape)
    Box(
        modifier = modifier
            .then(material)
            .border(
                0.5.dp,
                Brush.verticalGradient(listOf(
                    Color.White.copy(alpha = if (dark) 0.16f else 0.85f),
                    colors.outlineVariant.copy(alpha = 0.42f),
                )),
                shape,
            )
            .clip(shape),
        content = content,
    )
}
