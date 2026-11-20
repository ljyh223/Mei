package com.ljyh.mei.ui.screen.artist

import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.repository.artwork.AppleArtistArtwork
import com.ljyh.mei.data.repository.artwork.AppleArtistImage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class ArtistArtworkControllerTest {
    private val artist = ArtistDetail.Data.Artist(name = "歌手")
    private fun artwork(id: String = "10") = AppleArtistArtwork(
        id = id, name = "歌手", portrait = AppleArtistImage("https://example.invalid/$id.jpg", 1000, 1000),
    )

    @Test fun defaultOffNeverResolvesEvenWhenArtworkWouldAlreadyBeCached() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val enabled = MutableStateFlow(false)
        var calls = 0
        try {
            val controller = ArtistArtworkController(scope, enabled) { _, _ -> calls++; artwork() }
            controller.bind("1", artist)
            controller.bind("2", artist)
            assertEquals(0, calls)
            assertFalse(controller.state.value.enabled)
            assertNull(controller.state.value.artwork)
        } finally { scope.cancel() }
    }

    @Test fun waitingForThePreferenceDoesNotBrieflyOptIn() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val enabled = MutableSharedFlow<Boolean>(replay = 1)
        var calls = 0
        try {
            val controller = ArtistArtworkController(scope, enabled) { _, _ -> calls++; artwork() }
            controller.bind("1", artist)
            assertEquals(0, calls)
            assertFalse(controller.state.value.enabled)
            enabled.emit(true)
            assertEquals(1, calls)
            assertEquals(artwork(), controller.state.value.artwork)
            enabled.emit(false)
            assertFalse(controller.state.value.enabled)
            assertNull(controller.state.value.artwork)
        } finally { scope.cancel() }
    }

    @Test fun enablingLoadsTheCurrentArtistAndReturningDoesNotReloadIt() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val enabled = MutableStateFlow(false)
        val ids = mutableListOf<String>()
        try {
            val controller = ArtistArtworkController(scope, enabled) { id, _ -> ids += id; artwork(id) }
            controller.bind("1", artist)
            enabled.value = true
            assertEquals(artwork("1"), controller.state.value.artwork)
            controller.bind("1", artist)
            assertEquals(listOf("1"), ids)
            enabled.value = false
            assertNull(controller.state.value.artwork)
            controller.bind("2", artist)
            assertEquals(listOf("1"), ids)
            enabled.value = true
            assertEquals(listOf("1", "2"), ids)
            assertEquals(artwork("2"), controller.state.value.artwork)
        } finally { scope.cancel() }
    }

    @Test fun enableBeforeDetailsWaitsAndDisableBeforeDetailsPreventsResolution() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val enabled = MutableStateFlow(true)
        var calls = 0
        try {
            val controller = ArtistArtworkController(scope, enabled) { _, _ -> calls++; artwork() }
            controller.bind("1", null)
            assertEquals(0, calls)
            enabled.value = false
            controller.bind("1", artist)
            assertEquals(0, calls)
            enabled.value = true
            assertEquals(1, calls)
        } finally { scope.cancel() }
    }

    @Test fun disablingCancelsTheRequestAndRejectsItsLateArtwork() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val enabled = MutableStateFlow(true)
        val delayed = CompletableDeferred<AppleArtistArtwork?>()
        var request: Job? = null
        try {
            val controller = ArtistArtworkController(scope, enabled) { _, _ ->
                request = currentCoroutineContext()[Job]
                withContext(NonCancellable) { delayed.await() }
            }
            controller.bind("1", artist)
            enabled.value = false
            assertTrue(requireNotNull(request).isCancelled)
            delayed.complete(artwork())
            assertFalse(controller.state.value.enabled)
            assertNull(controller.state.value.artwork)
        } finally { delayed.complete(null); scope.cancel() }
    }

    @Test fun oldRequestCannotReplaceArtworkAfterTurningOffAndOn() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val enabled = MutableStateFlow(true)
        val old = CompletableDeferred<AppleArtistArtwork?>()
        var calls = 0
        try {
            val controller = ArtistArtworkController(scope, enabled) { _, _ ->
                calls++
                if (calls == 1) withContext(NonCancellable) { old.await() } else artwork("new")
            }
            controller.bind("1", artist)
            enabled.value = false
            enabled.value = true
            assertEquals(artwork("new"), controller.state.value.artwork)
            old.complete(artwork("old"))
            assertEquals(artwork("new"), controller.state.value.artwork)
            assertEquals(2, calls)
        } finally { old.complete(null); scope.cancel() }
    }

    @Test fun switchingArtistKeepsOnlyTheLatestArtwork() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val enabled = MutableStateFlow(true)
        val old = CompletableDeferred<AppleArtistArtwork?>()
        try {
            val controller = ArtistArtworkController(scope, enabled) { id, _ ->
                if (id == "1") withContext(NonCancellable) { old.await() } else artwork(id)
            }
            controller.bind("1", artist)
            controller.bind("2", artist)
            old.complete(artwork("1"))
            assertEquals(artwork("2"), controller.state.value.artwork)
        } finally { old.complete(null); scope.cancel() }
    }

    @Test fun failedResolutionFallsBackAndCanBeRetriedByReenabling() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val enabled = MutableStateFlow(true)
        var calls = 0
        try {
            val controller = ArtistArtworkController(scope, enabled) { _, _ ->
                calls++
                if (calls == 1) error("offline") else artwork()
            }
            controller.bind("1", artist)
            assertTrue(controller.state.value.enabled)
            assertNull(controller.state.value.artwork)
            controller.bind("1", artist)
            assertEquals(1, calls)
            enabled.value = false
            enabled.value = true
            assertEquals(artwork(), controller.state.value.artwork)
        } finally { scope.cancel() }
    }
}
