package com.ljyh.mei.ui.component.glass

import android.os.Build
import androidx.compose.runtime.Composable

/** Page background intent, independent of the user's optical-material preference. */
enum class MeiTopBarBackground { Auto, Transparent, Solid }

internal data class MeiTopBarBackgroundState(val usesGlass: Boolean, val opacity: Float)

/** Reduced transparency wins over page intent; ordinary controls keep their own solid fallback. */
internal fun resolveMeiTopBarBackground(
    mode: MeiTopBarBackground,
    glassAvailable: Boolean,
    reduceTransparency: Boolean,
    glassProgress: Float = 1f,
): MeiTopBarBackgroundState {
    fun Float.opacity() = if (isFinite()) coerceIn(0f, 1f) else 1f
    return when {
        reduceTransparency || mode == MeiTopBarBackground.Solid -> MeiTopBarBackgroundState(false, 1f)
        mode == MeiTopBarBackground.Transparent -> MeiTopBarBackgroundState(false, 0f)
        glassAvailable -> MeiTopBarBackgroundState(true, glassProgress.opacity())
        else -> MeiTopBarBackgroundState(false, 1f)
    }
}

/** Pages choose whether their hero has passed; persistent artwork pages leave this false. */
@Composable
fun meiArtworkTopBarBackground(artworkPassed: Boolean = false): MeiTopBarBackground = when {
    artworkPassed -> MeiTopBarBackground.Solid
    LocalMeiGlassEnabled.current && Build.VERSION.SDK_INT >= 33 -> MeiTopBarBackground.Auto
    else -> MeiTopBarBackground.Transparent
}
