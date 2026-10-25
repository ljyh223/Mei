package com.ljyh.mei.playback.equalizer

/**
 * NetEase's own 10-band curves, taken from the 9.6.05 client's `assets/audioeffect/eq` 下的 json 预设.
 *
 * The band centres and the ±12 dB range come from the native engine lookup table
 * (`libnesmarteffects.so`, 31/63/125/250/500/1k/2k/4k/8k/16k Hz). Keeping the same layout means
 * an imported `eqBands` payload, a built-in preset and our own auto-EQ curve are interchangeable.
 */
object NeteaseEqualizerPresets {
    val bandFrequencies = floatArrayOf(
        31f, 63f, 125f, 250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f, 16_000f,
    )
    /** 1/3-octave-ish peak filters: each band spans roughly one octave around its centre. */
    const val BAND_Q = 1.41f
    const val MAX_GAIN_DB = 12f

    data class Definition(val id: String, val name: String, val gains: List<Float>)

    private fun definition(id: String, name: String, vararg gains: Float) =
        Definition(id, name, gains.toList())

    val all: List<Definition> = listOf(
        definition("acg", "ACG", 4f, 6.19f, 3.5f, -0.5f, -0.8f, 2f, 5.19f, 1f, 1.5f, 4f),
        definition("ballad", "抒情", 0f, 3f, 0f, -0.8f, 1.5f, 4.8f, 5.8f, 3.2f, 0.5f, 2f),
        definition("bass", "低音", 6f, 4.5f, 6.2f, 2.09f, 0f, 0f, 0f, 0f, 0f, 0f),
        definition("bass_treble", "低音+高音", 6f, 5f, 6.2f, 1.5f, 0f, 0f, 1f, 3.2f, 4f, 0f),
        definition("blues", "蓝调", 2f, 6f, 4.5f, 0f, -2.2f, -1.5f, 2f, 2.5f, 1.79f, 3f),
        definition("chinese", "华语", 4f, 2f, 2f, -0.5f, -1f, 3.5f, 4.5f, 1f, 1f, 3f),
        definition("classical", "古典", 4.5f, 4f, 3f, 2.29f, -1f, -1.2f, 0f, 1.79f, 3f, 4.5f),
        definition("country", "乡村", 0f, 2.2f, 3f, 0.6f, 0f, 2.29f, 3.09f, 1f, -0.19f, -0.3f),
        definition("dance", "舞曲", 4f, 5.8f, 7f, 0f, 1.5f, 3f, 4.59f, 4f, 3.2f, 0f),
        definition("downtempo", "慢速", 2f, 0f, 0.6f, 0f, -0.8f, 0f, 0f, 0f, -1f, 1f),
        definition("electronic", "电子", 5f, 6.09f, 5.2f, 0f, -1.6f, 1.5f, 0f, 1f, 4.57f, 3.77f),
        definition("heavy_metal", "重金属", -2.5f, 5.29f, 4.49f, -2f, -2.09f, -1.6f, 2.09f, 3f, 1.11f, 4f),
        definition("hiphop", "嘻哈", 5.6f, 5.09f, 4f, 0.5f, -2f, 1.2f, 3.79f, 0.5f, 3.5f, 4f),
        definition("jazz", "爵士", 3.2f, 3f, 1.5f, 2.4f, -1.5f, -1f, 0f, 1.6f, 2f, 4.5f),
        definition("live", "现场", 5.59f, 5f, 6f, 0f, -1f, 0f, 3.09f, 4.5f, 6f, 5f),
        definition("mid", "中频", -2f, -3f, -3.5f, 0f, 1.29f, 4.5f, 3.2f, 2f, -1.5f, -2f),
        definition("pop", "流行", -1.5f, -1f, 0f, 1.79f, 4f, 3.79f, 1.79f, 0.5f, -1f, 1f),
        definition("rock", "摇滚", 4.5f, 3.09f, 3.4f, 1.2f, 0f, -1f, 0f, 1.5f, 2.79f, 4.5f),
        definition("soft", "轻柔", 0.5f, 0.3f, 0f, 0f, 0f, 0f, -0.5f, -2.08f, -2.79f, 0f),
        definition("soft_bass", "轻柔低音", 3.5f, 2.2f, 1f, 0.6f, 0f, 0f, 0f, -2f, -2.5f, -2f),
        definition("soft_treble", "轻柔高音", -3f, -1.88f, 0f, 0f, 0f, 0f, -0.5f, -1.28f, 3.2f, 2f),
        definition("strong_bass", "重低音", 6f, 5.5f, 8.3f, 2.07f, 0f, 0f, 0f, 0f, 0f, 0f),
    )

    fun byId(id: String): Definition? = all.firstOrNull { it.id == id }
}

fun NeteaseEqualizerPresets.Definition.toProfile(): EqualizerProfile = EqualizerProfile(
    enabled = true,
    presetId = id,
    filters = gains.mapIndexed { index, gain ->
        EqFilter(
            id = index + 1,
            type = FilterType.PEAK,
            frequencyHz = NeteaseEqualizerPresets.bandFrequencies[index],
            gainDb = gain,
            q = NeteaseEqualizerPresets.BAND_Q,
        )
    },
)
