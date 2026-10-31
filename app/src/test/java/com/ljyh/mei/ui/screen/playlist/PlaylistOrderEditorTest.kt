package com.ljyh.mei.ui.screen.playlist

import androidx.lifecycle.SavedStateHandle
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.network.Resource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class PlaylistOrderEditorTest {
    private fun items(vararg ids: Long) = ids.map { PlaylistOrderItem(it, null) }
    private fun PlaylistOrderEditor.ids() = state.value.items.orEmpty().map(PlaylistOrderItem::id)

    @Test fun draftRestoresIdsAgainstReloadedMetadataAndDoesNotResubmitItself() = runBlocking {
        val handle = SavedStateHandle()
        var saves = 0
        val first = PlaylistOrderEditor(handle, this, { Resource.Success(items(1, 2, 3)) }, { _, _ -> saves++; Resource.Success(BaseResponse(200)) })
        first.prepare(42, listOf(1, 2, 3)); yield()
        first.move(0, 2)
        val restoredHandle = SavedStateHandle(mapOf("playlist_order_draft:42" to handle.get<LongArray>("playlist_order_draft:42")!!.clone()))
        val restored = PlaylistOrderEditor(restoredHandle, this, { Resource.Success(items(1, 2, 3)) }, { _, _ -> saves++; Resource.Success(BaseResponse(200)) })
        restored.prepare(42, listOf(1, 2, 3)); yield()
        assertEquals(listOf(2L, 3L, 1L), restored.ids())
        assertFalse(restored.state.value.saving)
        assertEquals(0, saves)
    }
    @Test fun repeatedPreparationDoesNotEraseAnUnsavedDraft() = runBlocking {
        var loads = 0
        val editor = PlaylistOrderEditor(SavedStateHandle(), this, { loads++; Resource.Success(items(1, 2, 3)) }, { _, _ -> Resource.Success(BaseResponse(200)) })
        editor.prepare(42, listOf(1, 2, 3)); yield(); editor.move(0, 2)
        editor.prepare(42, listOf(1, 2, 3)); yield()
        assertEquals(1, loads)
        assertEquals(listOf(2L, 3L, 1L), editor.ids())
    }
    @Test fun saveLivesInOwnerScopeAndCannotBeSubmittedTwiceWhilePending() = runBlocking {
        val pending = CompletableDeferred<Resource<BaseResponse>>()
        val handle = SavedStateHandle()
        var saves = 0
        val editor = PlaylistOrderEditor(handle, this, { Resource.Success(items(1, 2, 3)) }, { _, _ -> saves++; pending.await() })
        editor.prepare(42, listOf(1, 2, 3)); yield(); editor.move(0, 2); editor.finish(); yield()
        assertTrue(editor.state.value.saving)
        editor.prepare(42, listOf(1, 2, 3)); editor.finish(); editor.move(0, 1)
        pending.complete(Resource.Success(BaseResponse(200))); yield()
        assertEquals(1, saves)
        assertFalse(editor.state.value.saving)
        assertEquals(listOf(2L, 3L, 1L), editor.ids())
        assertNull(handle.get<LongArray>("playlist_order_draft:42"))
    }
    @Test fun failedSaveRollsBackAndRemovesThePersistedDraft() = runBlocking {
        val handle = SavedStateHandle()
        val editor = PlaylistOrderEditor(handle, this, { Resource.Success(items(1, 2, 3)) }, { _, _ -> Resource.Error("offline") })
        editor.prepare(42, listOf(1, 2, 3)); yield(); editor.move(0, 2); editor.finish(); yield()
        assertEquals(listOf(1L, 2L, 3L), editor.ids())
        assertFalse(editor.state.value.saving)
        assertNull(handle.get<LongArray>("playlist_order_draft:42"))
    }
    @Test fun remoteMembershipChangeDiscardsAnIncompatibleSavedDraft() = runBlocking {
        val handle = SavedStateHandle(mapOf("playlist_order_draft:42" to longArrayOf(3, 2, 1)))
        val editor = PlaylistOrderEditor(handle, this, { Resource.Success(items(1, 4, 3)) }, { _, _ -> Resource.Success(BaseResponse(200)) })
        editor.prepare(42, listOf(1, 4, 3)); yield()
        assertEquals(listOf(1L, 4L, 3L), editor.ids())
        assertNull(handle.get<LongArray>("playlist_order_draft:42"))
    }
    @Test fun lateCancelledPreparationCannotOverwriteANewerPlaylist() = runBlocking {
        val old = CompletableDeferred<Resource<List<PlaylistOrderItem>>>()
        val editor = PlaylistOrderEditor(SavedStateHandle(), this, { id ->
            if (id == 1L) withContext(NonCancellable) { old.await() } else Resource.Success(items(8, 9))
        }, { _, _ -> Resource.Success(BaseResponse(200)) })
        editor.prepare(1, listOf(1, 2)); yield(); editor.prepare(2, listOf(8, 9)); yield()
        old.complete(Resource.Success(items(1, 2))); yield(); yield()
        assertEquals(listOf(8L, 9L), editor.ids())
    }
    @Test fun alreadyAcknowledgedDraftIsClearedWithoutAnotherServerWrite() = runBlocking {
        val handle = SavedStateHandle(mapOf("playlist_order_draft:42" to longArrayOf(3, 2, 1)))
        var writes = 0
        val editor = PlaylistOrderEditor(handle, this, { Resource.Success(items(3, 2, 1)) }, { _, _ -> writes++; Resource.Success(BaseResponse(200)) })
        editor.prepare(42, listOf(3, 2, 1)); yield(); editor.finish(); yield()
        assertEquals(listOf(3L, 2L, 1L), editor.ids())
        assertNull(handle.get<LongArray>("playlist_order_draft:42"))
        assertEquals(0, writes)
    }

}
