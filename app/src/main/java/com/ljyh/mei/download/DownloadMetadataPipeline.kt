package com.ljyh.mei.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

internal data class DownloadMetadata(
    val lyric: String?,
    val coverBytes: ByteArray?,
)

/** Fetch optional metadata while audio downloads, cancelling it if audio fails. */
internal suspend fun downloadWithMetadata(
    downloadAudio: suspend () -> Boolean,
    fetchLyric: suspend () -> String?,
    fetchCover: suspend () -> ByteArray?,
    metadataTimeoutMs: Long = 20_000L,
): DownloadMetadata? = coroutineScope {
    val lyric = async(start = CoroutineStart.UNDISPATCHED) {
        try {
            withTimeoutOrNull(metadataTimeoutMs) { fetchLyric() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Download lyric lookup failed")
            null
        }
    }
    val cover = async(start = CoroutineStart.UNDISPATCHED) {
        try {
            withTimeoutOrNull(metadataTimeoutMs) { fetchCover() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Download cover lookup failed")
            null
        }
    }

    try {
        if (!downloadAudio()) {
            lyric.cancel()
            cover.cancel()
            return@coroutineScope null
        }
        DownloadMetadata(lyric.await(), cover.await())
    } catch (e: Throwable) {
        lyric.cancel()
        cover.cancel()
        throw e
    }
}

/** A resolved null means the song has no usable lyric; it must not trigger another lookup. */
internal suspend fun lyricForSidecar(
    alreadyResolved: Boolean,
    resolvedLyric: String?,
    fetchLyric: suspend () -> String?,
): String? = if (alreadyResolved) resolvedLyric else fetchLyric()
