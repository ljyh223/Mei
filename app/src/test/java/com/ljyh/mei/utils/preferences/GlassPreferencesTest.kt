package com.ljyh.mei.utils.preferences

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.ljyh.mei.constants.GlassBlurKey
import com.ljyh.mei.constants.GlassOpacityKey
import com.ljyh.mei.constants.GlassStrongContentKey
import com.ljyh.mei.ui.component.glass.GlassAppearance
import org.junit.Assert.assertEquals
import org.junit.Test

class GlassPreferencesTest {
    @Test
    fun existingInstallWithoutGlassKeysUsesReadableDefaults() {
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
}
