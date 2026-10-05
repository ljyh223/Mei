package com.ljyh.mei.ui.screen.main.findmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.audio.match.AudioMatchRepository
import com.ljyh.mei.audio.match.AudioSampleRecorder
import com.ljyh.mei.audio.match.MatchedSong
import com.ljyh.mei.audio.match.WasmAudioFingerprinter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AudioMatchUiState {
    data object Ready : AudioMatchUiState
    data object Recording : AudioMatchUiState
    data object Fingerprinting : AudioMatchUiState
    data object Matching : AudioMatchUiState
    data object NoMatch : AudioMatchUiState
    data class Results(val songs: List<MatchedSong>) : AudioMatchUiState
    data class Failure(val message: String) : AudioMatchUiState
}

@HiltViewModel
class AudioMatchViewModel @Inject constructor(
    private val recorder: AudioSampleRecorder,
    private val fingerprinter: WasmAudioFingerprinter,
    private val repository: AudioMatchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<AudioMatchUiState>(AudioMatchUiState.Ready)
    val state = _state.asStateFlow()
    private var matchJob: Job? = null

    fun start() {
        if (matchJob?.isActive == true) return
        matchJob = viewModelScope.launch {
            try {
                _state.value = AudioMatchUiState.Recording
                val samples = recorder.record()
                _state.value = AudioMatchUiState.Fingerprinting
                val fingerprint = fingerprinter.generate(samples)
                _state.value = AudioMatchUiState.Matching
                val songs = repository.match(fingerprint)
                _state.value = if (songs.isEmpty()) AudioMatchUiState.NoMatch
                else AudioMatchUiState.Results(songs)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val fallback = when (_state.value) {
                    AudioMatchUiState.Recording -> "录音失败，请检查麦克风后重试"
                    AudioMatchUiState.Fingerprinting -> "指纹生成失败，请重试"
                    else -> "连接识曲服务失败，请检查网络后重试"
                }
                val message = error.message?.takeIf { text -> text.any { it in '\u4e00'..'\u9fff' } }
                    ?: fallback
                _state.value = AudioMatchUiState.Failure(message)
            } finally {
                if (matchJob === currentCoroutineContext()[Job]) matchJob = null
            }
        }
    }

    fun cancel() {
        matchJob?.cancel()
        matchJob = null
        _state.value = AudioMatchUiState.Ready
    }

    fun cancelIfRunning() {
        if (matchJob?.isActive == true) cancel()
    }

    fun onPermissionDenied() {
        _state.value = AudioMatchUiState.Failure("需要麦克风权限才能听歌识曲")
    }
}
