package com.ljyh.mei.ui.screen.artist

import com.ljyh.mei.data.network.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

internal data class ArtistSubscriptionState(
    val subscribed: Boolean? = null,
    val loading: Boolean = false,
    val pending: Boolean = false,
    val authenticated: Boolean = false,
    val error: Boolean = false,
)

/** Account and request revisions prevent old reads or writes from changing a new session. */
internal class ArtistSubscriptionController(
    private val scope: CoroutineScope,
    private val load: suspend (String) -> Resource<Boolean>,
    private val change: suspend (String, Boolean) -> Resource<Unit>,
) {
    private val _state = MutableStateFlow(ArtistSubscriptionState())
    val state = _state.asStateFlow()
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    private var session: Pair<String, String>? = null
    private var job: Job? = null
    private var revision = 0

    fun bind(id: String, account: String, force: Boolean = false) {
        val next = id to account
        if (!force && next == session) return
        session = next
        val generation = ++revision
        job?.cancel()
        val authenticated = account.toLongOrNull()?.let { it > 0 } == true
        _state.value = ArtistSubscriptionState(subscribed = if (authenticated) null else false,
            loading = authenticated, authenticated = authenticated)
        if (!authenticated) return
        job = scope.launch {
            val result = load(id)
            if (generation != revision) return@launch
            _state.value = when (result) {
                is Resource.Success -> ArtistSubscriptionState(result.data, authenticated = true)
                else -> ArtistSubscriptionState(authenticated = true, error = true)
            }
        }
    }

    fun toggle() {
        val current = _state.value
        if (!current.authenticated) { messages.trySend("请先登录后收藏歌手"); return }
        if (current.loading || current.pending) return
        val (id, account) = session ?: return
        val previous = current.subscribed ?: run { bind(id, account, force = true); return }
        val generation = revision
        val subscribed = !previous
        _state.value = current.copy(subscribed = subscribed, pending = true, error = false)
        job = scope.launch {
            try {
                val result = change(id, subscribed)
                if (generation != revision) return@launch
                if (result is Resource.Success) {
                    _state.value = _state.value.copy(pending = false)
                    messages.trySend(if (subscribed) "已收藏歌手" else "已取消收藏")
                } else {
                    _state.value = current.copy(pending = false)
                    messages.trySend("操作失败，请稍后重试")
                }
            } catch (cancelled: CancellationException) {
                if (generation == revision) _state.value = current.copy(pending = false)
                throw cancelled
            } catch (_: Exception) {
                if (generation == revision) {
                    _state.value = current.copy(pending = false)
                    messages.trySend("操作失败，请稍后重试")
                }
            }
        }
    }
}
