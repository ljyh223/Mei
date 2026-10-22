package com.ljyh.mei.utils.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.GlassBlurKey
import com.ljyh.mei.constants.GlassHighlightKey
import com.ljyh.mei.constants.GlassOpacityKey
import com.ljyh.mei.constants.GlassRefractionKey
import com.ljyh.mei.constants.GlassSaturationKey
import com.ljyh.mei.constants.GlassStrongContentKey
import com.ljyh.mei.ui.component.glass.GlassAppearance
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun Preferences.glassAppearance(): GlassAppearance = GlassAppearance(
    opacity = this[GlassOpacityKey] ?: GlassAppearance.Balanced.opacity,
    blur = this[GlassBlurKey] ?: GlassAppearance.Balanced.blur,
    refraction = this[GlassRefractionKey] ?: GlassAppearance.Balanced.refraction,
    highlight = this[GlassHighlightKey] ?: GlassAppearance.Balanced.highlight,
    saturation = this[GlassSaturationKey] ?: GlassAppearance.Balanced.saturation,
    strongContent = this[GlassStrongContentKey] ?: GlassAppearance.Balanced.strongContent,
).normalized()

@Composable
fun rememberGlassAppearance(): State<GlassAppearance> {
    val context = LocalContext.current
    return remember(context) {
        context.dataStore.data.map { it.glassAppearance() }.distinctUntilChanged()
    }.collectAsState(initial = GlassAppearance.Balanced)
}

suspend fun Context.saveGlassAppearance(appearance: GlassAppearance) {
    val value = appearance.normalized()
    dataStore.edit {
        it[GlassOpacityKey] = value.opacity
        it[GlassBlurKey] = value.blur
        it[GlassRefractionKey] = value.refraction
        it[GlassHighlightKey] = value.highlight
        it[GlassSaturationKey] = value.saturation
        it[GlassStrongContentKey] = value.strongContent
    }
}
