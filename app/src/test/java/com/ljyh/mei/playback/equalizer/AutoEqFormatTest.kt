package com.ljyh.mei.playback.equalizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoEqFormatTest {

    private val sample = """
        # AutoEQ style file
        Preamp: -6.8 dB
        Filter 1: ON PK Fc 21 Hz Gain 5.5 dB Q 0.71
        Filter 2: ON LSC Fc 105 Hz Gain 3.0 dB Q 0.70
        Filter 3: ON PK Fc 1.2 kHz Gain -2.5 dB Q 1.4
        Filter 4: OFF HSC Fc 10000 Hz Gain 2.0 dB Q 0.70
        Filter 5: ON HP Fc 30 Hz Q 0.7
    """.trimIndent()

    @Test
    fun parsesPreampAndFilters() {
        val profile = assertNotNullProfiles()
        assertEquals(-6.8f, profile.inputGainDb, 0.01f)
        assertEquals(5, profile.filters.size)
        assertTrue(profile.enabled)
    }

    @Test
    fun mapsFilterTypesAndUnits() {
        val profile = assertNotNullProfiles()
        assertEquals(FilterType.PEAK, profile.filters[0].type)
        assertEquals(21f, profile.filters[0].frequencyHz, 0.01f)
        assertEquals(5.5f, profile.filters[0].gainDb, 0.01f)
        assertEquals(FilterType.LOW_SHELF, profile.filters[1].type)
        assertEquals(1200f, profile.filters[2].frequencyHz, 0.01f)
        assertEquals(FilterType.HIGH_SHELF, profile.filters[3].type)
        assertEquals(false, profile.filters[3].enabled)
        assertEquals(FilterType.HIGH_PASS, profile.filters[4].type)
        assertEquals(0f, profile.filters[4].gainDb, 0.01f)
    }

    @Test
    fun rejectsTextWithoutFilters() {
        assertNull(AutoEqFormat.parse("Preamp: -3 dB\nnothing here"))
        assertNull(AutoEqFormat.parse(""))
    }

    @Test
    fun malformedLinesAreSkippedButValidOnesKept() {
        val profile = AutoEqFormat.parse(
            """
            Filter 1: ON PK Fc abc Hz Gain 1 dB Q 1
            Filter 2: ON PK Fc 500 Hz Gain 1 dB Q 1
            Filter 3: ON XX Fc 800 Hz Gain 1 dB Q 1
            """.trimIndent()
        )
        assertNotNull(profile)
        assertEquals(1, profile!!.filters.size)
        assertEquals(500f, profile.filters[0].frequencyHz, 0.01f)
    }

    @Test
    fun exportRoundTrips() {
        val original = EqualizerProfile(
            enabled = true,
            presetId = "custom",
            inputGainDb = -4f,
            filters = listOf(
                EqFilter(1, FilterType.PEAK, 60f, 3.5f, 0.9f),
                EqFilter(2, FilterType.LOW_SHELF, 120f, -1.5f, 0.7f),
                EqFilter(3, FilterType.HIGH_SHELF, 9_000f, 2f, 0.7f),
                EqFilter(4, FilterType.LOW_PASS, 18_000f, 0f, 0.7f, enabled = false),
            ),
        )
        val text = AutoEqFormat.export(original)
        val parsed = AutoEqFormat.parse(text)
        assertNotNull(parsed)
        assertEquals(original.inputGainDb, parsed!!.inputGainDb, 0.01f)
        assertEquals(original.filters.size, parsed.filters.size)
        original.filters.forEachIndexed { index, filter ->
            assertEquals(filter.type, parsed.filters[index].type)
            assertEquals(filter.frequencyHz, parsed.filters[index].frequencyHz, 0.5f)
            assertEquals(filter.enabled, parsed.filters[index].enabled)
        }
    }

    @Test
    fun preampIsClampedByProfileNormalization() {
        val profile = AutoEqFormat.parse("Preamp: -40 dB\nFilter 1: ON PK Fc 100 Hz Gain 0 dB Q 1")
        assertNotNull(profile)
        assertTrue(profile!!.inputGainDb >= -24f)
    }

    private fun assertNotNullProfiles(): EqualizerProfile {
        val profile = AutoEqFormat.parse(sample)
        assertNotNull(profile)
        return profile!!
    }
}
