package com.ljyh.mei.data.repository.artwork

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

@Serializable
internal data class ArtistArtworkEntry(val artwork: AppleArtistArtwork?, val until: Long)

/** Per-artist request sharing, bounded persistence and short negative caching for missing artwork. */
internal class AppleArtistArtworkCache(
    private val read: suspend () -> Map<String, ArtistArtworkEntry>,
    private val write: suspend (Map<String, ArtistArtworkEntry>) -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val gates = Array(16) { Mutex() }
    private var loaded = false
    private val entries = mutableMapOf<String, ArtistArtworkEntry>()

    suspend fun get(key: String, fetch: suspend () -> AppleArtistArtwork?): AppleArtistArtwork? =
        gates[Math.floorMod(key.hashCode(), gates.size)].withLock gate@{
            val cached = mutex.withLock {
                if (!loaded) {
                    try { entries.putAll(read()) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { /* A damaged optional cache must not affect the artist page. */ }
                    loaded = true
                }
                entries[key]?.takeIf { it.until > now() }
            }
            if (cached != null) return@gate cached.artwork
            val artwork = fetch() // Errors and cancellations are not persisted as a missing match.
            mutex.withLock {
                val time = now()
                entries[key] = ArtistArtworkEntry(artwork, time + if (artwork == null) 6 * 60 * 60_000L else 7 * 24 * 60 * 60_000L)
                entries.entries.removeAll { it.value.until <= time }
                while (entries.size > 128) entries.remove(entries.minBy { it.value.until }.key)
                try { write(entries.toMap()) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Keep the in-memory result if local persistence fails. */ }
            }
            artwork
        }
}
