package com.ljyh.mei.ui.screen.setting

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.playback.source.musicfree.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SubscriptionImportState(
    val manifest: MusicFreeSubscription,
    val selected: Set<String>,
    val completed: Int = 0,
    val total: Int = 0,
    val report: MusicFreeBatchImportResult? = null,
)

@HiltViewModel
class MusicFreePluginsViewModel @Inject constructor(private val store: MusicFreeStore) : ViewModel() {
    val plugins = store.plugins.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    private var operation: Job? = null
    private val _subscription = MutableStateFlow<SubscriptionImportState?>(null)
    val subscription = _subscription.asStateFlow()

    fun importFile(uri: Uri) = action("插件已导入") { store.importFile(uri) }
    fun importUrl(url: String) = action {
        when (val document = store.readImportUrl(url)) {
            is MusicFreeImportDocument.Script -> {
                store.importBytes(document.bytes)
                messages.send("插件已导入")
            }
            is MusicFreeImportDocument.Subscription -> {
                currentCoroutineContext().ensureActive()
                _subscription.value = SubscriptionImportState(document.manifest,
                    document.manifest.plugins.takeIf { it.size <= MusicFreeStore.MAX_PLUGINS }?.map { it.url }?.toSet().orEmpty())
            }
        }
    }

    fun selectSubscription(url: String, selected: Boolean) {
        if (_busy.value) return
        _subscription.update { state ->
            state ?: return@update null
            if (state.manifest.plugins.none { it.url == url } || state.report?.imported?.any { it.url == url } == true) return@update state
            val selection = if (selected) state.selected + url else state.selected - url
            if (selection.size > MusicFreeStore.MAX_PLUGINS) return@update state
            state.copy(selected = selection)
        }
    }

    fun importSelected() {
        val state = _subscription.value ?: return
        val entries = state.manifest.plugins.filter { it.url in state.selected }
        if (entries.isEmpty()) return
        action {
            _subscription.update { it?.copy(completed = 0, total = entries.size) }
            val report = store.importSubscription(entries) { completed, total ->
                _subscription.update { it?.copy(completed = completed, total = total) }
            }
            _subscription.update { current -> current?.copy(
                selected = report.failures.map { it.entry.url }.toSet(),
                report = MusicFreeBatchImportResult(
                    (current.report?.imported.orEmpty() + report.imported).distinctBy { it.url },
                    current.report?.failures.orEmpty().filter { old -> entries.none { it.url == old.entry.url } } + report.failures,
                ),
            ) }
        }
    }

    fun closeSubscription() {
        cancel()
        _subscription.value = null
        clearError()
    }
    fun enable(id: String, enabled: Boolean) = action { store.enable(id, enabled) }
    fun delete(id: String) = action("插件已删除") { store.delete(id) }
    fun configure(id: String, values: Map<String, String>) = action("插件配置已保存") { store.configure(id, values) }
    fun check(id: String) = action("插件初始化成功") { store.check(id) }
    fun clearError() { _error.value = null }
    fun cancel() { operation?.cancel() }

    private fun action(success: String? = null, block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        operation = viewModelScope.launch {
            try {
                block()
                success?.let { messages.send(it) }
            } catch (_: TimeoutCancellationException) {
                _error.value = "插件响应超时，请检查网络或更换插件"
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _error.value = musicFreeImportError(error)
            } finally { _busy.value = false }
        }
    }
}
