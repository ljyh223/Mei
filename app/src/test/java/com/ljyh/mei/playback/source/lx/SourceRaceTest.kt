package com.ljyh.mei.playback.source.lx

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SourceRaceTest {
    @Test fun aFastPreviewDoesNotBeatFullAudio() = runBlocking {
        val result = firstUsable(listOf(30_000L, 180_000L), staggerMs = 0) { duration ->
            if (duration > 30_000) delay(50)
            duration.takeUnless { PreviewPolicy.isPreview(it) }
        }
        assertEquals(180_000L, result)
    }
    @Test fun fasterLowPriorityWinsAndCancelsLoser() = runBlocking {
        var cancelled = false
        val result = firstUsable(listOf("slow", "fast"), staggerMs = 10) { source ->
            if (source == "slow") try { delay(10_000); source } finally { cancelled = true }
            else { delay(10); source }
        }
        assertEquals("fast", result)
        assertTrue(cancelled)
    }
    @Test fun failureDoesNotCancelOtherSources() = runBlocking {
        assertEquals("good", firstUsable(listOf("bad", "good"), staggerMs = 0) {
            if (it == "bad") error("bad source") else { delay(20); it }
        })
    }
    @Test fun exhaustionAndTimeoutReturnNoSource() = runBlocking {
        assertNull(firstUsable(listOf(1, 2), staggerMs = 0) { null as String? })
        assertNull(firstUsable(listOf(1), timeoutMs = 20) { delay(1000); "late" })
    }
    @Test fun parentCancellationPropagates() = runBlocking {
        val result = runCatching { withTimeout(20) { firstUsable(listOf(1)) { delay(1000); "late" } } }
        assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
    }
    @Test fun previewBoundaryAndQualityDowngrade() {
        assertTrue(PreviewPolicy.isPreview(0))
        assertTrue(PreviewPolicy.isPreview(29_999))
        assertTrue(PreviewPolicy.isPreview(30_000))
        assertFalse(PreviewPolicy.isPreview(30_001))
        assertFalse(PreviewPolicy.isPreview(null))
        assertEquals(listOf("flac", "320k", "128k"), PreviewPolicy.qualities("lossless", listOf("128k", "320k", "flac")))
        assertEquals(listOf("128k"), PreviewPolicy.qualities("standard", listOf("128k", "320k")))
    }
}
