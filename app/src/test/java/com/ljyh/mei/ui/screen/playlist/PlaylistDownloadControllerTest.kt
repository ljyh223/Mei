package com.ljyh.mei.ui.screen.playlist

import androidx.lifecycle.SavedStateHandle
import com.ljyh.mei.constants.DownloadQuality
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.network.Resource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class PlaylistDownloadControllerTest {
    private fun track(id: Long) = MediaMetadata(id, "Song $id", "", emptyList(), 1000, MediaMetadata.Album(0, ""))
    private val selection = PlaylistDownloadSelection(42, "歌单", listOf(1, 2))
    private val options = PlaylistDownloadOptions(DownloadQuality.EXHIGH, "Music/Mei", true)
    private fun snapshot(handle: SavedStateHandle): SavedStateHandle = SavedStateHandle(handle.keys().associateWith { key ->
        val value = handle.get<Any>(key)
        if (value is LongArray) value.clone() else value
    })

    @Test fun confirmationRestoresFromIdsAndRefetchesMetadataWithoutEnqueueing() = runBlocking {
        val handle = SavedStateHandle()
        var writes = 0
        val first = PlaylistDownloadController(handle, this, { Resource.Success(it.ids.map(::track)) }, { _, _, _ -> writes++; Resource.Success(2) })
        first.begin(selection, selection.ids.map(::track))
        assertEquals(PlaylistDownloadPhase.Confirming, first.state.value.phase)
        var loads = 0
        val restored = PlaylistDownloadController(snapshot(handle), this, { loads++; Resource.Success(it.ids.map(::track)) }, { _, _, _ -> writes++; Resource.Success(2) })
        yield()
        assertEquals(1, loads)
        assertEquals(selection, restored.state.value.selection)
        assertEquals(PlaylistDownloadPhase.Confirming, restored.state.value.phase)
        assertEquals(0, writes)
    }
    @Test fun preparationSurvivesARecreatedOwnerWithoutRestoringAPermanentBusyFlag() = runBlocking {
        val handle = SavedStateHandle()
        val oldScope = CoroutineScope(coroutineContext + SupervisorJob())
        val never = CompletableDeferred<Resource<List<MediaMetadata>>>()
        val first = PlaylistDownloadController(handle, oldScope, { never.await() }, { _, _, _ -> Resource.Success(2) })
        first.begin(selection); yield()
        val saved = snapshot(handle)
        oldScope.cancel()
        val restored = PlaylistDownloadController(saved, this, { Resource.Success(it.ids.map(::track)) }, { _, _, _ -> Resource.Success(2) })
        yield()
        assertEquals(PlaylistDownloadPhase.Confirming, restored.state.value.phase)
    }
    @Test fun duplicatePreparationAndConfirmationDoNotStartDuplicateJobs() = runBlocking {
        var loads = 0; var writes = 0
        val metadata = CompletableDeferred<Resource<List<MediaMetadata>>>()
        val queued = CompletableDeferred<Resource<Int>>()
        val controller = PlaylistDownloadController(SavedStateHandle(), this,
            { loads++; metadata.await() }, { _, _, _ -> writes++; queued.await() })
        controller.begin(selection); controller.begin(selection); yield()
        assertEquals(1, loads)
        metadata.complete(Resource.Success(selection.ids.map(::track))); yield()
        controller.confirm(options); controller.confirm(options); yield()
        controller.begin(selection.copy(ids = listOf(3)))
        assertEquals(1, writes)
        assertEquals(selection, controller.state.value.selection)
        queued.complete(Resource.Success(2)); yield()
        assertEquals(PlaylistDownloadPhase.Idle, controller.state.value.phase)
    }
    @Test fun uncertainSubmissionIsNeverAutomaticallyReplayedAfterRecreation() = runBlocking {
        val handle = SavedStateHandle()
        val oldScope = CoroutineScope(coroutineContext + SupervisorJob())
        val never = CompletableDeferred<Resource<Int>>()
        val first = PlaylistDownloadController(handle, oldScope, { Resource.Success(it.ids.map(::track)) }, { _, _, _ -> never.await() })
        first.begin(selection, selection.ids.map(::track)); first.confirm(options); yield()
        val saved = snapshot(handle); oldScope.cancel()
        var writes = 0
        val restored = PlaylistDownloadController(saved, this, { Resource.Success(it.ids.map(::track)) }, { _, _, _ -> writes++; Resource.Success(2) })
        yield()
        assertEquals(PlaylistDownloadPhase.Confirming, restored.state.value.phase)
        assertTrue(restored.state.value.uncertainSubmission)
        assertEquals(0, writes)
        restored.confirm(options); yield()
        assertEquals(1, writes)
    }
    @Test fun failedPreparationClearsBusyStateAndAllowsRetry() = runBlocking {
        var fail = true
        val controller = PlaylistDownloadController(SavedStateHandle(), this, {
            if (fail) Resource.Error("offline") else Resource.Success(it.ids.map(::track))
        }, { _, _, _ -> Resource.Success(2) })
        controller.begin(selection); yield()
        assertEquals(PlaylistDownloadPhase.Idle, controller.state.value.phase)
        fail = false; controller.begin(selection); yield()
        assertEquals(PlaylistDownloadPhase.Confirming, controller.state.value.phase)
    }
    @Test fun failedSubmissionRetainsSelectionForExplicitRetryAndSuccessfulSubmissionClearsIt() = runBlocking {
        val handle = SavedStateHandle()
        var fail = true
        val controller = PlaylistDownloadController(handle, this, { Resource.Success(it.ids.map(::track)) }, { _, _, _ ->
            if (fail) Resource.Error("offline") else Resource.Success(2)
        })
        controller.begin(selection, selection.ids.map(::track)); controller.confirm(options); yield()
        assertEquals(PlaylistDownloadPhase.Confirming, controller.state.value.phase)
        assertNotNull(handle.get<LongArray>(PlaylistDownloadController.IdsKey))
        fail = false; controller.confirm(options); yield()
        assertEquals(PlaylistDownloadPhase.Idle, controller.state.value.phase)
        assertNull(handle.get<LongArray>(PlaylistDownloadController.IdsKey))
    }
    @Test fun lateCancelledPreparationDoesNotReopenADismissedDialog() = runBlocking {
        val late = CompletableDeferred<Resource<List<MediaMetadata>>>()
        val controller = PlaylistDownloadController(SavedStateHandle(), this,
            { withContext(NonCancellable) { late.await() } }, { _, _, _ -> Resource.Success(2) })
        controller.begin(selection); yield(); controller.dismiss()
        late.complete(Resource.Success(selection.ids.map(::track))); yield(); yield()
        assertEquals(PlaylistDownloadPhase.Idle, controller.state.value.phase)
    }
    @Test fun incompleteMetadataCannotSilentlyConfirmAPartialSelection() = runBlocking {
        val controller = PlaylistDownloadController(SavedStateHandle(), this,
            { Resource.Success(listOf(track(1))) }, { _, _, _ -> error("Should not enqueue") })
        controller.begin(selection); yield()
        assertEquals(PlaylistDownloadPhase.Idle, controller.state.value.phase)
    }
    @Test fun exceptionsAfterSubmissionMarkTheSelectionAsUncertain() = runBlocking {
        val controller = PlaylistDownloadController(SavedStateHandle(), this,
            { Resource.Success(it.ids.map(::track)) }, { _, _, _ -> throw IllegalStateException("commit interrupted") })
        controller.begin(selection, selection.ids.map(::track)); controller.confirm(options); yield()
        assertEquals(PlaylistDownloadPhase.Confirming, controller.state.value.phase)
        assertTrue(controller.state.value.uncertainSubmission)
    }
}
