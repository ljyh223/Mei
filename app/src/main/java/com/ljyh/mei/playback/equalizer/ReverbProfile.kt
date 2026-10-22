package com.ljyh.mei.playback.equalizer

/**
 * Freeverb-style room reverb. Parameters mirror the BASS_FX/BASS "V3 reverb" vocabulary that
 * Salt Player exposes, but the DSP is implemented here so it can run inside the ExoPlayer chain.
 */
data class ReverbProfile(
    val enabled: Boolean = false,
    val presetId: String = "off",
    /** 0..1 — comb feedback, larger means longer tail. */
    val roomSize: Float = 0.85f,
    /** 0..1 — high-frequency damping inside the tail. */
    val damping: Float = 0.4f,
    /** 0..1 — stereo spread of the wet signal. */
    val width: Float = 0.75f,
    /** 0..1 — wet gain. BASS_FX freeverb uses 0..1 mixes too; Salt's V3 sits at 0.75. */
    val wet: Float = 0.65f,
    /** 0..1 — dry gain. */
    val dry: Float = 0.85f,
    /** 0..100 ms — gap before the tail starts. */
    val preDelayMs: Float = 18f,
) {
    fun normalized(): ReverbProfile = copy(
        roomSize = roomSize.finiteOr(0.85f).coerceIn(0f, 1f),
        damping = damping.finiteOr(0.4f).coerceIn(0f, 1f),
        width = width.finiteOr(0.75f).coerceIn(0f, 1f),
        wet = wet.finiteOr(0.65f).coerceIn(0f, 1f),
        dry = dry.finiteOr(0.85f).coerceIn(0f, 1f),
        preDelayMs = preDelayMs.finiteOr(18f).coerceIn(0f, 100f),
    )

    companion object {
        val Off = ReverbProfile(enabled = false, presetId = "off", wet = 0f, dry = 1f)
        // Salt Player's "V3 混响" applies BASS_FX freeverb with dry 0.85 / wet 0.75 /
        // room 0.9 / damping 0.4 / width 0.75; these presets keep that level.
        val Room = ReverbProfile(enabled = true, presetId = "room", roomSize = 0.75f, damping = 0.55f,
            width = 0.8f, wet = 0.5f, dry = 0.9f, preDelayMs = 12f)
        val Hall = ReverbProfile(enabled = true, presetId = "hall", roomSize = 0.9f, damping = 0.4f,
            width = 0.75f, wet = 0.7f, dry = 0.85f, preDelayMs = 24f)
        val Plate = ReverbProfile(enabled = true, presetId = "plate", roomSize = 0.82f, damping = 0.3f,
            width = 0.9f, wet = 0.75f, dry = 0.85f, preDelayMs = 8f)
        val Cathedral = ReverbProfile(enabled = true, presetId = "cathedral", roomSize = 0.95f,
            damping = 0.5f, width = 1f, wet = 0.8f, dry = 0.8f, preDelayMs = 36f)
    }
}

private fun Float.finiteOr(fallback: Float) = if (isFinite()) this else fallback

data class ReverbPreset(val id: String, val label: String, val profile: ReverbProfile)

object ReverbPresets {
    val all: List<ReverbPreset> = listOf(
        ReverbPreset("off", "关闭", ReverbProfile.Off),
        ReverbPreset("room", "房间", ReverbProfile.Room),
        ReverbPreset("hall", "大厅", ReverbProfile.Hall),
        ReverbPreset("plate", "板式", ReverbProfile.Plate),
        ReverbPreset("cathedral", "教堂", ReverbProfile.Cathedral),
    )

    fun byId(id: String): ReverbProfile = all.firstOrNull { it.id == id }?.profile ?: ReverbProfile.Off
}

/** Compact DataStore representation; malformed entries fall back to the defaults. */
object ReverbProfileCodec {
    fun encode(profile: ReverbProfile): String {
        val safe = profile.normalized()
        return buildString {
            append("v1;")
            append(if (safe.enabled) "1" else "0")
            append(';').append(safe.presetId.replace(";", ""))
            append(';').append(safe.roomSize)
            append(';').append(safe.damping)
            append(';').append(safe.width)
            append(';').append(safe.wet)
            append(';').append(safe.dry)
            append(';').append(safe.preDelayMs)
        }
    }

    fun decode(raw: String?): ReverbProfile? {
        if (raw == null) return null
        val parts = raw.split(';')
        if (parts.size != 9 || parts[0] != "v1") return null
        fun field(index: Int, fallback: Float): Float =
            parts[index].toFloatOrNull()?.takeIf(Float::isFinite) ?: fallback
        return ReverbProfile(
            enabled = parts[1] == "1",
            presetId = parts[2],
            roomSize = field(3, 0.85f),
            damping = field(4, 0.4f),
            width = field(5, 0.75f),
            wet = field(6, 0.65f),
            dry = field(7, 0.85f),
            preDelayMs = field(8, 18f),
        ).normalized()
    }
}
