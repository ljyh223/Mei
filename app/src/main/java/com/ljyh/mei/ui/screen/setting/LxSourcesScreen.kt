package com.ljyh.mei.ui.screen.setting

import com.ljyh.mei.ui.component.glass.MeiTopAppBar
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.ui.screen.setting.component.AudioSourceModePreference
import com.ljyh.mei.utils.preferences.AudioSourceMode
import com.ljyh.mei.utils.preferences.AudioSourceModeKey
import com.ljyh.mei.playback.source.lx.sourceDisplayName
import com.ljyh.mei.ui.local.LocalNavController
import androidx.compose.foundation.layout.asPaddingValues
import com.ljyh.mei.ui.component.glass.MeiPinnedLazyPage
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeSource
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.ui.component.layout.meiPageGutter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LxSourcesScreen(scrollBehavior: TopAppBarScrollBehavior, viewModel: LxSourcesViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    val (audioSourceMode, onAudioSourceModeChange) = rememberEnumPreference(AudioSourceModeKey, AudioSourceMode.Lx)
    val pageHaze = rememberHazeState()
    val scripts by viewModel.scripts.collectAsState()
    val statuses by viewModel.status.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val urlImport by viewModel.urlImport.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showUrlImport by rememberSaveable { mutableStateOf(false) }
    var urlInput by rememberSaveable { mutableStateOf("") }
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri.toString()
    }
    LaunchedEffect(viewModel) { viewModel.events.collect { snackbar.showSnackbar(it) } }

    LaunchedEffect(urlImport) {
        if (urlImport == UrlImportState.Success) { showUrlImport = false; urlInput = "" }
    }
    if (showUrlImport) {
        val importing = urlImport == UrlImportState.Loading
        val error = (urlImport as? UrlImportState.Failed)?.message
        AlertDialog(
            onDismissRequest = { viewModel.cancelUrlImport(); showUrlImport = false },
            title = { Text("从链接导入") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it; viewModel.clearUrlError() },
                        label = { Text("JS 文件链接") },
                        placeholder = { Text("https://example.com/source.js") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (!importing && urlInput.isNotBlank()) viewModel.importUrl(urlInput) }),
                        singleLine = true, enabled = !importing, isError = error != null,
                        supportingText = if (error != null) { { Text(error) } } else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (importing) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("正在下载并检查脚本…", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("请使用直接返回 JS 文件的链接。脚本可以访问网络并接收待播放歌曲的信息，请仅导入可信来源。",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.importUrl(urlInput) }, enabled = !importing && urlInput.isNotBlank()) {
                    Text(if (importing) "导入中…" else "导入")
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.cancelUrlImport(); showUrlImport = false }) { Text(if (importing) "取消导入" else "取消") } },
        )
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("导入音源？") },
            text = { Text("脚本运行时可以访问网络，并会收到待播放歌曲的信息。请仅导入你信任的脚本。") },
            confirmButton = { TextButton(onClick = { pendingImport = null; viewModel.import(Uri.parse(uri)) }) { Text("导入") } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("取消") } },
        )
    }
    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除音源？") },
            text = { Text("删除后需要重新导入脚本才能使用。") },
            confirmButton = { TextButton(onClick = { pendingDelete = null; viewModel.delete(id) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
    Scaffold(containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snackbar) }) {
        MeiPinnedLazyPage(
            title = "自定义音源",
            bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
            horizontalContentPadding = meiPageGutter(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            onNavigateBack = navController::navigateUp,
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { viewModel.beginUrlImport(); showUrlImport = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("从链接导入")
                    }
                    OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("从文件导入")
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    AudioSourceModePreference(audioSourceMode, onAudioSourceModeChange)
                    Text(
                        if (audioSourceMode == AudioSourceMode.Lx) {
                            "网易云无法播放或返回试听时自动尝试。排序靠前的音源先启动，优先使用最先通过校验的结果。"
                        } else {
                            "当前未启用 LX。已导入的脚本会保留，选择 LX 自定义音源后用于播放。"
                        },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val list = scripts
            if (list == null) {
                item { CircularProgressIndicator(Modifier.size(32.dp)) }
            } else if (list.isEmpty()) {
                item { Text("尚未导入音源", style = MaterialTheme.typography.titleMedium) }
            } else {
                itemsIndexed(list, key = { _, script -> script.id }) { index, script ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(script.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                val detail = listOf(script.version, script.author).filter { it.isNotBlank() }.joinToString(" · ")
                                if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Switch(checked = script.enabled, onCheckedChange = { viewModel.enable(script.id, it) }, enabled = !busy,
                                modifier = Modifier.semanticsLabel("启用 ${script.name}"))
                        }
                        Text(if (!script.enabled) "已停用" else statuses[script.id] ?: "已启用 · ${script.sources.keys.joinToString(" / ") { sourceDisplayName(it) }}",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { viewModel.check(script.id) }, enabled = !busy) { Text("检查脚本") }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { viewModel.move(script.id, -1) }, enabled = !busy && index > 0) {
                                Icon(Icons.Rounded.ArrowUpward, "提高 ${script.name} 的优先级")
                            }
                            IconButton(onClick = { viewModel.move(script.id, 1) }, enabled = !busy && index < list.lastIndex) {
                                Icon(Icons.Rounded.ArrowDownward, "降低 ${script.name} 的优先级")
                            }
                            IconButton(onClick = { pendingDelete = script.id }, enabled = !busy) { Icon(Icons.Rounded.DeleteOutline, "删除 ${script.name}") }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private fun Modifier.semanticsLabel(label: String): Modifier = this.then(Modifier.semantics { contentDescription = label })
