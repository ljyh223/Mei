package com.ljyh.mei.ui.screen.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.repository.AppleMusicCoverDiagnostic
import com.ljyh.mei.data.repository.DynamicCoverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DiagnosticState {
    data object Idle : DiagnosticState
    data object Loading : DiagnosticState
    data class Success(val result: AppleMusicCoverDiagnostic) : DiagnosticState
    data class Failed(val message: String) : DiagnosticState
}

@HiltViewModel
class DeveloperSettingsViewModel @Inject constructor(
    private val covers: DynamicCoverRepository
) : ViewModel() {
    private val _state = MutableStateFlow<DiagnosticState>(DiagnosticState.Idle)
    val state = _state.asStateFlow()
    private var queryJob: Job? = null

    fun inspect(album: String, artist: String) {
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            _state.value = DiagnosticState.Loading
            try {
                _state.value = DiagnosticState.Success(covers.inspectAppleMusic(album, artist))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = DiagnosticState.Failed(error.message ?: error.javaClass.simpleName)
            }
        }
    }
}
