package com.ljyh.mei.ui.screen.local

import android.content.IntentSender
import android.app.RecoverableSecurityException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.LocalMetadataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocalMatchState(
    val song: Song? = null,
    val query: String = "",
    val matches: List<MediaMetadata> = emptyList(),
    val selected: MediaMetadata? = null,
    val searching: Boolean = false,
    val saving: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class LocalMetadataViewModel @Inject constructor(
    private val repository: LocalMetadataRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LocalMatchState())
    val state: StateFlow<LocalMatchState> = _state
    private val _writePermission = MutableSharedFlow<IntentSender>(extraBufferCapacity = 1)
    val writePermission = _writePermission.asSharedFlow()
    private var searchJob: Job? = null

    fun open(song: Song) {
        if (_state.value.song?.id == song.id) return
        val title = song.title.takeUnless { it == "未知歌曲" }.orEmpty()
        val query = listOf(title, song.artist.firstOrNull().orEmpty())
            .filter(String::isNotBlank).joinToString(" ")
        _state.value = LocalMatchState(song = song, query = query)
        if (query.isNotBlank()) search()
    }

    fun close() {
        searchJob?.cancel()
        _state.value = LocalMatchState()
    }

    fun updateQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun select(candidate: MediaMetadata) {
        _state.value = _state.value.copy(selected = candidate, message = null)
    }

    fun search() {
        val songId = _state.value.song?.id ?: return
        val query = _state.value.query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(searching = true, matches = emptyList(), selected = null, message = null)
            try {
                val result = repository.findMatches(query)
                if (_state.value.song?.id != songId) return@launch
                _state.value = when (result) {
                    is Resource.Success -> _state.value.copy(searching = false, matches = result.data,
                        message = if (result.data.isEmpty()) "没有找到候选歌曲，请调整关键词" else null)
                    is Resource.Error -> _state.value.copy(searching = false, message = result.message)
                    Resource.Loading -> _state.value.copy(searching = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (_state.value.song?.id == songId) {
                    _state.value = _state.value.copy(searching = false, message = error.message ?: "搜索失败")
                }
            }
        }
    }

    fun saveDisplay() = save(writeFile = false)

    fun writeToFile() = save(writeFile = true)

    private fun save(writeFile: Boolean) {
        val songId = _state.value.song?.id ?: return
        val selected = _state.value.selected ?: return
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            try {
                if (writeFile) repository.writeToFile(songId, selected)
                else repository.saveDisplay(songId, selected)
                if (_state.value.song?.id == songId) {
                    _state.value = _state.value.copy(saving = false,
                        message = if (writeFile) "已写入音频文件" else "已补全显示信息")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (security: RecoverableSecurityException) {
                _state.value = _state.value.copy(saving = false)
                _writePermission.emit(security.userAction.actionIntent.intentSender)
            } catch (error: Exception) {
                if (_state.value.song?.id == songId) {
                    _state.value = _state.value.copy(saving = false,
                        message = error.message ?: "补全失败")
                }
            }
        }
    }
}
