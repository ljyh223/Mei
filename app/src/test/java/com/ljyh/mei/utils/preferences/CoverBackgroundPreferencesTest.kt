package com.ljyh.mei.utils.preferences

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.ljyh.mei.constants.CoverBackgroundConfidenceKey
import com.ljyh.mei.constants.CoverBackgroundPresetKey
import com.ljyh.mei.constants.CoverBackgroundToleranceKey
import com.ljyh.mei.constants.CoverMaximumTextureKey
import com.ljyh.mei.utils.image.CoverBackgroundConfig
import com.ljyh.mei.utils.image.CoverBackgroundPreset
import org.junit.Assert.*
import org.junit.Test

class CoverBackgroundPreferencesTest {
    @Test fun missingPreferencesKeepTheCurrentAlgorithmAsStandard() {
        assertEquals(CoverBackgroundSettingsState(), emptyPreferences().coverBackgroundSettings())
    }

    @Test fun oldCustomThresholdsSurviveWithoutAPresetKey() {
        val saved = mutablePreferencesOf(CoverBackgroundToleranceKey to 0.1f, CoverBackgroundConfidenceKey to 0.8f)
        val result = saved.coverBackgroundSettings()
        assertEquals(CoverBackgroundPreset.Custom, result.preset)
        assertEquals(0.1f, result.config.colorTolerance, 0f)
        assertEquals(0.8f, result.config.minimumBackgroundConfidence, 0f)
        assertEquals(0.008f, result.config.maximumBandTexture, 0f)
    }

    @Test fun everyPresetRoundTripsAllThresholdsTogether() {
        val saved = mutablePreferencesOf()
        CoverBackgroundPreset.entries.filter { it.config != null }.forEach { preset ->
            saved.setCoverBackgroundSettings(preset.config!!, preset)
            assertEquals(CoverBackgroundSettingsState(preset.config!!, preset), saved.coverBackgroundSettings())
        }
    }

    @Test fun explicitCustomModeRemainsCustomEvenWithStandardValues() {
        val saved = mutablePreferencesOf()
        saved.setCoverBackgroundSettings(CoverBackgroundConfig(), CoverBackgroundPreset.Custom)
        assertEquals(CoverBackgroundPreset.Custom, saved.coverBackgroundSettings().preset)
    }

    @Test fun customExceptionRulesAndAdvancedValuesRoundTrip() {
        val config = CoverBackgroundConfig(
            maximumBandTexture = 0.0123f, maximumBandGradient = 0.0567f,
            maximumSolidGap = 0.04f, maximumSoftGap = 0.23f,
            solidSideSlack = 0.2f, singleEdgeStrongMatch = 0.95f, threeSideMinimumMatch = 0.42f,
            allowSingleOccupiedSide = false, allowThreeStrongSides = false,
        )
        val saved = mutablePreferencesOf()
        saved.setCoverBackgroundSettings(config, CoverBackgroundPreset.Custom)
        assertEquals(config, saved.coverBackgroundSettings().config)
    }

    @Test fun corruptedStoredValuesAreNormalizedWithoutLosingOtherChoices() {
        val saved = mutablePreferencesOf(
            CoverBackgroundPresetKey to "unknown", CoverBackgroundToleranceKey to Float.NaN,
            CoverMaximumTextureKey to 999f, CoverBackgroundConfidenceKey to 0.77f,
        )
        val result = saved.coverBackgroundSettings()
        assertEquals(0.075f, result.config.colorTolerance, 0f)
        assertEquals(0.1f, result.config.maximumBandTexture, 0f)
        assertEquals(0.77f, result.config.minimumBackgroundConfidence, 0f)
        assertEquals(CoverBackgroundPreset.Custom, result.preset)
    }
}
