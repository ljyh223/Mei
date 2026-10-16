package com.ljyh.mei.utils.image

import android.net.Uri
import java.util.Locale

fun String.smallImage(): String = if (startsWith("/")) this else "$this?param=100y100"

fun String.middleImage(): String = if (startsWith("/")) this else "$this?param=300y300"

fun String.largeImage(): String = if (startsWith("/")) this else "$this?param=500y500"

fun String.size1600(): String = "$this?param=1600y1600"

/** Request a square thumbnail from NetEase's image CDN, preserving other query parameters. */
fun String.sizedNeteaseImage(side: Int): String {
    if (isBlank()) return this
    val uri = Uri.parse(this)
    val host = uri.host?.lowercase(Locale.ROOT) ?: return this
    if (host != "music.126.net" && !host.endsWith(".music.126.net")) return this

    val builder = uri.buildUpon().clearQuery()
    uri.queryParameterNames.forEach { name ->
        if (name != "param") {
            uri.getQueryParameters(name).forEach { value -> builder.appendQueryParameter(name, value) }
        }
    }
    val dimension = side.coerceAtLeast(1)
    return builder.appendQueryParameter("param", "${dimension}y${dimension}").build().toString()
}
