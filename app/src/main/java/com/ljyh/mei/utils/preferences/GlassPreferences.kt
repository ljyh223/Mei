package com.ljyh.mei.utils.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.GlassBlurKey
import com.ljyh.mei.constants.GlassHighlightKey
import com.ljyh.mei.constants.GlassMaterialVersionKey
import com.ljyh.mei.constants.GlassReduceTransparencyKey
import com.ljyh.mei.constants.GlassOpacityKey
import com.ljyh.mei.constants.GlassRefractionKey
import com.ljyh.mei.constants.GlassSaturationKey
import com.ljyh.mei.constants.GlassStrongContentKey
import com.ljyh.mei.ui.component.glass.GlassAppearance
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val CurrentGlassMaterialVersion = 2

internal fun Preferences.glassAppearance(): GlassAppearance {
    val value = GlassAppearance(
        opacity = this[GlassOpacityKey] ?: GlassAppearance.Balanced.opacity,
        blur = this[GlassBlurKey] ?: GlassAppearance.Balanced.blur,
        refraction = this[GlassRefractionKey] ?: GlassAppearance.Balanced.refraction,
        highlight = this[GlassHighlightKey] ?: GlassAppearance.Balanced.highlight,
        saturation = this[GlassSaturationKey] ?: GlassAppearance.Balanced.saturation,
        strongContent = this[GlassStrongContentKey] ?: GlassAppearance.Balanced.strongContent,
        reduceTransparency = this[GlassReduceTransparencyKey] ?: false,
    ).normalized()
    val version = this[GlassMaterialVersionKey]
    if (version != null && version >= CurrentGlassMaterialVersion) return value
    // Upgrade complete legacy presets only. Partial or customized settings remain intact.
    val legacyPresets = if (version == 1) listOf(
        GlassAppearance(0.08f, 1.25f, 0.85f, 0.55f, 1f),
        GlassAppearance(0f, 1.25f, 0.85f, 0.55f, 1f),
        GlassAppearance.Readable,
    ) else listOf(
        GlassAppearance(0.60f, 20f, 0.25f, 0.20f, 0.65f),
        GlassAppearance(0.25f, 12f, 0.45f, 0.30f, 0.85f, strongContent = false),
        GlassAppearance(0.85f, 28f, 0.10f, 0.10f, 0.35f),
    )
    val presets = listOf(GlassAppearance.Balanced, GlassAppearance.Clear, GlassAppearance.Readable)
    val complete = listOf(GlassOpacityKey, GlassBlurKey, GlassRefractionKey,
        GlassHighlightKey, GlassSaturationKey).all { this[it] != null }
    val index = legacyPresets.indexOfFirst { value.withMaterial(it) == value }
    return if (complete && index >= 0) value.withMaterial(presets[index]) else value
}

@Composable
fun rememberGlassAppearance(): State<GlassAppearance> {
    val context = LocalContext.current
    return remember(context) {
        context.dataStore.data.map { it.glassAppearance() }.distinctUntilChanged()
    }.collectAsState(initial = GlassAppearance.Balanced)
}

internal fun MutablePreferences.setGlassAppearance(appearance: GlassAppearance) {
    val value = appearance.normalized()
    this[GlassOpacityKey] = value.opacity
    this[GlassBlurKey] = value.blur
    this[GlassRefractionKey] = value.refraction
    this[GlassHighlightKey] = value.highlight
    this[GlassSaturationKey] = value.saturation
    this[GlassStrongContentKey] = value.strongContent
    this[GlassReduceTransparencyKey] = value.reduceTransparency
    this[GlassMaterialVersionKey] = CurrentGlassMaterialVersion
}

suspend fun Context.saveGlassAppearance(appearance: GlassAppearance) {
    dataStore.edit { it.setGlassAppearance(appearance) }
}
