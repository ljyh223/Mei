package com.ljyh.mei.ui.screen.artist

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import com.ljyh.mei.ui.theme.LocalPageLightStatusBar

/** Only activate optional artwork colors after the main photograph has decoded. */
@Composable
internal fun ArtistArtworkTheme(
    artwork: AppleArtistArtwork?,
    ready: Boolean,
    immersive: Boolean = false,
    content: @Composable () -> Unit,
) {
    val original = MaterialTheme.colorScheme
    val background = remember(artwork, original.background, immersive) {
        val source = artwork?.portrait?.background?.let { value ->
            runCatching { Color(android.graphics.Color.parseColor("#$value")) }.getOrNull()
        } ?: original.background
        if (immersive) artistImmersiveBackground(source) else source
    }
    val foreground = if (background.luminance() > 0.179f) Color.Black else Color.White
    val colors = if (ready) original.copy(
        background = background, onBackground = foreground,
        surface = background, onSurface = foreground, onSurfaceVariant = foreground.copy(alpha = 0.74f),
        primary = foreground, onPrimary = background, secondary = foreground,
        surfaceVariant = lerp(background, foreground, 0.10f),
        surfaceContainerLowest = background,
        surfaceContainerLow = lerp(background, foreground, 0.04f),
        surfaceContainer = lerp(background, foreground, 0.08f),
        surfaceContainerHigh = lerp(background, foreground, 0.12f),
        surfaceContainerHighest = lerp(background, foreground, 0.16f),
        outline = foreground.copy(alpha = 0.35f), outlineVariant = foreground.copy(alpha = 0.16f),
    ) else original
    val owner = remember { Any() }
    val statusBar = LocalPageLightStatusBar.current
    LifecycleResumeEffect(ready, background) {
        statusBar(owner, if (ready) background.luminance() > 0.179f else null)
        onPauseOrDispose { statusBar(owner, null) }
    }
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}

/** Keep the source hue, while giving white titles and secondary text a stable dark backdrop. */
internal fun artistImmersiveBackground(source: Color): Color {
    if (source.luminance() <= 0.065f) return source
    var low = 0f
    var high = 1f
    repeat(10) {
        val amount = (low + high) / 2f
        if (lerp(source, Color.Black, amount).luminance() > 0.065f) low = amount else high = amount
    }
    return lerp(source, Color.Black, high)
}
