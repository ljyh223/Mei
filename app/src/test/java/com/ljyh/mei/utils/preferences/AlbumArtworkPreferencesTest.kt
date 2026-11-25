package com.ljyh.mei.utils.preferences

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.ljyh.mei.constants.AlbumArtworkMode
import com.ljyh.mei.constants.AlbumArtworkModeKey
import com.ljyh.mei.constants.AlbumModelMode
import com.ljyh.mei.constants.AlbumModelModeKey
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumArtworkPreferencesTest {
    @Test fun oldAndInvalidPreferencesDefaultToAutomaticModelSelection() {
        assertEquals(AlbumArtworkPreferences(), emptyPreferences().albumArtworkPreferences())
        assertEquals(AlbumArtworkPreferences(AlbumArtworkMode.Model),
            mutablePreferencesOf(AlbumArtworkModeKey to "Model").albumArtworkPreferences())
        assertEquals(AlbumArtworkPreferences(), mutablePreferencesOf(
            AlbumArtworkModeKey to "broken", AlbumModelModeKey to "broken").albumArtworkPreferences())
    }
    @Test fun fixedModelIsSavedWithModelCategoryAndSurvivesReload() {
        val saved = mutablePreferencesOf(AlbumArtworkModeKey to "Motion")
        saved.setAlbumModelMode(AlbumModelMode.CD)
        assertEquals(AlbumArtworkPreferences(AlbumArtworkMode.Model, AlbumModelMode.CD),
            saved.toPreferences().albumArtworkPreferences())
    }
    @Test fun reenteringModelCategoryDefaultsToYearSelection() {
        val saved = emptyPreferences().toMutablePreferences()
        saved.setAlbumModelMode(AlbumModelMode.Cassette)
        saved.setAlbumArtworkMode(AlbumArtworkMode.Model)
        assertEquals(AlbumModelMode.Cassette, saved.albumArtworkPreferences().model)
        saved.setAlbumArtworkMode(AlbumArtworkMode.Auto)
        saved.setAlbumArtworkMode(AlbumArtworkMode.Model)
        assertEquals(AlbumArtworkPreferences(AlbumArtworkMode.Model), saved.albumArtworkPreferences())
    }
}
