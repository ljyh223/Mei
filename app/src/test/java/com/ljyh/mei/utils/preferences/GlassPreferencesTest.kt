package com.ljyh.mei.utils.preferences

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.ljyh.mei.constants.GlassReduceTransparencyKey
import com.ljyh.mei.constants.GlassMaterialVersionKey
import com.ljyh.mei.constants.GlassRefractionKey
import com.ljyh.mei.constants.GlassHighlightKey
import com.ljyh.mei.constants.GlassSaturationKey
import com.ljyh.mei.constants.LiquidGlassEnabledKey
import com.ljyh.mei.constants.GlassBlurKey
import com.ljyh.mei.constants.GlassOpacityKey
import com.ljyh.mei.constants.GlassStrongContentKey
import com.ljyh.mei.ui.component.glass.GlassAppearance
import org.junit.Assert.assertEquals
import org.junit.Test

class GlassPreferencesTest {
    @Test
    fun existingInstallWithoutGlassKeysUsesLiquidDefaults() {
        assertEquals(GlassAppearance.Balanced, emptyPreferences().glassAppearance())
    }

    @Test
    fun partialPreferencesPreserveChoicesAndDefaultMissingFields() {
        val saved = mutablePreferencesOf(GlassOpacityKey to 0.25f, GlassStrongContentKey to false)
        assertEquals(GlassAppearance.Balanced.copy(opacity = 0.25f, strongContent = false), saved.glassAppearance())
    }

    @Test
    fun corruptedPreferencesAreSanitizedOnRead() {
        val saved = mutablePreferencesOf(GlassOpacityKey to Float.NaN, GlassBlurKey to 999f)
        assertEquals(GlassAppearance.Balanced.copy(blur = 40f), saved.glassAppearance())
    }

    @Test
    fun completeLegacyPresetUpgradesWithoutOverwritingCustomMaterials() {
        val saved = mutablePreferencesOf(GlassOpacityKey to 0.60f, GlassBlurKey to 20f,
            GlassRefractionKey to 0.25f, GlassHighlightKey to 0.20f, GlassSaturationKey to 0.65f,
            GlassStrongContentKey to true)
        assertEquals(GlassAppearance.Balanced, saved.glassAppearance())
        saved[GlassOpacityKey] = 0.61f
        assertEquals(GlassAppearance(0.61f, 20f, 0.25f, 0.20f, 0.65f), saved.glassAppearance())
    }

    @Test
    fun newlySavedMaterialIsNeverReinterpretedAsLegacyPreset() {
        val appearance = GlassAppearance(0.60f, 20f, 0.25f, 0.20f, 0.65f)
        val saved = emptyPreferences().toMutablePreferences()
        saved.setGlassAppearance(appearance)
        assertEquals(2, saved[GlassMaterialVersionKey])
        assertEquals(appearance, saved.toPreferences().glassAppearance())
    }

    @Test
    fun reducedTransparencySurvivesSaveAndReloadWithLiquidGlassDisabled() {
        val saved = mutablePreferencesOf(LiquidGlassEnabledKey to false)
        val appearance = GlassAppearance.Clear.copy(reduceTransparency = true, strongContent = false)
        saved.setGlassAppearance(appearance)
        assertEquals(false, saved[LiquidGlassEnabledKey])
        val restored = saved.toPreferences().glassAppearance()
        assertEquals(appearance, restored)
        assertEquals(1f, restored.tintOpacity(liveGlass = false), 0f)
    }

    @Test
    fun previousLiquidPresetUpgradesWhilePreservingIndependentOptions() {
        val saved = mutablePreferencesOf(GlassMaterialVersionKey to 1,
            GlassOpacityKey to 0.08f, GlassBlurKey to 1.25f,
            GlassRefractionKey to 0.85f, GlassHighlightKey to 0.55f, GlassSaturationKey to 1f,
            GlassStrongContentKey to false, GlassReduceTransparencyKey to true)
        assertEquals(GlassAppearance.Balanced.copy(strongContent = false, reduceTransparency = true),
            saved.glassAppearance())
    }

    @Test
    fun customizedLiquidMaterialIsNotReplacedOnUpgrade() {
        val saved = mutablePreferencesOf(GlassMaterialVersionKey to 1,
            GlassOpacityKey to 0.11f, GlassBlurKey to 1.25f,
            GlassRefractionKey to 0.85f, GlassHighlightKey to 0.55f, GlassSaturationKey to 1f,
            GlassStrongContentKey to false)
        assertEquals(GlassAppearance(opacity = 0.11f, blur = 1.25f, strongContent = false),
            saved.glassAppearance())
    }

}
