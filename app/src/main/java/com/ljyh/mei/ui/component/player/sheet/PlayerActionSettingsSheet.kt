package com.ljyh.mei.ui.component.player.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.player.controls.rememberPlayerActionPreference
import com.ljyh.mei.ui.model.PlayerAction
import com.ljyh.mei.ui.model.PlayerActionProfile
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerActionSettingsSheet(
    onDismiss: () -> Unit,
    embedded: Boolean = false,
    profile: PlayerActionProfile = PlayerActionProfile.Phone,
) {
    val (saved, save) = rememberPlayerActionPreference(profile)
    var actions by remember(profile, saved) { mutableStateOf(profile.parse(saved)) }
    PlayerActionSettingsContainer(onDismiss, embedded) {
        PlayerActionSettingsContent(profile, actions) { updated ->
            actions = updated
            save(PlayerAction.toSettings(updated))
        }
    }
}

/** Selection and ordering edit the same live list; available entries are derived every composition. */
@Composable
internal fun PlayerActionSettingsContent(
    profile: PlayerActionProfile,
    actions: List<PlayerAction>,
    onChange: (List<PlayerAction>) -> Unit,
) {
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = actions.indexOfFirst { it.id == from.key }
        val toIndex = actions.indexOfFirst { it.id == to.key }
        onChange(profile.move(actions, fromIndex, toIndex))
    }
    val firstMovableIndex = if (profile == PlayerActionProfile.Tablet) 1 else 0
    val available = profile.candidates.filterNot(actions::contains)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
    ) {
        item(key = "heading") {
            Text("自定义播放操作栏", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp))
            Text(if (profile == PlayerActionProfile.Tablet) "平板播放器 · 最多 5 项，播放模式固定首位"
                else "手机播放器 · 最多 5 项",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("显示中", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        }
        itemsIndexed(actions, key = { _, action -> action.id }) { index, action ->
            val fixed = profile == PlayerActionProfile.Tablet && index == 0
            ReorderableItem(reorder, key = action.id) { dragging ->
                Row(
                    Modifier.fillMaxWidth().background(if (dragging) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(48.dp).then(if (fixed) Modifier else Modifier.longPressDraggableHandle())
                        .semantics { contentDescription = if (fixed) "固定播放模式" else "拖动调整${action.label}顺序" },
                        contentAlignment = Alignment.Center) { Icon(action.icon, null) }
                    Text(action.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (!fixed) {
                        IconButton(onClick = { onChange(profile.move(actions, index, index - 1)) },
                            enabled = index > firstMovableIndex, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.ArrowUpward, "前移${action.label}")
                        }
                        IconButton(onClick = { onChange(profile.move(actions, index, index + 1)) },
                            enabled = index < actions.lastIndex, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.ArrowDownward, "后移${action.label}")
                        }
                        IconButton(onClick = { onChange(actions - action) }, enabled = actions.size > 1,
                            modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Close, "移除${action.label}") }
                    }
                }
            }
        }
        item(key = "available-heading") {
            Text("添加功能", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        }
        items(available, key = { "available-${it.id}" }) { action ->
            TextButton(
                onClick = { onChange(actions + action) }, enabled = actions.size < 5,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(action.icon, null)
                Spacer(Modifier.width(12.dp))
                Text(action.label, modifier = Modifier.weight(1f))
            }
        }
        item(key = "restore") {
            TextButton(onClick = { onChange(profile.defaults) }, modifier = Modifier.padding(top = 16.dp)) { Text("恢复默认") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerActionSettingsContainer(onDismiss: () -> Unit, embedded: Boolean, content: @Composable () -> Unit) {
    if (embedded) content() else ModalBottomSheet(onDismissRequest = onDismiss) { content() }
}
