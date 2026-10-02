package com.ljyh.mei.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class AlbumMatchEntry(val id: String?, val validUntil: Long)

private const val MatchTtlMs = 30L * 24 * 60 * 60_000
private const val NoMatchTtlMs = 6L * 60 * 60_000

/** Serializes lookups, so songs from the same album share one search even when requested together. */
internal class AppleAlbumMatchCache(
    private val read: suspend () -> Map<String, AlbumMatchEntry>,
    private val write: suspend (Map<String, AlbumMatchEntry>) -> Unit,
    private val search: suspend (album: String, artist: String) -> String?,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val entries = mutableMapOf<String, AlbumMatchEntry>()
    private var loaded = false

    suspend fun get(album: String, artist: String): String? {
        val albumKey = normalized(album)
        val artistKey = normalized(artist)
        if (albumKey.isBlank() || artistKey.isBlank()) return null
        val key = "$albumKey\u0000$artistKey"
        return mutex.withLock {
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
            entries[key]?.takeIf { it.validUntil > currentTime }?.let { return@withLock it.id }
            val id = search(album, artist)
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
            id
        }
    }
}
