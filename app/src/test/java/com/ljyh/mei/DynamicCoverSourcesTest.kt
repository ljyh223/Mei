package com.ljyh.mei.constants

import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test

class DynamicCoverSourcesTest {
    @Test
    fun `existing enabled setting enables both sources`() {
        val preferences = preferencesOf(DynamicCoverKey to true)

        assertEquals(DynamicCoverSources(appleMusic = true, netease = true), preferences.dynamicCoverSources())
    }

    @Test
    fun `source choices override the existing setting independently`() {
        val preferences = preferencesOf(
            DynamicCoverKey to true,
            AppleDynamicCoverKey to false,
        )

        assertEquals(DynamicCoverSources(appleMusic = false, netease = true), preferences.dynamicCoverSources())
    }

    @Test
    fun `explicitly disabled sources stay disabled`() {
        val preferences = preferencesOf(
            DynamicCoverKey to true,
            AppleDynamicCoverKey to false,
            NeteaseDynamicCoverKey to false,
        )

        assertEquals(DynamicCoverSources.Disabled, preferences.dynamicCoverSources())
    }

    @Test
    fun `new installations start with both sources disabled`() {
        val settings = preferencesOf().dynamicCoverSettings()

        assertEquals(false, settings.enabled)
        assertEquals(DynamicCoverSources.Disabled, settings.activeSources)
        assertEquals(DynamicCoverSources(appleMusic = true, netease = true), settings.selectedSources)
    }

    @Test
    fun `master switch disables both sources without erasing selections`() {
        val preferences = preferencesOf(
            DynamicCoverKey to true,
            DynamicCoverEnabledKey to false,
            AppleDynamicCoverKey to true,
            NeteaseDynamicCoverKey to false,
        )

        assertEquals(DynamicCoverSources.Disabled, preferences.dynamicCoverSources())
        assertEquals(
            DynamicCoverSources(appleMusic = true, netease = false),
            preferences.dynamicCoverSettings().selectedSources,
        )
    }

    @Test
    fun `split source preference enables master for users of previous screen`() {
        val preferences = preferencesOf(
            DynamicCoverKey to false,
            AppleDynamicCoverKey to true,
        )

        assertEquals(true, preferences.dynamicCoverSettings().enabled)
        assertEquals(
            DynamicCoverSources(appleMusic = true, netease = false),
            preferences.dynamicCoverSources(),
        )
    }

    @Test
    fun `master switch can enable a selected source`() {
        val preferences = preferencesOf(
            DynamicCoverEnabledKey to true,
            NeteaseDynamicCoverKey to true,
        )

        assertEquals(
            DynamicCoverSources(appleMusic = false, netease = true),
            preferences.dynamicCoverSources(),
        )
    }

    @Test
    fun `enabling a legacy disabled switch defaults to both sources`() {
        val preferences = preferencesOf(
            DynamicCoverKey to false,
            DynamicCoverEnabledKey to true,
        )

        assertEquals(
            DynamicCoverSources(appleMusic = true, netease = true),
            preferences.dynamicCoverSources(),
        )
    }
}
