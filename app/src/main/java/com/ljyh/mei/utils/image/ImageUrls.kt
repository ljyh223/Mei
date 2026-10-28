package com.ljyh.mei.utils.image

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Shared CDN pixel sizes. UI dimensions in dp must not create new network variants. */
enum class ArtworkSize(val pixels: Int) {
    Small(192), Card(512), Detail(1024);

    companion object {
        fun forPixels(pixels: Int): ArtworkSize = entries.firstOrNull { pixels <= it.pixels } ?: Detail

        fun forBounds(width: Int?, height: Int?): ArtworkSize = forPixels(
            listOfNotNull(width, height).filter { it > 0 }.maxOrNull() ?: Card.pixels,
        )
    }
}

private val processingParameters = setOf("param", "imageView", "thumbnail", "type", "quality", "enlarge")
private val processingChainSeparator = Regex("\\||%7c", RegexOption.IGNORE_CASE)

/** In NOS pipelines, query names such as `type` belong to individual operations (e.g. watermark). */
private fun HttpUrl.hasCompositeProcessing(): Boolean =
    processingChainSeparator.containsMatchIn(encodedQuery.orEmpty()) ||
        "watermark" in queryParameterNames

internal fun String.neteaseImageUrl(): HttpUrl? = toHttpUrlOrNull()?.takeIf {
    it.host == "music.126.net" || it.host.endsWith(".music.126.net")
}

/** Preserve the complete source image; cropping belongs to the displaying component. */
fun String.artworkImage(size: ArtworkSize): String = resizedArtwork(size.pixels)

/** The full player keeps its own quality setting; ordinary automatic tiers stop at 1024px. */
fun String.playerArtworkUrl(original: Boolean): String =
    if (original) originalArtworkUrl() else resizedArtwork(1600)

private fun String.resizedArtwork(pixels: Int): String {
    val url = neteaseImageUrl() ?: return this
    // Keep composed artwork intact. Flattening the query can remove operation boundaries and
    // replace watermark's type=1 with an image output format. Do not append a second resize.
    if (url.hasCompositeProcessing()) return this
    // Preserve animation when the source is an animated image. NOS defaults to a static frame.
    val builder = url.newBuilder()
    processingParameters.forEach(builder::removeAllQueryParameters)
    return builder
        .addQueryParameter("imageView", "1")
        .addQueryParameter("quality", "80")
        .addQueryParameter("thumbnail", "${pixels}z${pixels}")
        .addQueryParameter("type", "webp")
        .setQueryParameter("tostatic", "0")
        .build().toString()
}

/** Remove our resize/encoding parameters only. Other transforms and query values are retained. */
fun String.originalArtworkUrl(): String {
    val url = neteaseImageUrl() ?: return this
    // The composed cover is the resource supplied by the API; do not strip its overlays.
    if (url.hasCompositeProcessing()) return this
    val builder = url.newBuilder()
    (processingParameters + "tostatic").forEach(builder::removeAllQueryParameters)
    return builder.build().toString()
}
