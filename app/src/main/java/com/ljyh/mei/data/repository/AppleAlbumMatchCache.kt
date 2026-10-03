package com.ljyh.mei.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class AlbumMatchEntry(val id: String?, val validUntil: Long)

private const val MatchTtlMs = 30L * 24 * 60 * 60_000
private const val NoMatchTtlMs = 6L * 60 * 60_000

/** Shares work for one album while allowing different albums to search concurrently. */
internal class AppleAlbumMatchCache(
    private val read: suspend () -> Map<String, AlbumMatchEntry>,
    private val write: suspend (Map<String, AlbumMatchEntry>) -> Unit,
    private val search: suspend (album: String, artist: String, artworkUrl: String) -> String?,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val entries = mutableMapOf<String, AlbumMatchEntry>()
    private val inFlight = mutableMapOf<String, CompletableDeferred<String?>>()
    private var loaded = false

    suspend fun get(album: String, artist: String, artworkUrl: String): String? {
        val albumKey = normalized(album)
        val artistKey = normalized(artist)
        if (albumKey.isBlank() || artistKey.isBlank()) {
            appleMatchLog { "cache skip reason=blank-normalized-key album='$album' artist='$artist'" }
            return null
        }
        // Version the key so old negative results do not suppress the new image fallback.
        val key = "v2\u0000$albumKey\u0000$artistKey"
        var owner = false
        val pending = mutex.withLock {
            if (!loaded) {
                try {
                    entries.putAll(read())
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // A damaged cache must not disable cover lookup.
                }
                loaded = true
            }
            val currentTime = now()
            entries[key]?.takeIf { it.validUntil > currentTime }?.let {
                appleMatchLog { "cache hit album='$album' artist='$artist' id=${it.id ?: "none"} ttlMs=${it.validUntil - currentTime}" }
                return it.id
            }
            inFlight[key] ?: CompletableDeferred<String?>().also {
                inFlight[key] = it
                owner = true
            }
        }
        if (!owner) {
            appleMatchLog { "cache join in-flight album='$album' artist='$artist'" }
            return pending.await()
        }
        appleMatchLog { "cache miss album='$album' artist='$artist'; starting search" }
        try {
            val id = search(album, artist, artworkUrl)
            mutex.withLock {
                val currentTime = now()
                entries[key] = AlbumMatchEntry(
                    id, currentTime + if (id == null) NoMatchTtlMs else MatchTtlMs
                )
                entries.entries.removeAll { it.value.validUntil <= currentTime }
                if (entries.size > 256) {
                    entries.minByOrNull { it.value.validUntil }?.key?.let(entries::remove)
                }
                try {
                    write(entries.toMap())
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // In-memory deduplication still works if persistence temporarily fails.
                }
                inFlight.remove(key)?.complete(id)
            }
            appleMatchLog { "cache stored album='$album' artist='$artist' id=${id ?: "none"}" }
            return id
        } catch (failure: Throwable) {
            appleMatchLog { "cache search failed album='$album' artist='$artist' error=${failure.javaClass.simpleName}" }
            withContext(NonCancellable) {
                mutex.withLock { inFlight.remove(key)?.completeExceptionally(failure) }
            }
            throw failure
        }
    }
}
