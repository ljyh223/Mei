package com.ljyh.mei.ui.screen.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.data.repository.LocalMusicScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LocalScanState {
    data object Idle : LocalScanState
    data object Scanning : LocalScanState
    data class Done(val count: Int) : LocalScanState
    data class Failed(val message: String) : LocalScanState
}

@HiltViewModel
class LocalMusicViewModel @Inject constructor(
    private val scanner: LocalMusicScanner,
) : ViewModel() {
    private val _scanState = MutableStateFlow<LocalScanState>(LocalScanState.Idle)
    val scanState: StateFlow<LocalScanState> = _scanState
    private var scanJob: Job? = null

    fun scan() {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            _scanState.value = LocalScanState.Scanning
            try {
                _scanState.value = LocalScanState.Done(scanner.scan())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _scanState.value = LocalScanState.Failed(error.message ?: "扫描本地音乐失败")
            }
        }
    }
}
