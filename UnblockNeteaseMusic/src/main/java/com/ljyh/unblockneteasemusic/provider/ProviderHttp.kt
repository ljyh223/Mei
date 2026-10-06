package com.ljyh.unblockneteasemusic.provider

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

internal fun providerClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = true
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 15_000
        socketTimeoutMillis = 10_000
    }
}

/** A signed URL is useful only if its upstream actually serves audio bytes. */
internal suspend fun HttpClient.canReadAudio(url: String, expectedDurationMs: Long = 0): Boolean {
    if (!url.startsWith("https://") && !url.startsWith("http://")) return false
    return prepareGet(url) {
        header(HttpHeaders.Range, "bytes=0-8191")
        header(HttpHeaders.AcceptEncoding, "identity")
    }.execute { response ->
        val contentType = response.headers[HttpHeaders.ContentType].orEmpty().lowercase()
        val totalBytes = response.headers[HttpHeaders.ContentRange]
            ?.substringAfterLast('/')?.toLongOrNull()
            ?: response.headers[HttpHeaders.ContentLength]
                ?.toLongOrNull()?.takeIf { response.status == HttpStatusCode.OK }
        // Kuwo can serve a short spoken copyright notice as a valid MP3. Its URL and
        // first bytes pass the normal probe, so reject files far too short for the song.
        val plausibleLength = expectedDurationMs <= 0 || totalBytes == null ||
            totalBytes >= expectedDurationMs * 4 // 32 kbit/s, below normal MP3 music quality.
        response.status.isSuccess() &&
            !contentType.startsWith("text/html") &&
            !contentType.startsWith("application/json") &&
            plausibleLength &&
            response.bodyAsChannel().awaitContent()
    }
}
