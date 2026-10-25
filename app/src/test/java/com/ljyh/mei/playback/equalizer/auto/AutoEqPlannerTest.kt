package com.ljyh.mei.playback.equalizer.auto

import com.ljyh.mei.playback.equalizer.NeteaseEqualizerPresets
import com.ljyh.mei.playback.equalizer.toProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoEqPlannerTest {

    private val sampleJson = """
        {"code":200,"data":[{"song_id":1417453306,"emotion_label":"治愈,中性情绪","genre_id":1000,
        "key_mode":"C:Major","mel_spectrogram_32":[-15.0169,-21.3934,-23.0872,-22.7379,-24.5007,
        -27.0557,-27.506,-27.7177,-28.3107,-30.2383,-31.4739,-31.2737,-34.3019,-35.1423,-37.3087,
        -37.6894,-36.9165,-37.4506,-38.2218,-39.5614,-44.4288,-48.0612,-48.6716,-46.3193,-44.903,
        -45.2852,-48.2064,-51.5893,-54.2276,-57.7459,-62.6225,-68.7808],"dynamic_range":11.1,
        "loudness_lufs":-8.23,"stereo_width_ratio":0.296,"repbpm":78,"chorus_loudness":-7.9984,
        "vocal_main_freq":533.81,"vocal_time_ratio":0.95,"harmonic_intensity_ratio":0.41,
        "vocal_dynamic_range":18.38,"drum_time_ratio":0.68,"rhythm_complexity":0.7,
        "vocal_energy_ratio":0.2,"drum_energy_ratio":0.12,"bass_energy_ratio":0.58,
        "other_instrument_energy_ratio":0.11}],"message":""}
    """.trimIndent()

    @Test
    fun parsesNeteasePayload() {
        val features = TrackAudioFeaturesParser.parse(sampleJson).single()
        assertEquals(1417453306L, features.songId)
        assertEquals("治愈,中性情绪", features.emotionLabel)
        assertEquals(0.58f, features.bassEnergyRatio, 1e-6f)
        assertEquals(78f, features.bpm, 1e-6f)
        assertEquals(-8.23f, features.loudnessLufs, 1e-6f)
        assertEquals(32, features.melSpectrumDb.size)
        assertTrue(features.hasAnalysis)
    }

    @Test
    fun rejectsMalformedPayload() {
        assertTrue(TrackAudioFeaturesParser.parse("").isEmpty())
        assertTrue(TrackAudioFeaturesParser.parse("""{"code":200}""").isEmpty())
        assertTrue(TrackAudioFeaturesParser.parse("not json").isEmpty())
    }

    @Test
    fun presetsUseTheNativeBandLayout() {
        assertEquals(22, NeteaseEqualizerPresets.all.size)
        assertEquals(
            listOf(31f, 63f, 125f, 250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f, 16_000f),
            NeteaseEqualizerPresets.bandFrequencies.toList(),
        )
        val bass = requireNotNull(NeteaseEqualizerPresets.byId("bass"))
        assertEquals(10, bass.gains.size)
        val profile = bass.toProfile()
        assertEquals(10, profile.filters.size)
        assertEquals(31f, profile.filters.first().frequencyHz, 1e-3f)
        assertEquals(16_000f, profile.filters.last().frequencyHz, 1e-3f)
        assertTrue(profile.filters.all { it.gainDb in -12f..12f })
    }

    @Test
    fun emotionPicksTheBaseCurve() {
        val features = TrackAudioFeaturesParser.parse(sampleJson).single()
        assertEquals("soft", AutoEqPlanner.basePresetId(features))
        val plan = AutoEqPlanner.plan(features, 0.5f)
        val soft = requireNotNull(NeteaseEqualizerPresets.byId("soft")).gains
        assertEquals(soft[0] * 0.5f, plan.filters[0].gainDb, 1e-4f)
    }

    @Test
    fun bassHeavyTrackCutsLowBands() {
        val neutral = TrackAudioFeatures(emotionLabel = "中性情绪")
        val bassHeavy = neutral.copy(bassEnergyRatio = 0.7f)
        val pop = requireNotNull(NeteaseEqualizerPresets.byId("pop")).gains
        val plan = AutoEqPlanner.plan(bassHeavy, 1f)
        val low = plan.filters[1].gainDb
        assertTrue("63 Hz should be below the pop curve", low < pop[1])
    }

    @Test
    fun loudMasterLowersInputGain() {
        val loud = TrackAudioFeatures(emotionLabel = "中性情绪", loudnessLufs = -5f)
        val plan = AutoEqPlanner.plan(loud, 1f)
        assertTrue(plan.inputGainDb <= -1.5f)
    }

    @Test
    fun bigBoostKeepsHeadroom() {
        val features = TrackAudioFeatures(emotionLabel = "中性情绪")
        val strong = requireNotNull(NeteaseEqualizerPresets.byId("strong_bass"))
        val plan = AutoEqPlanner.plan(strong, features, 1f)
        // "strong_bass" boosts +8.3 dB, so at least 6.3 dB of headroom is expected.
        assertTrue("input gain ${plan.inputGainDb}", plan.inputGainDb <= -6.3f)
        assertTrue(plan.filters.all { it.gainDb <= 12f && it.gainDb >= -12f })
    }
}
