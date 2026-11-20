package com.ljyh.mei.data.repository

import com.ljyh.mei.data.repository.artwork.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AppleArtistArtworkCacheTest {
    private val art = AppleArtistArtwork("1", "Artist", AppleArtistImage("https://example.com/portrait", 100, 100))

    @Test fun successfulArtworkPersistsAcrossCacheInstancesAndExpiresAfterSevenDays() = runBlocking {
        var disk = emptyMap<String, ArtistArtworkEntry>()
        var time = 0L
        var requests = 0
        fun cache() = AppleArtistArtworkCache({ disk }, { disk = it }, { time })
        val first = cache()
        assertEquals(art, first.get("one") { requests++; art })
        assertEquals(art, first.get("one") { error("Unexpected fetch") })
        assertEquals(art, cache().get("one") { error("Unexpected fetch") })
        time = 7 * 24 * 60 * 60_000L + 1
        cache().get("one") { requests++; art }
        assertEquals(2, requests)
    }

    @Test fun missingAssetsAreCachedForSixHoursButFailuresAreNotCached() = runBlocking {
        var time = 0L
        var requests = 0
        val cache = AppleArtistArtworkCache({ emptyMap() }, {}, { time })
        assertNull(cache.get("missing") { requests++; null })
        assertNull(cache.get("missing") { error("Unexpected fetch") })
        time = 6 * 60 * 60_000L + 1
        assertEquals(art, cache.get("missing") { requests++; art })
        assertEquals(2, requests)
        try { cache.get("offline") { throw IllegalStateException("offline") }; fail() }
        catch (_: IllegalStateException) { }
        assertEquals(art, cache.get("offline") { art })
    }

    @Test fun concurrentRequestsForTheSameArtistShareOneFetch() = runBlocking {
        val cache = AppleArtistArtworkCache({ emptyMap() }, {})
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var requests = 0
        val first = async { cache.get("one") { requests++; started.complete(Unit); finish.await(); art } }
        started.await()
        val second = async { cache.get("one") { requests++; art } }
        yield(); finish.complete(Unit)
        assertEquals(art, first.await())
        assertEquals(art, second.await())
        assertEquals(1, requests)
    }

    @Test fun cancelledOwnerReleasesItsGateAndDoesNotPersistAMiss() = runBlocking {
        var writes = 0
        val cache = AppleArtistArtworkCache({ emptyMap() }, { writes++ })
        val started = CompletableDeferred<Unit>()
        val request = async { cache.get("one") { started.complete(Unit); awaitCancellation() } }
        started.await(); request.cancelAndJoin()
        assertEquals(0, writes)
        assertEquals(art, cache.get("one") { art })
        assertEquals(1, writes)
    }

    @Test fun damagedOrUnwritablePersistenceDoesNotDisableTheInMemoryResult() = runBlocking {
        val cache = AppleArtistArtworkCache({ error("corrupt cache") }, { error("unwritable") })
        assertEquals(art, cache.get("one") { art })
        assertEquals(art, cache.get("one") { error("Unexpected fetch") })
    }
}
