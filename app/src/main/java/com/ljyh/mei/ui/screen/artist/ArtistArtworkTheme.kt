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
import com.ljyh.mei.utils.color.immersiveArtworkBackground

internal const val ArtistImmersiveSecondaryOpacity = 0.86f

/** Only activate optional artwork colors after the main photograph has decoded. */
@Composable
internal fun ArtistArtworkTheme(
    artwork: AppleArtistArtwork?,
    ready: Boolean,
    immersive: Boolean = false,
    backgroundOverride: String? = null,
    sourceColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val original = MaterialTheme.colorScheme
    val background = remember(artwork, original.background, immersive, backgroundOverride, sourceColor) {
        val source = sourceColor ?: (backgroundOverride ?: artwork?.portrait?.background)?.let { value ->
            runCatching { Color(android.graphics.Color.parseColor("#$value")) }.getOrNull()
        } ?: original.background
        if (immersive) immersiveArtworkBackground(source) else source
    }
    val foreground = if (background.luminance() > 0.179f) Color.Black else Color.White
    val colors = if (ready) original.copy(
        background = background, onBackground = foreground,
        surface = background, onSurface = foreground, onSurfaceVariant = foreground.copy(alpha = if (immersive) ArtistImmersiveSecondaryOpacity else 0.74f),
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
