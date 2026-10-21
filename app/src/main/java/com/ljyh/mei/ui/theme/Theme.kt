package com.ljyh.mei.ui.theme

import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.DynamicMaterialTheme

/** Stable reading surfaces; artwork and interactive roles retain the dynamic accent. */
internal fun meiColorScheme(accent: ColorScheme, dark: Boolean): ColorScheme {
    val ink = if (dark) Color(0xFFF1F3F7) else Color(0xFF20242C)
    fun neutral(light: Long, night: Long) = lerp(
        Color(if (dark) night else light), accent.primary, if (dark) 0.025f else 0.018f,
    )
    val background = neutral(0xFFF4F6F9, 0xFF101218)
    val low = neutral(0xFFFFFFFF, 0xFF191D25)
    val high = neutral(0xFFFFFFFF, 0xFF272D38)
    return accent.copy(
        background = background,
        onBackground = ink,
        surface = background,
        onSurface = ink,
        surfaceDim = neutral(0xFFE5E8EE, 0xFF0D1015),
        surfaceBright = high,
        surfaceContainerLowest = neutral(0xFFFFFFFF, 0xFF0D1015),
        surfaceContainerLow = low,
        surfaceContainer = neutral(0xFFF9FAFC, 0xFF20252F),
        surfaceContainerHigh = high,
        surfaceContainerHighest = neutral(0xFFE9EDF3, 0xFF303744),
        surfaceVariant = neutral(0xFFE9EDF3, 0xFF303744),
        onSurfaceVariant = if (dark) Color(0xFFADB7C7) else Color(0xFF5B6473),
        outline = if (dark) Color(0xFF8994A5) else Color(0xFF737D8D),
        outlineVariant = if (dark) Color(0xFF39414F) else Color(0xFFDCE2EB),
        surfaceTint = Color.Transparent,
        secondaryContainer = lerp(low, accent.primary, if (dark) 0.20f else 0.10f),
        onSecondaryContainer = ink,
        inverseSurface = if (dark) Color(0xFFE9EDF3) else Color(0xFF282E39),
        inverseOnSurface = if (dark) Color(0xFF20242C) else Color(0xFFF1F3F7),
    )
}

val MeiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun text(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontSize = size.sp, lineHeight = height.sp, fontWeight = weight, letterSpacing = 0.sp,
)

private val MeiTypography = Typography(
    displayLarge = text(52, 60, FontWeight.Bold),
    displayMedium = text(42, 50, FontWeight.Bold),
    displaySmall = text(34, 42, FontWeight.Bold),
    headlineLarge = text(30, 38, FontWeight.Bold),
    headlineMedium = text(26, 34, FontWeight.Bold),
    headlineSmall = text(22, 30, FontWeight.SemiBold),
    titleLarge = text(20, 28, FontWeight.SemiBold),
    titleMedium = text(16, 24, FontWeight.SemiBold),
    titleSmall = text(14, 20, FontWeight.SemiBold),
    bodyLarge = text(16, 24),
    bodyMedium = text(14, 21),
    bodySmall = text(12, 18),
    labelLarge = text(14, 20, FontWeight.SemiBold),
    labelMedium = text(12, 18, FontWeight.Medium),
    labelSmall = text(11, 16, FontWeight.Medium),
)

@Composable
fun MusicTheme(
    seedColor: Color,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    DynamicMaterialTheme(
        seedColor = seedColor,
        isDark = isDark,
        animate = true,
        animationSpec = tween(400),
        typography = MeiTypography,
        shapes = MeiShapes,
    ) {
        MusicThemeContent(isDark = isDark, content = content)
    }
}

@Composable
private fun MusicThemeContent(isDark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = meiColorScheme(MaterialTheme.colorScheme, isDark),
        shapes = MeiShapes,
        typography = MeiTypography,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            content = content,
        )
    }
}
