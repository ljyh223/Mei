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

internal fun String.neteaseImageUrl(): HttpUrl? = toHttpUrlOrNull()?.takeIf {
    it.host == "music.126.net" || it.host.endsWith(".music.126.net")
}

/** Preserve the complete source image; cropping belongs to the displaying component. */
fun String.artworkImage(size: ArtworkSize): String {
    val url = neteaseImageUrl() ?: return this
    // Preserve animation when the source is an animated image. NOS defaults to a static frame.
    val builder = url.newBuilder()
    processingParameters.forEach(builder::removeAllQueryParameters)
    return builder
        .addQueryParameter("imageView", "1")
        .addQueryParameter("quality", "80")
        .addQueryParameter("thumbnail", "${size.pixels}z${size.pixels}")
        .addQueryParameter("type", "webp")
        .setQueryParameter("tostatic", "0")
        .build().toString()
}

/** Remove our resize/encoding parameters only. Other transforms and query values are retained. */
fun String.originalArtworkUrl(): String {
    val url = neteaseImageUrl() ?: return this
    val builder = url.newBuilder()
    (processingParameters + "tostatic").forEach(builder::removeAllQueryParameters)
    return builder.build().toString()
}
