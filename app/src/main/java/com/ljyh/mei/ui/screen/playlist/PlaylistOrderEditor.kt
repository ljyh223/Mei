package com.ljyh.mei.ui.screen.playlist

import androidx.lifecycle.SavedStateHandle
import com.ljyh.mei.data.model.api.BaseResponse
import com.ljyh.mei.data.network.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal data class PlaylistOrderState(
    val items: List<PlaylistOrderItem>? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
)

/** ViewModel-owned editor: only IDs are persisted; metadata and server order are reloaded. */
internal class PlaylistOrderEditor(
    private val savedState: SavedStateHandle,
    private val scope: CoroutineScope,
    private val load: suspend (Long) -> Resource<List<PlaylistOrderItem>>,
    private val save: suspend (Long, List<PlaylistOrderItem>) -> Resource<BaseResponse>,
) {
    private val mutableState = MutableStateFlow(PlaylistOrderState())
    val state = mutableState.asStateFlow()
    private val messages = Channel<String>(Channel.BUFFERED)
    val feedback = messages.receiveAsFlow()
    private var playlistId: Long? = null
    private var expectedIds: List<Long> = emptyList()
    private var confirmed: List<PlaylistOrderItem> = emptyList()
    private var preparation: Job? = null
    private var generation = 0
    private fun key(id: Long) = "playlist_order_draft:$id"

    fun prepare(id: Long, ids: List<Long>) {
        if (state.value.saving) return
        if (playlistId == id && expectedIds.toSet() == ids.toSet() &&
            (state.value.items != null || state.value.loading)) return
        preparation?.cancel()
        val request = ++generation
        playlistId = id
        expectedIds = ids.toList()
        mutableState.value = PlaylistOrderState(loading = true)
        preparation = scope.launch {
            val result = try { load(id) } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { Resource.Error(error.message ?: "排序准备失败") }
            if (generation != request) return@launch
            if (result is Resource.Success) {
                confirmed = result.data
                val draft = savedState.get<LongArray>(key(id))?.toList()
                val byId = confirmed.associateBy(PlaylistOrderItem::id)
                val validDraft = draft != null && draft.size == confirmed.size && draft.toSet() == byId.keys
                if (draft != null && !validDraft) {
                    savedState.remove<LongArray>(key(id))
                    messages.trySend("歌单歌曲已变化，已加载最新顺序")
                }
                if (validDraft && draft == confirmed.map(PlaylistOrderItem::id)) savedState.remove<LongArray>(key(id))
                val items = if (validDraft) draft.map(byId::getValue) else confirmed
                mutableState.value = PlaylistOrderState(items = items)
            } else {
                mutableState.value = PlaylistOrderState()
                messages.trySend("排序准备失败，请重新打开歌单重试")
            }
        }
    }

    fun move(from: Int, to: Int) {
        val current = state.value
        val items = current.items ?: return
        if (current.loading || current.saving || from !in items.indices || to !in items.indices) return
        val moved = items.toMutableList().apply { add(to, removeAt(from)) }
        mutableState.value = current.copy(items = moved)
        val id = playlistId ?: return
        if (moved.map(PlaylistOrderItem::id) == confirmed.map(PlaylistOrderItem::id)) savedState.remove<LongArray>(key(id))
        else savedState[key(id)] = moved.map(PlaylistOrderItem::id).toLongArray()
    }

    fun finish() {
        val current = state.value
        val snapshot = current.items ?: return
        val id = playlistId ?: return
        if (current.loading || current.saving || snapshot.map(PlaylistOrderItem::id) == confirmed.map(PlaylistOrderItem::id)) return
        val request = generation
        mutableState.value = current.copy(saving = true)
        scope.launch {
            val result = try { save(id, snapshot) } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { Resource.Error(error.message ?: "排序保存失败") }
            if (generation != request) return@launch
            val success = result is Resource.Success && result.data.code == 200
            if (success) confirmed = snapshot
            savedState.remove<LongArray>(key(id))
            mutableState.value = PlaylistOrderState(items = confirmed)
            messages.trySend(if (success) "歌曲顺序已保存" else "排序保存失败，已恢复原顺序")
        }
    }
}
