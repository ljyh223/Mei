package com.ljyh.mei.playback.equalizer

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class EqualizerPreset(val id: String, val name: String, val profile: EqualizerProfile)

object EqualizerPresets {
    private fun filter(id: Int, type: FilterType, frequency: Float, gain: Float, q: Float = 0.7f) =
        EqFilter(id, type, frequency, gain, q)

    val builtIn = listOf(
        EqualizerPreset("flat", "原声", EqualizerProfile(presetId = "flat")),
        EqualizerPreset("studio", "录音室参考", EqualizerProfile(presetId = "studio", inputGainDb = -2f, filters = listOf(
            filter(1, FilterType.LOW_SHELF, 90f, 1.5f),
            filter(2, FilterType.PEAK, 250f, -1.2f, 1.1f),
            filter(3, FilterType.PEAK, 3_000f, 0.8f, 1.0f),
            filter(4, FilterType.HIGH_SHELF, 9_000f, 1.2f),
        ))),
        EqualizerPreset("mix", "混音校准", EqualizerProfile(presetId = "mix", inputGainDb = -2f, filters = listOf(
            filter(1, FilterType.LOW_SHELF, 100f, -1f),
            filter(2, FilterType.PEAK, 350f, -1.5f, 1.2f),
            filter(3, FilterType.PEAK, 1_800f, 1f, 1.2f),
            filter(4, FilterType.PEAK, 4_500f, 1f, 1.4f),
            filter(5, FilterType.HIGH_SHELF, 10_000f, -0.5f),
        ))),
        EqualizerPreset("clean_low_mid", "低中频清理", EqualizerProfile(presetId = "clean_low_mid", inputGainDb = -1.5f, filters = listOf(
            filter(1, FilterType.HIGH_PASS, 35f, 0f),
            filter(2, FilterType.PEAK, 180f, -1.5f, 1.1f),
            filter(3, FilterType.PEAK, 350f, -2f, 1.0f),
            filter(4, FilterType.HIGH_SHELF, 8_000f, 0.5f),
        ))),
        EqualizerPreset("soft_high", "柔顺高频", EqualizerProfile(presetId = "soft_high", inputGainDb = -1f, filters = listOf(
            filter(1, FilterType.LOW_SHELF, 100f, 1f),
            filter(2, FilterType.PEAK, 3_500f, -1f, 1.0f),
            filter(3, FilterType.PEAK, 7_500f, -2f, 1.5f),
            filter(4, FilterType.HIGH_SHELF, 12_000f, -1f),
        ))),
        EqualizerPreset("air", "空气感与细节", EqualizerProfile(presetId = "air", inputGainDb = -3f, filters = listOf(
            filter(1, FilterType.LOW_SHELF, 100f, 1f),
            filter(2, FilterType.PEAK, 300f, -1f, 1f),
            filter(3, FilterType.PEAK, 3_500f, 1.2f, 1.1f),
            filter(4, FilterType.HIGH_SHELF, 9_000f, 2.5f),
        ))),
        EqualizerPreset("bass", "超低频延伸", EqualizerProfile(presetId = "bass", inputGainDb = -5f, filters = listOf(
            filter(1, FilterType.LOW_SHELF, 65f, 4.5f, 0.7f),
            filter(2, FilterType.PEAK, 160f, 1f, 0.9f),
            filter(3, FilterType.PEAK, 350f, -1f, 1.1f),
            filter(4, FilterType.HIGH_SHELF, 9_000f, 0.5f),
        ))),
        EqualizerPreset("vocal", "清澈人声", EqualizerProfile(presetId = "vocal", inputGainDb = -3f, filters = listOf(
            filter(1, FilterType.LOW_SHELF, 100f, -1.5f),
            filter(2, FilterType.PEAK, 300f, -1.5f, 1.0f),
            filter(3, FilterType.PEAK, 1_500f, 1f, 1.0f),
            filter(4, FilterType.PEAK, 3_500f, 2.5f, 1.1f),
            filter(5, FilterType.HIGH_SHELF, 10_000f, 1f),
        ))),
    )

    fun decodeCustom(raw: String): List<EqualizerPreset> = raw.lineSequence().mapNotNull { line ->
        val parts = line.split('\t', limit = 3)
        if (parts.size != 3 || !parts[0].startsWith("user_")) return@mapNotNull null
        try {
            val name = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
            val profile = EqualizerProfileCodec.decode(parts[2]) ?: return@mapNotNull null
            EqualizerPreset(parts[0], name, profile)
        } catch (_: IllegalArgumentException) {
            null
        }
    }.take(50).toList()

    fun encodeCustom(presets: List<EqualizerPreset>): String = presets.take(50).joinToString("\n") {
        "${it.id}\t${URLEncoder.encode(it.name, StandardCharsets.UTF_8.name())}\t${EqualizerProfileCodec.encode(it.profile)}"
    }
}
