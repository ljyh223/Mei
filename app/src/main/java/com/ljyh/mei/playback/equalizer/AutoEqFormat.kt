package com.ljyh.mei.playback.equalizer

/**
 * Reader/writer for the AutoEQ "ParametricEQ.txt" format used by the AutoEQ project and by
 * Salt Player's headphone-correction import:
 *
 * ```
 * Preamp: -6.8 dB
 * Filter 1: ON PK Fc 21 Hz Gain 5.5 dB Q 0.71
 * Filter 2: ON LSC Fc 105 Hz Gain 3.0 dB Q 0.70
 * ```
 */
object AutoEqFormat {
    private val filterLine = Regex(
        """^Filter\s+(\d+)\s*:\s*(ON|OFF)\s+(\w+)\s+Fc\s+([\d.]+)\s*(k?Hz)\s*(?:Gain\s+([-\d.]+)\s*dB\s*)?(?:Q\s+([\d.]+))?""",
        RegexOption.IGNORE_CASE,
    )
    private val preampLine = Regex("""^Preamp\s*:\s*([-\d.]+)\s*dB""", RegexOption.IGNORE_CASE)

    /** Returns null when the text contains no usable filter line. */
    fun parse(text: String, presetId: String = "autoeq"): EqualizerProfile? {
        var preamp = 0f
        val filters = mutableListOf<EqFilter>()
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) return@forEach
            preampLine.find(line)?.let { match ->
                preamp = match.groupValues[1].toFloatOrNull() ?: preamp
                return@forEach
            }
            val match = filterLine.find(line) ?: return@forEach
            val type = typeOf(match.groupValues[3]) ?: return@forEach
            val frequency = match.groupValues[4].toFloatOrNull() ?: return@forEach
            val scale = if (match.groupValues[5].equals("kHz", ignoreCase = true)) 1000f else 1f
            val gain = match.groupValues[6].toFloatOrNull() ?: 0f
            val q = match.groupValues[7].toFloatOrNull() ?: 0.707f
            filters += EqFilter(
                id = filters.size + 1,
                type = type,
                frequencyHz = frequency * scale,
                gainDb = gain,
                q = q,
                enabled = match.groupValues[2].equals("ON", ignoreCase = true),
            )
        }
        if (filters.isEmpty()) return null
        return EqualizerProfile(
            enabled = true,
            presetId = presetId,
            inputGainDb = preamp,
            filters = filters,
        ).normalized()
    }

    /** Emits a file AutoEQ and other parametric-EQ importers can read back. */
    fun export(profile: EqualizerProfile): String {
        val safe = profile.normalized()
        return buildString {
            append("# Exported by Mei\n")
            if (safe.inputGainDb != 0f) append("Preamp: ").append(format(safe.inputGainDb)).append(" dB\n")
            safe.filters.forEachIndexed { index, filter ->
                val name = when (filter.type) {
                    FilterType.PEAK -> "PK"
                    FilterType.LOW_SHELF -> "LSC"
                    FilterType.HIGH_SHELF -> "HSC"
                    FilterType.LOW_PASS -> "LPQ"
                    FilterType.HIGH_PASS -> "HPQ"
                }
                append("Filter ").append(index + 1).append(": ")
                append(if (filter.enabled) "ON" else "OFF").append(' ')
                append(name).append(" Fc ").append(format(filter.frequencyHz)).append(" Hz")
                if (filter.type.usesGain) {
                    append(" Gain ").append(format(filter.gainDb)).append(" dB")
                }
                append(" Q ").append(format(filter.q)).append('\n')
            }
        }
    }

    private fun typeOf(token: String): FilterType? = when (token.uppercase()) {
        "PK", "PEAK" -> FilterType.PEAK
        "LSC", "LS", "LOWSHELF" -> FilterType.LOW_SHELF
        "HSC", "HS", "HIGHSHELF" -> FilterType.HIGH_SHELF
        "LP", "LPQ", "LOWPASS" -> FilterType.LOW_PASS
        "HP", "HPQ", "HIGHPASS" -> FilterType.HIGH_PASS
        else -> null
    }

    private fun format(value: Float): String {
        val rounded = kotlin.math.round(value * 100f) / 100f
        return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
    }
}
