package com.ljyh.mei.ui.screen.setting

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ljyh.mei.playback.source.musicfree.*
import com.ljyh.mei.ui.component.glass.MeiPinnedLazyPage
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.local.LocalNavController

@Composable
fun MusicFreePluginsScreen(viewModel: MusicFreePluginsViewModel = hiltViewModel()) {
    val nav = LocalNavController.current
    val context = LocalContext.current
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val subscription by viewModel.subscription.collectAsStateWithLifecycle()
    var urlDialog by rememberSaveable { mutableStateOf(false) }
    var url by rememberSaveable { mutableStateOf("") }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var configuring by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importFile)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (message == "插件已导入") { urlDialog = false; url = "" }
        }
    }
    LaunchedEffect(subscription != null) {
        if (subscription != null) { urlDialog = false; url = "" }
    }
    subscription?.let { state ->
        SubscriptionImportDialog(state, busy, error, viewModel::selectSubscription, viewModel::importSelected, viewModel::closeSubscription)
    }
    if (urlDialog && subscription == null) AlertDialog(
        onDismissRequest = { viewModel.cancel(); urlDialog = false },
        title = { Text("导入 MusicFree 链接") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url, onValueChange = { url = it; viewModel.clearError() },
                    label = { Text("插件或订阅链接") }, singleLine = true, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!busy && url.isNotBlank()) viewModel.importUrl(url) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("支持单个 JS 插件和 JSON 订阅清单。", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.importUrl(url) }, enabled = !busy && url.isNotBlank()) { Text("导入") } },
        dismissButton = { TextButton(onClick = { viewModel.cancel(); urlDialog = false }) { Text("取消") } },
    )
    pendingDelete?.let { id ->
        AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("删除插件？") },
            text = { Text("删除后，该插件的歌曲将无法重新获取播放地址。") },
            confirmButton = { TextButton(onClick = { pendingDelete = null; viewModel.delete(id) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } })
    }
    plugins.firstOrNull { it.id == configuring }?.let { plugin ->
        PluginVariablesDialog(plugin, onDismiss = { configuring = null }, onSave = { values ->
            viewModel.configure(plugin.id, values); configuring = null
        })
    }
    MeiPinnedLazyPage(
        title = "MusicFree 插件", onNavigateBack = nav::navigateUp,
        horizontalContentPadding = meiPageGutter(), verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("import") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { picker.launch(arrayOf("*/*")) }, enabled = !busy) { Text("从文件导入") }
                OutlinedButton(onClick = { viewModel.clearError(); urlDialog = true }, enabled = !busy) { Text("从链接导入") }
            }
        }
        item("notice") {
            Text("在搜索结果「单曲」右侧选择音源。插件可以访问网络，并接收搜索关键词和歌曲信息，请使用可信来源的 MusicFree JS 插件。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (busy && !urlDialog && subscription == null) item("busy") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (error != null && !urlDialog && subscription == null) item("error") { Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        if (plugins.isEmpty() && !busy) item("empty") { Text("导入并启用插件后，在搜索结果「单曲」右侧选择音源。") }
        items(plugins, key = { it.id }) { plugin ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(plugin.name, style = MaterialTheme.typography.titleMedium)
                        Text(listOf(plugin.version, plugin.author).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = plugin.enabled, onCheckedChange = { viewModel.enable(plugin.id, it) }, enabled = !busy,
                        modifier = Modifier.semantics { contentDescription = plugin.name + "，启用音源" })
                }
                Text(listOfNotNull("搜索".takeIf { plugin.canSearch }, "播放解析".takeIf { plugin.canResolve }).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.check(plugin.id) }, enabled = !busy) { Text("检查") }
                    if (plugin.userVariables.isNotEmpty()) TextButton(onClick = { configuring = plugin.id }, enabled = !busy) { Text("配置") }
                    IconButton(onClick = { pendingDelete = plugin.id }, enabled = !busy) { Icon(Icons.Rounded.DeleteOutline, "删除" + plugin.name) }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SubscriptionImportDialog(
    state: SubscriptionImportState,
    busy: Boolean,
    error: String?,
    onSelect: (String, Boolean) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val imported = state.report?.imported.orEmpty().map { it.url }.toSet()
    val failures = state.report?.failures.orEmpty().associateBy { it.entry.url }
    val finished = state.report != null && failures.isEmpty() && state.selected.isEmpty()
    val maxHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.45f).coerceAtMost(400.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择订阅中的插件") },
        text = {
            Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.manifest.description.isNotBlank()) Text(state.manifest.description,
                    style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("已处理 ${state.completed} / ${state.total}，停止后会保留已导入的插件。", style = MaterialTheme.typography.bodySmall)
                } else if (state.report != null) {
                    Text("已导入 ${imported.size} 个插件" + if (failures.isNotEmpty()) "，${failures.size} 个失败，可勾选重试。" else "。",
                        style = MaterialTheme.typography.bodyMedium)
                }
                if (!finished) Text("已选择 ${state.selected.size} 个，每次最多 ${MusicFreeStore.MAX_PLUGINS} 个。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                state.manifest.plugins.forEach { entry ->
                    val done = entry.url in imported
                    val checked = done || entry.url in state.selected
                    val canSelect = !busy && !done && (checked || state.selected.size < MusicFreeStore.MAX_PLUGINS)
                    Row(Modifier.fillMaxWidth().toggleable(value = checked, enabled = canSelect, role = Role.Checkbox,
                        onValueChange = { onSelect(entry.url, it) }).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = null, enabled = canSelect)
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(entry.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                            if (entry.version.isNotBlank()) Text(entry.version, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (done) Text("已导入", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            failures[entry.url]?.let { Text(it.reason, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (finished && !busy) TextButton(onClick = onDismiss) { Text("完成") }
            else TextButton(onClick = onImport, enabled = !busy && state.selected.isNotEmpty()) {
                Text((if (state.report == null) "导入所选" else "重试所选") + "（${state.selected.size}）")
            }
        },
        dismissButton = {
            if (!finished || busy) TextButton(onClick = onDismiss) { Text(if (busy) "停止" else "关闭") }
        },
    )
}

@Composable
private fun PluginVariablesDialog(plugin: MusicFreePlugin, onDismiss: () -> Unit, onSave: (Map<String, String>) -> Unit) {
    var values by remember(plugin.id) { mutableStateOf(plugin.variables) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(plugin.name + "配置") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                plugin.userVariables.forEach { variable ->
                    val secret = Regex("key|token|secret|password", RegexOption.IGNORE_CASE).containsMatchIn(variable.key)
                    OutlinedTextField(value = values[variable.key].orEmpty(),
                        onValueChange = { value -> values = values + (variable.key to value) },
                        label = { Text(variable.name) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(values) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
