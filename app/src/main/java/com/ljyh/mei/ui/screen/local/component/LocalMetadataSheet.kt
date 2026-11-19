package com.ljyh.mei.ui.screen.local.component

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.ui.component.sheet.MeiAdaptiveSheet
import com.ljyh.mei.ui.component.sheet.meiSheetContentHeight
import com.ljyh.mei.ui.screen.local.LocalMetadataViewModel

@Composable
fun LocalMetadataSheet(
    song: Song,
    onDismiss: () -> Unit,
    viewModel: LocalMetadataViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var writeError by remember(song.id) { mutableStateOf<String?>(null) }
    val writeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.writeToFile()
        else writeError = "未获得修改文件的权限"
    }
    LaunchedEffect(song.id) { viewModel.open(song) }
    LaunchedEffect(viewModel) {
        viewModel.writePermission.collect { sender ->
            writeLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    fun requestWrite() {
        val path = song.path ?: return
        writeError = null
        if (Build.VERSION.SDK_INT >= 30) {
            runCatching {
                val request = MediaStore.createWriteRequest(context.contentResolver, listOf(Uri.parse(path)))
                writeLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure { writeError = it.message ?: "无法申请文件修改权限" }
        } else {
            viewModel.writeToFile()
        }
    }

    MeiAdaptiveSheet(onDismissRequest = {
        viewModel.close()
        onDismiss()
    }) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = meiSheetContentHeight(.9f)),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item("title") { Text("补全歌曲信息", style = MaterialTheme.typography.titleLarge) }
            item("song") {
                Text(
                    text = "${song.title} · ${song.folderPath.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            item("search") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::updateQuery,
                        label = { Text("歌曲名或歌手") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = viewModel::search, enabled = !state.searching) { Text("搜索") }
                }
            }
            if (state.searching) item("loading") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
            items(state.matches, key = { it.id }) { candidate ->
                val selected = state.selected?.id == candidate.id
                ListItem(
                    headlineContent = { Text(candidate.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Text(
                            candidate.artists.joinToString(" / ") { it.name } + " · " + candidate.album.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        AsyncImage(
                            model = candidate.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier.clickable { viewModel.select(candidate) },
                )
            }
            state.selected?.let { selected ->
                item("selection") {
                    Text(
                        "已选择：${selected.title} · ${selected.artists.joinToString(" / ") { it.name }}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            state.message?.let { message ->
                item("message") { Text(message, style = MaterialTheme.typography.bodySmall) }
            }
            writeError?.let { message ->
                item("write-error") { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            item("actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = viewModel::saveDisplay,
                        enabled = state.selected != null && !state.saving,
                        modifier = Modifier.weight(1f),
                    ) { Text("仅补全显示") }
                    Button(
                        onClick = ::requestWrite,
                        enabled = state.selected != null && !state.saving,
                        modifier = Modifier.weight(1f),
                    ) { Text("写入文件") }
                }
            }
        }
    }
}
