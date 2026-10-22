package com.ljyh.mei.playback.equalizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningBundleTest {

    private val eq = EqualizerProfile(
        enabled = true,
        presetId = "custom",
        inputGainDb = -3f,
        filters = listOf(
            EqFilter(1, FilterType.PEAK, 100f, 4f, 0.9f),
            EqFilter(2, FilterType.HIGH_SHELF, 8_000f, -2f),
        ),
    )

    @Test
    fun roundTripsBothProfiles() {
        val bundle = TuningBundle("我的调音", eq, ReverbProfile.Hall)
        val raw = TuningBundleCodec.encode(bundle)
        val decoded = assertNotNullCodec(raw)
        assertEquals("我的调音", decoded.name)
        assertEquals(eq.inputGainDb, decoded.eq.inputGainDb, 1e-5f)
        assertEquals(eq.filters.size, decoded.eq.filters.size)
        assertEquals(ReverbProfile.Hall.roomSize, decoded.reverb.roomSize, 1e-5f)
        assertEquals("hall", decoded.reverb.presetId)
    }

    @Test
    fun rejectsForeignOrMalformedPayloads() {
        assertNull(TuningBundleCodec.decode("not json"))
        assertNull(TuningBundleCodec.decode("{}"))
        assertNull(TuningBundleCodec.decode("""{"format":"other","version":1,"eq":{"codec":"v1;0;x;0;0"},"reverb":{"codec":"v1;0;off;0.5;0.5;1;0.2;0.8;18"}}"""))
        assertNull(TuningBundleCodec.decode("""{"format":"mei-tuning"}"""))
    }

    @Test
    fun rejectsNewerVersions() {
        val raw = TuningBundleCodec.encode(TuningBundle("x", eq, ReverbProfile.Room))
            .replace("\"version\":1", "\"version\":99")
        assertNull(TuningBundleCodec.decode(raw))
    }

    @Test
    fun detectsBundleJson() {
        assertTrue(TuningBundleCodec.looksLikeBundle(TuningBundleCodec.encode(
            TuningBundle("x", eq, ReverbProfile.Off)
        )))
        assertTrue(TuningBundleCodec.looksLikeBundle("{\"a\":1}"))
        assertTrue(!TuningBundleCodec.looksLikeBundle("Preamp: -2 dB\nFilter 1: ON PK Fc 100 Hz Gain 1 dB Q 1"))
    }

    private fun assertNotNullCodec(raw: String): TuningBundle {
        val decoded = TuningBundleCodec.decode(raw)
        assertNotNull(decoded)
        return decoded!!
    }
}
