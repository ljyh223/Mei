package com.ljyh.mei.ui.screen.setting

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.playback.source.lx.LxScriptStore
import com.ljyh.mei.playback.source.lx.LxScriptImportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

sealed interface UrlImportState {
    data object Idle : UrlImportState
    data object Loading : UrlImportState
    data object Success : UrlImportState
    data class Failed(val message: String) : UrlImportState
}

@HiltViewModel
class LxSourcesViewModel @Inject constructor(private val store: LxScriptStore) : ViewModel() {
    val scripts = store.scripts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val status = store.status
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _urlImport = MutableStateFlow<UrlImportState>(UrlImportState.Idle)
    val urlImport = _urlImport.asStateFlow()
    private var operation: Job? = null
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    fun import(uri: Uri) = action {
        withTimeout(17_000) { store.import(uri) }
        messages.send("音源已导入")
    }
    fun beginUrlImport() { if (!_busy.value) _urlImport.value = UrlImportState.Idle }
    fun clearUrlError() { if (_urlImport.value is UrlImportState.Failed) _urlImport.value = UrlImportState.Idle }
    fun importUrl(input: String) {
        if (_busy.value) return
        _urlImport.value = UrlImportState.Loading
        action(onError = { _urlImport.value = UrlImportState.Failed(it) }) {
            withTimeout(30_000) { store.importUrl(input) }
            _urlImport.value = UrlImportState.Success
            messages.send("音源已导入")
        }
    }
    fun cancelUrlImport() {
        if (_urlImport.value == UrlImportState.Loading) operation?.cancel()
        _urlImport.value = UrlImportState.Idle
    }
    fun enable(id: String, enabled: Boolean) = action { store.enable(id, enabled) }
    fun move(id: String, offset: Int) = action { store.move(id, offset) }
    fun delete(id: String) = action { store.delete(id) }
    fun check(id: String) = action {
        val script = store.snapshot().firstOrNull { it.id == id } ?: return@action
        store.status(id, "正在检查")
        try {
            store.runtime().use(script, store.code(script)) { sources }
            store.status(id, "初始化成功；播放时验证地址")
        } catch (c: CancellationException) { store.status(id, "检查已取消"); throw c }
        catch (_: Exception) { store.status(id, "初始化失败，请更换脚本或检查网络") }
    }
    private fun action(onError: suspend (String) -> Unit = { messages.send(it) }, block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        operation = viewModelScope.launch {
            try { block() }
            catch (_: TimeoutCancellationException) { onError("音源响应超时，请重试或更换脚本") }
            catch (error: LxScriptImportException) { onError(error.message ?: "导入失败，请检查链接或文件") }
            catch (c: CancellationException) { throw c }
            catch (_: Exception) { onError("导入或操作失败，请检查文件、链接和网络后重试") }
            finally { _busy.value = false }
        }
    }
}
