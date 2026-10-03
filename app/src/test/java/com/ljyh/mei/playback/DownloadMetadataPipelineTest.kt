package com.ljyh.mei.playback

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadMetadataPipelineTest {
    @Test
    fun startsCoverAndLyricsBeforeAudioFinishes() = runBlocking {
        val audioGate = CompletableDeferred<Unit>()
        val lyricStarted = CompletableDeferred<Unit>()
        val coverStarted = CompletableDeferred<Unit>()
        val result = async {
            downloadWithMetadata(
                downloadAudio = { audioGate.await(); true },
                fetchLyric = { lyricStarted.complete(Unit); "[00:01.00]line" },
                fetchCover = { coverStarted.complete(Unit); byteArrayOf(1, 2, 3) },
            )
        }

        lyricStarted.await()
        coverStarted.await()
        assertTrue("Audio must still be downloading", !result.isCompleted)
        audioGate.complete(Unit)
        assertEquals("[00:01.00]line", result.await()?.lyric)
        assertArrayEquals(byteArrayOf(1, 2, 3), result.await()?.coverBytes)
    }

    @Test
    fun failedAudioCancelsUnfinishedMetadataRequests() = runBlocking {
        val lyricCancelled = CompletableDeferred<Unit>()
        val coverCancelled = CompletableDeferred<Unit>()
        val result = downloadWithMetadata(
            downloadAudio = { false },
            fetchLyric = { try { awaitCancellation() } finally { lyricCancelled.complete(Unit) } },
            fetchCover = { try { awaitCancellation() } finally { coverCancelled.complete(Unit) } },
        )
        assertNull(result)
        lyricCancelled.await()
        coverCancelled.await()
    }

    @Test
    fun optionalMetadataFailureDoesNotDiscardDownloadedAudio() = runBlocking {
        val result = downloadWithMetadata(
            downloadAudio = { true },
            fetchLyric = { error("Lyric server unavailable") },
            fetchCover = { error("Cover server unavailable") },
        )
        assertNull(result?.lyric)
        assertNull(result?.coverBytes)
    }

    @Test
    fun missingLyricIsNotFetchedTwiceForSidecar() = runBlocking {
        var fetches = 0
        val fetch: suspend () -> String? = { fetches++; "[00:01.00]line" }
        assertNull(lyricForSidecar(true, null, fetch))
        assertEquals(0, fetches)
        assertEquals("[00:01.00]line", lyricForSidecar(false, null, fetch))
        assertEquals(1, fetches)
    }

    @Test
    fun slowOptionalMetadataStopsDelayingCompletedAudio() = runBlocking {
        val result = downloadWithMetadata(
            downloadAudio = { true },
            fetchLyric = { awaitCancellation() },
            fetchCover = { awaitCancellation() },
            metadataTimeoutMs = 25L,
        )
        assertNull(result?.lyric)
        assertNull(result?.coverBytes)
    }
}
