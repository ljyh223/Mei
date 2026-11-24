package com.ljyh.mei.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.response.ListeningTrack
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

internal data class ListeningArchiveState(
    val allTime: Boolean = false,
    val rank: Resource<List<ListeningTrack>> = Resource.Loading,
    val recent: Resource<List<ListeningTrack>> = Resource.Loading,
    val refreshing: Boolean = false,
    val refreshError: String? = null,
)

internal interface ListeningArchiveDataSource {
    suspend fun rank(uid: String, allTime: Boolean): Resource<List<ListeningTrack>>
    suspend fun recent(): Resource<List<ListeningTrack>>
}

private class RepositoryListeningArchiveSource(private val user: UserRepository) : ListeningArchiveDataSource {
    override suspend fun rank(uid: String, allTime: Boolean) = user.getListeningRank(uid, allTime)
    override suspend fun recent() = user.getRecentListening()
}

/** Lives with the library entry; switching periods and revisiting the archive reuse loaded data. */
@HiltViewModel
internal class ListeningArchiveViewModel internal constructor(
    private val source: ListeningArchiveDataSource,
    scopeOverride: CoroutineScope? = null,
) : ViewModel() {
    @Inject constructor(repository: UserRepository) : this(RepositoryListeningArchiveSource(repository))
    private val requestScope = scopeOverride ?: viewModelScope
    private var generation = 0
    private val _state = MutableStateFlow(ListeningArchiveState())
    val state = _state.asStateFlow()
    private var identity: Pair<String, String>? = null
    private var job: Job? = null
    private val ranks = mutableMapOf<Boolean, Resource<List<ListeningTrack>>>()
    private var recent: Resource<List<ListeningTrack>> = Resource.Loading

    fun bind(uid: String?, cookie: String?) {
        if (cookie == null) return
        val next = uid.orEmpty() to cookie
        if (identity == next) return
        identity = next
        generation++
        job?.cancel()
        ranks.clear()
        recent = Resource.Loading
        _state.value = ListeningArchiveState()
        if (next.first.isNotBlank() && next.second.isNotBlank()) load()
    }

    fun selectPeriod(allTime: Boolean) {
        if (_state.value.allTime == allTime) return
        _state.value = _state.value.copy(allTime = allTime, rank = ranks[allTime] ?: Resource.Loading)
        load()
    }

    fun refresh() = load(force = true)

    fun refreshRecent() = load(force = true, recentOnly = true)

    private fun load(force: Boolean = false, recentOnly: Boolean = false) {
        val session = identity ?: return
        if (session.first.isBlank() || session.second.isBlank()) return
        if (force && job?.isActive == true) return
        val period = _state.value.allTime
        val request = ++generation
        job?.cancel()
        _state.value = _state.value.copy(refreshing = force, refreshError = null)
        job = requestScope.launch {
            try {
                coroutineScope {
                    launch {
                        if (!recentOnly && (force || period !in ranks)) {
                            val result = source.rank(session.first, period)
                            if (identity != session || request != generation) return@launch
                            if (result is Resource.Error && ranks[period] is Resource.Success)
                                _state.value = _state.value.copy(refreshError = "听歌排行刷新失败，已保留原内容")
                            if (result is Resource.Success || ranks[period] !is Resource.Success) ranks[period] = result
                            _state.value = _state.value.copy(rank = ranks.getValue(period))
                        }
                    }
                    launch {
                        if (force || recent is Resource.Loading) {
                            val result = source.recent()
                            if (identity != session || request != generation) return@launch
                            if (result is Resource.Error && recent is Resource.Success)
                                _state.value = _state.value.copy(refreshError = "最近收听刷新失败，已保留原内容")
                            if (result is Resource.Success || recent !is Resource.Success) recent = result
                            _state.value = _state.value.copy(recent = recent)
                        }
                    }
                }
            } finally {
                if (identity == session && request == generation) _state.value = _state.value.copy(refreshing = false)
            }
        }
    }
}
