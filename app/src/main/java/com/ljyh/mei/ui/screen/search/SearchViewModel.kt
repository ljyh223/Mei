package com.ljyh.mei.ui.screen.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.SearchResults
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.SearchRepository
import com.ljyh.mei.playback.source.musicfree.MusicFreePlugin
import com.ljyh.mei.playback.source.musicfree.MusicFreeStore
import com.ljyh.mei.playback.source.musicfree.MusicFreeTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
    private val musicFree: MusicFreeStore,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val input = MutableStateFlow("")
    private val tracks = mutableMapOf<Long, MusicFreeTrack>()
    private val _sources = MutableStateFlow<List<MusicFreePlugin>>(emptyList())
    val sources = _sources.asStateFlow()
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    private var preparationRevision = 0L
    private var menuPreparation: Job? = null

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchSuggest = input.debounce(300).distinctUntilChanged().mapLatest { query ->
        if (query.isBlank()) Resource.Loading else repository.searchSuggest(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading)

    private val results = SearchResultsController(
        viewModelScope,
        initialSongSource = saved.get<String>("source")?.takeIf { it.isNotBlank() },
        sourceSearch = { query, source, offset ->
            try {
                val page = musicFree.search(source, query, offset + 1)
                page.tracks.forEach { tracks[it.mediaId] = it }
                Resource.Success(SearchResults(songs = page.tracks.map { it.metadata() },
                    hasMore = !page.isEnd, nextOffset = offset + 1))
            } catch (_: TimeoutCancellationException) {
                Resource.Error("搜索超时，请重试或更换音源")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Resource.Error("搜索失败，请检查插件配置与网络")
            }
        },
    ) { query, type, offset ->
        if (type == SearchType.All) repository.overview(query)
        else repository.search(query, type.type, limit = 30, offset = offset)
    }
    val currentQuery = results.query
    val currentTab = results.tab
    val currentSource = results.songSource
    val pageState = results.page

    init {
        viewModelScope.launch {
            musicFree.plugins.collect { installed ->
                val available = installed.filter { it.enabled && it.canSearch }
                val previous = _sources.value
                _sources.value = available
                if (currentSource.value != null && available.none { it.id == currentSource.value }) {
                    cancelPreparation()
                    saved["source"] = null
                    results.selectSource(null, selectSong = false)
                }
                previous.forEach { old ->
                    if (available.firstOrNull { it.id == old.id } != old) {
                        if (currentSource.value == old.id) cancelPreparation()
                        results.invalidateSource(old.id)
                    }
                }
            }
        }
    }

    fun updateInputQuery(query: String) { input.value = query }
    fun onSearchInit(query: String, type: Int) {
        if (query.trim() != currentQuery.value) {
            cancelPreparation()
            tracks.clear()
        }
        results.initialize(query, type)
    }
    fun onTabChange(type: SearchType) {
        if (type != currentTab.value) cancelPreparation()
        results.select(type)
    }
    fun onSourceChange(id: String?) {
        if (id != null && sources.value.none { it.id == id }) return
        if (id != currentSource.value || currentTab.value != SearchType.Song) cancelPreparation()
        saved["source"] = id
        results.selectSource(id)
    }
    fun retry() = results.retry()
    fun loadMore() = results.loadMore()

    internal fun cancelPreparation() {
        preparationRevision++
        menuPreparation?.cancel()
    }

    /** The service starts the queue command at click time and owns cancellation of its setup. */
    internal fun queuePreparation(songs: List<MediaMetadata>): suspend () -> Unit {
        cancelPreparation()
        return trackPreparation(songs)
    }

    fun prepareSongs(songs: List<MediaMetadata>, onReady: () -> Unit) {
        menuPreparation?.cancel()
        val prepare = trackPreparation(songs)
        menuPreparation = viewModelScope.launch {
            prepare()
            onReady()
        }
    }

    private fun trackPreparation(songs: List<MediaMetadata>): suspend () -> Unit {
        val originals = songs.filter { it.id < 0 }.map { tracks[it.id] }
        if (originals.isEmpty()) return {}
        val revision = preparationRevision
        return {
            if (revision != preparationRevision) throw CancellationException()
            try {
                check(originals.none { it == null }) { "Missing original plugin track" }
                musicFree.prepareQueue(originals.filterNotNull())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (revision == preparationRevision) messages.trySend(
                    if (originals.any { it == null }) "歌曲信息已失效，请重新搜索"
                    else "无法准备播放，请重试或检查音源",
                )
                throw CancellationException()
            }
            if (revision != preparationRevision) throw CancellationException()
        }
    }

    override fun onCleared() = cancelPreparation()
}
