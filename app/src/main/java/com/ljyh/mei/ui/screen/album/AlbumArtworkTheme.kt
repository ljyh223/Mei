package com.ljyh.mei.ui.screen.album

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ljyh.mei.data.repository.CoverPalette
import com.ljyh.mei.ui.component.artwork.album.albumMediaBackground
import com.ljyh.mei.ui.component.glass.LocalMeiChromeContentColor
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.theme.LocalPageLightStatusBar

internal data class AlbumArtworkColors(
    val background: Color,
    val title: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
)
internal val LocalAlbumArtworkColors = staticCompositionLocalOf<AlbumArtworkColors?> { null }

/** Apple supplies RGB hex strings, sometimes with a leading '#'. Backgrounds stay opaque. */
internal fun albumPaletteColor(hex: String?): Color? {
    val value = hex?.trim()?.removePrefix("#") ?: return null
    if (value.length != 6 && value.length != 8) return null
    val rgb = value.toLongOrNull(16) ?: return null
    return Color(0xff000000L or (rgb and 0xffffffL))
}

internal fun albumColorContrast(a: Color, b: Color): Float {
    val first = a.luminance()
    val second = b.luminance()
    return (maxOf(first, second) + .05f) / (minOf(first, second) + .05f)
}

/** Use the displayed video's palette, falling back field by field to the square asset. */
internal fun albumArtworkColors(
    preferred: CoverPalette?,
    fallback: CoverPalette?,
    fallbackBackground: Color,
): AlbumArtworkColors {
    val background = albumPaletteColor(preferred?.bgColor) ?: albumPaletteColor(fallback?.bgColor) ?: fallbackBackground
    val ink = if (background.luminance() > .179f) Color.Black else Color.White
    fun text(first: String?, second: String?, default: Color): Color =
        listOfNotNull(albumPaletteColor(first), albumPaletteColor(second), default, ink)
            .first { albumColorContrast(it, background) >= 4.5f }
    val title = text(preferred?.textColor1, fallback?.textColor1, ink)
    val secondary = text(preferred?.textColor2, fallback?.textColor2, lerp(background, title, .78f))
    val tertiary = text(preferred?.textColor3, fallback?.textColor3, lerp(background, title, .68f))
    val accent = text(preferred?.textColor4, fallback?.textColor4, title)
    return AlbumArtworkColors(background, title, secondary, tertiary, accent)
}

@Composable
internal fun AlbumArtworkTheme(state: AlbumDetailUiState, content: @Composable () -> Unit) {
    val original = MaterialTheme.colorScheme
    val fallbackBackground = albumMediaBackground()
    val motion = state.motionArtwork?.takeIf {
        albumArtworkKind(state.artworkMode, state.album.publishTime, true, state.modelMode, allowMotion = !LocalPhoneLandscape.current) == AlbumArtworkKind.Motion
    }
    val portrait = !LocalPhoneLandscape.current
    val preferred = if (portrait) motion?.portraitClip?.palette else motion?.palette
    val palette = remember(motion, preferred, fallbackBackground) {
        motion?.let { albumArtworkColors(preferred, it.palette, fallbackBackground) }
    }
    val background = palette?.background ?: fallbackBackground
    val colors = if (palette == null) original.copy(background = background, surface = background) else {
        val base = if (background.luminance() < .179f) darkColorScheme() else lightColorScheme()
        base.copy(
            background = background, onBackground = palette.title,
            surface = background, onSurface = palette.title, onSurfaceVariant = palette.secondary,
            primary = palette.accent, onPrimary = background,
            primaryContainer = lerp(background, palette.accent, .15f), onPrimaryContainer = palette.accent,
            secondary = palette.secondary, onSecondary = background,
            secondaryContainer = lerp(background, palette.title, .12f), onSecondaryContainer = palette.title,
            tertiary = palette.tertiary, onTertiary = background,
            surfaceVariant = lerp(background, palette.title, .10f),
            surfaceContainerLowest = background,
            surfaceContainerLow = lerp(background, palette.title, .04f),
            surfaceContainer = lerp(background, palette.title, .08f),
            surfaceContainerHigh = lerp(background, palette.title, .12f),
            surfaceContainerHighest = lerp(background, palette.title, .16f),
            inverseSurface = palette.title, inverseOnSurface = background,
            outline = palette.secondary.copy(alpha = .45f), outlineVariant = palette.secondary.copy(alpha = .20f),
        )
    }
    val owner = remember { Any() }
    val setStatusBar = LocalPageLightStatusBar.current
    LifecycleResumeEffect(palette, setStatusBar) {
        setStatusBar(owner, palette?.let { it.background.luminance() > .179f })
        onPauseOrDispose { setStatusBar(owner, null) }
    }
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalContentColor provides colors.onSurface, LocalAlbumArtworkColors provides palette,
            LocalMeiChromeContentColor provides palette?.title,
            content = content)
    }
}
