package com.ljyh.mei.ui.screen.playlist

import androidx.lifecycle.SavedStateHandle
import com.ljyh.mei.constants.DownloadQuality
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.network.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal data class PlaylistDownloadSelection(val playlistId: Long, val title: String, val ids: List<Long>)
internal data class PlaylistDownloadOptions(val quality: DownloadQuality, val path: String, val separateLyrics: Boolean)
internal enum class PlaylistDownloadPhase { Idle, Preparing, Confirming, Submitting }
internal data class PlaylistDownloadState(
    val phase: PlaylistDownloadPhase = PlaylistDownloadPhase.Idle,
    val selection: PlaylistDownloadSelection? = null,
    val uncertainSubmission: Boolean = false,
)

/** Retain intent IDs in saved state and run preparation/submission in the owner's scope. */
internal class PlaylistDownloadController(
    private val saved: SavedStateHandle,
    private val scope: CoroutineScope,
    private val prepare: suspend (PlaylistDownloadSelection) -> Resource<List<MediaMetadata>>,
    private val enqueue: suspend (PlaylistDownloadSelection, List<MediaMetadata>, PlaylistDownloadOptions) -> Resource<Int>,
) {
    companion object {
        const val IdsKey = "playlist_download_ids"
        const val PlaylistKey = "playlist_download_playlist"
        const val TitleKey = "playlist_download_title"
        const val PhaseKey = "playlist_download_phase"
        const val UncertainKey = "playlist_download_uncertain"
    }
    private val mutableState = MutableStateFlow(PlaylistDownloadState())
    val state = mutableState.asStateFlow()
    private val messages = Channel<String>(Channel.BUFFERED)
    val feedback = messages.receiveAsFlow()
    private var job: Job? = null
    private var generation = 0
    private var prepared: List<MediaMetadata> = emptyList()

    init {
        val ids = saved.get<LongArray>(IdsKey)?.toList().orEmpty()
        val playlistId = saved.get<Long>(PlaylistKey)
        if (playlistId != null && ids.isNotEmpty()) {
            val uncertain = saved.get<String>(PhaseKey) == PlaylistDownloadPhase.Submitting.name || saved.get<Boolean>(UncertainKey) == true
            start(PlaylistDownloadSelection(playlistId, saved.get<String>(TitleKey).orEmpty(), ids), emptyList(), uncertain)
        }
    }

    fun begin(selection: PlaylistDownloadSelection, knownTracks: List<MediaMetadata> = emptyList()) {
        if (state.value.phase == PlaylistDownloadPhase.Submitting) {
            messages.trySend("正在提交下载任务")
            return
        }
        if (selection.ids.isEmpty()) { messages.trySend("暂无可下载歌曲"); return }
        if (selection.ids.size > 500) { messages.trySend("暂不支持超过500首歌曲下载"); return }
        if (state.value.selection == selection && state.value.phase == PlaylistDownloadPhase.Preparing) return
        start(selection.copy(ids = selection.ids.distinct()), knownTracks, false)
    }

    private fun start(selection: PlaylistDownloadSelection, known: List<MediaMetadata>, uncertain: Boolean) {
        job?.cancel()
        val request = ++generation
        prepared = emptyList()
        saved[IdsKey] = selection.ids.toLongArray()
        saved[PlaylistKey] = selection.playlistId
        saved[TitleKey] = selection.title
        saved[UncertainKey] = uncertain
        phase(PlaylistDownloadState(PlaylistDownloadPhase.Preparing, selection, uncertain))
        val knownById = known.associateBy(MediaMetadata::id)
        if (selection.ids.all(knownById::containsKey)) {
            prepared = selection.ids.map(knownById::getValue)
            phase(state.value.copy(phase = PlaylistDownloadPhase.Confirming))
            return
        }
        job = scope.launch {
            val result = try { prepare(selection) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { Resource.Error(error.message ?: "下载信息准备失败") }
            if (request != generation) return@launch
            val byId = (result as? Resource.Success)?.data?.associateBy(MediaMetadata::id)
            if (byId != null && selection.ids.all(byId::containsKey)) {
                prepared = selection.ids.map(byId::getValue)
                phase(state.value.copy(phase = PlaylistDownloadPhase.Confirming))
            } else {
                clear()
                messages.trySend((result as? Resource.Error)?.message ?: "部分歌曲信息无法获取，请重试")
            }
        }
    }

    fun confirm(options: PlaylistDownloadOptions) {
        val current = state.value
        val selection = current.selection ?: return
        if (current.phase != PlaylistDownloadPhase.Confirming) return
        val tracks = prepared.toList()
        val request = generation
        phase(current.copy(phase = PlaylistDownloadPhase.Submitting))
        job = scope.launch {
            var uncertain = current.uncertainSubmission
            val result = try { enqueue(selection, tracks, options) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    uncertain = true
                    Resource.Error(error.message ?: "下载提交未完成")
                }
            if (request != generation) return@launch
            if (result is Resource.Success) {
                clear()
                messages.trySend("已添加 ${result.data} 首到下载队列")
            } else {
                saved[UncertainKey] = uncertain
                phase(current.copy(uncertainSubmission = uncertain))
                messages.trySend((result as? Resource.Error)?.message ?: "下载提交未完成，请重试")
            }
        }
    }

    fun dismiss() {
        if (state.value.phase == PlaylistDownloadPhase.Submitting) return
        ++generation
        job?.cancel()
        clear()
    }
    private fun phase(value: PlaylistDownloadState) {
        saved[PhaseKey] = value.phase.name
        mutableState.value = value
    }
    private fun clear() {
        for (key in listOf(IdsKey, PlaylistKey, TitleKey, PhaseKey, UncertainKey)) saved.remove<Any>(key)
        prepared = emptyList()
        mutableState.value = PlaylistDownloadState()
    }
}
