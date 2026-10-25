package com.ljyh.mei.playback.equalizer.auto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Per-song analysis returned by NetEase's smart-sound-effect endpoint.
 *
 * All ratios are 0..1 energy shares, [loudnessLufs] is integrated loudness, [dynamicRange] is LU,
 * [melSpectrumDb] is the 32-band mel spectrum from low to high frequency. Fields default to 0 so a
 * partial response still produces a usable profile.
 */
data class TrackAudioFeatures(
    val songId: Long = 0,
    val emotionLabel: String = "",
    val genreId: Int = 0,
    val keyMode: String = "",
    val melSpectrumDb: List<Float> = emptyList(),
    val dynamicRange: Float = 0f,
    val loudnessLufs: Float = 0f,
    val stereoWidthRatio: Float = 0f,
    val bpm: Float = 0f,
    val chorusLoudness: Float = 0f,
    val vocalMainFreq: Float = 0f,
    val vocalTimeRatio: Float = 0f,
    val harmonicIntensityRatio: Float = 0f,
    val vocalDynamicRange: Float = 0f,
    val drumTimeRatio: Float = 0f,
    val rhythmComplexity: Float = 0f,
    val vocalEnergyRatio: Float = 0f,
    val drumEnergyRatio: Float = 0f,
    val bassEnergyRatio: Float = 0f,
    val otherInstrumentEnergyRatio: Float = 0f,
) {
    val hasAnalysis: Boolean get() = bassEnergyRatio > 0f || loudnessLufs != 0f
}

object TrackAudioFeaturesParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String): List<TrackAudioFeatures> = runCatching {
        val root = json.parseToJsonElement(raw).jsonObject
        root["data"]?.jsonArray?.mapNotNull { element ->
            (element as? JsonObject)?.let(::toFeatures)
        }.orEmpty()
    }.getOrDefault(emptyList())

    private fun toFeatures(item: JsonObject): TrackAudioFeatures {
        fun float(key: String): Float =
            item[key]?.jsonPrimitive?.contentOrNull?.toFloatOrNull() ?: 0f
        fun int(key: String): Int =
            item[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
        fun long(key: String): Long =
            item[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
        fun text(key: String): String =
            item[key]?.jsonPrimitive?.contentOrNull.orEmpty()

        val spectrum = item["mel_spectrogram_32"]?.jsonArray?.mapNotNull {
            it.jsonPrimitive.contentOrNull?.toFloatOrNull()
        }.orEmpty()

        return TrackAudioFeatures(
            songId = long("song_id"),
            emotionLabel = text("emotion_label"),
            genreId = int("genre_id"),
            keyMode = text("key_mode"),
            melSpectrumDb = spectrum,
            dynamicRange = float("dynamic_range"),
            loudnessLufs = float("loudness_lufs"),
            stereoWidthRatio = float("stereo_width_ratio"),
            bpm = float("repbpm"),
            chorusLoudness = float("chorus_loudness"),
            vocalMainFreq = float("vocal_main_freq"),
            vocalTimeRatio = float("vocal_time_ratio"),
            harmonicIntensityRatio = float("harmonic_intensity_ratio"),
            vocalDynamicRange = float("vocal_dynamic_range"),
            drumTimeRatio = float("drum_time_ratio"),
            rhythmComplexity = float("rhythm_complexity"),
            vocalEnergyRatio = float("vocal_energy_ratio"),
            drumEnergyRatio = float("drum_energy_ratio"),
            bassEnergyRatio = float("bass_energy_ratio"),
            otherInstrumentEnergyRatio = float("other_instrument_energy_ratio"),
        )
    }
}
