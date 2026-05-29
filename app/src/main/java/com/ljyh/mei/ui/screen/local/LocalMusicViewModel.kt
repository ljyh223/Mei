package com.ljyh.mei.ui.screen.local

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.utils.DownloadManager
import com.ljyh.mei.utils.LocalMusicScanner
import com.ljyh.mei.utils.PermissionsUtils
import com.ljyh.mei.utils.ScanProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
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

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission

    private var initialScanAttempted = false

    private val scanner = LocalMusicScanner(
        application.applicationContext,
        com.ljyh.mei.di.repository.SongRepository(db.songDao()),
        com.ljyh.mei.di.repository.ScanFolderRepository(db.scanFolderDao()),
        com.ljyh.mei.di.repository.LocalPlaylistRepository(db.playlistDao()),
        com.ljyh.mei.di.repository.PlaylistSongCrossRefRepository(db.playlistSongCrossRefDao())
    )

    init {
        checkPermission()
    }

    fun checkPermission() {
        _hasPermission.value = PermissionsUtils.checkFilesPermissions(getApplication())
    }

    fun scanAllMusic() {
        if (!_hasPermission.value || initialScanAttempted) return
        initialScanAttempted = true
        viewModelScope.launch {
            _scanState.value = ScanState(isScanning = true)
            try {
                val downloadPath = DownloadManager.getDefaultDownloadPath()
                scanner.scanAllMusic(
                    rootPath = downloadPath,
                    onProgress = { progress ->
                        _scanState.value = ScanState(isScanning = true, progress = progress)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "scanAllMusic failed")
            }
            _scanState.value = ScanState(isScanning = false)
        }
    }
}
