package com.ljyh.mei.data.repository.artwork

import com.ljyh.mei.utils.image.transparentAppleArtwork
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.text.Normalizer
import java.util.Locale

@Serializable
data class AppleArtistImage(val url: String, val width: Int, val height: Int, val background: String? = null)

@Serializable
data class AppleArtistArtwork(
    val id: String,
    val name: String,
    val portrait: AppleArtistImage,
    val banner: AppleArtistImage? = null,
    val logo: AppleArtistImage? = null,
    val motionPreview: AppleArtistImage? = null,
    val motionUrl: String? = null,
)

internal fun normalizedArtistName(name: String): String = Normalizer.normalize(name, Normalizer.Form.NFKD)
    .replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

/** Reject ambiguous homonyms and partial-name hits rather than decorating the wrong artist. */
internal fun matchAppleArtist(candidates: JsonArray, names: List<String>): String? {
    val expected = names.map(::normalizedArtistName).filter(String::isNotBlank).toSet()
    val matches = candidates.mapNotNull { it as? JsonObject }.filter {
        normalizedArtistName(it.obj("attributes")?.text("name").orEmpty()) in expected
    }.map { it.text("id") }.filter { it.isNotEmpty() && it.all(Char::isDigit) }.distinct()
    return matches.singleOrNull()
}

internal fun parseAppleArtistArtwork(item: JsonObject): AppleArtistArtwork? {
    val attributes = item.obj("attributes") ?: return null
    val portrait = parseArtistImage(attributes.obj("artwork")) ?: return null
    val editorial = attributes.obj("editorialArtwork")
    val logo = parseArtistImage(editorial?.obj("musicContentColorLogoTrimmed"))?.let {
        it.copy(url = it.url.transparentAppleArtwork())
    }
    // A portrait remains the phone hero. Wider editorial images are available to wide layouts.
    val banner = listOf("centeredFullscreenBackground", "subscriptionHero", "storeFlowcase", "originalFlowcaseBrick")
        .firstNotNullOfOrNull { parseArtistImage(editorial?.obj(it)) }
    val id = item.text("id").takeIf { it.isNotBlank() && it.all(Char::isDigit) } ?: return null
    val name = attributes.text("name").takeIf(String::isNotBlank) ?: return null
    val motion = attributes.obj("editorialVideo")?.obj("motionArtistFullscreen16x9")
    val motionPreview = parseArtistImage(motion?.obj("previewFrame"))
    val motionUrl = motion?.text("video")?.toHttpUrlOrNull()?.takeIf { url ->
        url.scheme == "https" && (url.host == "mzstatic.com" || url.host.endsWith(".mzstatic.com") ||
            url.host == "itunes.apple.com" || url.host.endsWith(".itunes.apple.com"))
    }?.toString()
    return AppleArtistArtwork(id, name, portrait, banner, logo, motionPreview, motionUrl)
}

private fun parseArtistImage(image: JsonObject?): AppleArtistImage? {
    image ?: return null
    val raw = image.text("url")
    val url = raw.toHttpUrlOrNull() ?: return null
    if (url.scheme != "https" || !(url.host == "mzstatic.com" || url.host.endsWith(".mzstatic.com"))) return null
    val width = (image["width"] as? JsonPrimitive)?.intOrNull ?: return null
    val height = (image["height"] as? JsonPrimitive)?.intOrNull ?: return null
    if (width !in 1..50000 || height !in 1..50000) return null
    val background = image.text("bgColor").takeIf { it.matches(Regex("[a-fA-F0-9]{6}")) }
    return AppleArtistImage(raw, width, height, background)
}

internal fun JsonObject.obj(key: String) = get(key) as? JsonObject
internal fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
