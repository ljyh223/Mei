package com.ljyh.mei.ui.screen.local

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.utils.LocalMusicScanner
import com.ljyh.mei.utils.ScanProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanState(
    val isScanning: Boolean = false,
    val progress: ScanProgress? = null
)

@HiltViewModel
class LocalMusicViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    private val _scanState = MutableStateFlow(ScanState())
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val scanner = LocalMusicScanner(
        application.applicationContext,
        com.ljyh.mei.di.repository.SongRepository(db.songDao()),
        com.ljyh.mei.di.repository.ScanFolderRepository(db.scanFolderDao()),
        com.ljyh.mei.di.repository.LocalPlaylistRepository(db.playlistDao()),
        com.ljyh.mei.di.repository.PlaylistSongCrossRefRepository(db.playlistSongCrossRefDao())
    )

    fun scanFolderUri(uri: Uri, label: String? = null, isDefault: Boolean = false) {
        viewModelScope.launch {
            _scanState.value = ScanState(isScanning = true)
            try {
                scanner.scanFolder(
                    uri = uri,
                    label = label,
                    isDefault = isDefault,
                    onProgress = { progress ->
                        _scanState.value = ScanState(isScanning = true, progress = progress)
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _scanState.value = ScanState(isScanning = false)
        }
    }

    fun scanFilePath(path: String, label: String? = null, isDefault: Boolean = false) {
        viewModelScope.launch {
            _scanState.value = ScanState(isScanning = true)
            try {
                scanner.scanFilePaths(
                    rootPath = path,
                    label = label,
                    isDefault = isDefault,
                    onProgress = { progress ->
                        _scanState.value = ScanState(isScanning = true, progress = progress)
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _scanState.value = ScanState(isScanning = false)
        }
    }
}
