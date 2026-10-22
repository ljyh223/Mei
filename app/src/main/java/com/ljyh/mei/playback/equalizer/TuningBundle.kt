package com.ljyh.mei.playback.equalizer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * One shareable tuning: the parametric EQ and the reverb used together.
 *
 * The payload embeds the existing versioned string codecs instead of duplicating every field, so
 * a bundle stays readable and old files keep working when fields are added.
 */
data class TuningBundle(
    val name: String,
    val eq: EqualizerProfile,
    val reverb: ReverbProfile,
) {
    companion object {
        const val FORMAT = "mei-tuning"
        const val VERSION = 1
    }
}

object TuningBundleCodec {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun encode(bundle: TuningBundle): String = buildJsonObject {
        put("format", TuningBundle.FORMAT)
        put("version", TuningBundle.VERSION)
        put("name", bundle.name.take(80))
        putJsonObject("eq") {
            put("codec", EqualizerProfileCodec.encode(bundle.eq))
        }
        putJsonObject("reverb") {
            put("codec", ReverbProfileCodec.encode(bundle.reverb))
        }
    }.toString()

    fun decode(raw: String): TuningBundle? = runCatching {
        val root = json.parseToJsonElement(raw).jsonObject
        if (root["format"]?.jsonPrimitive?.contentOrNull != TuningBundle.FORMAT) return null
        val version = root["version"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return null
        if (version > TuningBundle.VERSION) return null
        val name = root["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: "调音"
        val eq = root["eq"]?.jsonObject?.get("codec")?.jsonPrimitive?.contentOrNull
            ?.let(EqualizerProfileCodec::decode) ?: return null
        val reverb = root["reverb"]?.jsonObject?.get("codec")?.jsonPrimitive?.contentOrNull
            ?.let(ReverbProfileCodec::decode) ?: return null
        TuningBundle(name, eq, reverb)
    }.getOrNull()

    fun looksLikeBundle(raw: String): Boolean =
        raw.contains("\"${TuningBundle.FORMAT}\"") || raw.trimStart().startsWith("{")
}
