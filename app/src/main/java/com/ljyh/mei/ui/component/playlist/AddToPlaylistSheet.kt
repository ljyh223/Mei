package com.ljyh.mei.ui.component.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.sortedBySmartScore
import com.ljyh.mei.utils.image.smallImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    embedded: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.68f
    var query by rememberSaveable { mutableStateOf("") }
    val sortedPlaylists = remember(playlists) { playlists.sortedBySmartScore(System.currentTimeMillis()) }
    val matchingPlaylists = if (query.isBlank()) sortedPlaylists else sortedPlaylists.filter {
        it.title.contains(query.trim(), ignoreCase = true)
    }

    val sheetContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight)
                .then(if (embedded) Modifier else Modifier.navigationBarsPadding()),
        ) {
            Text(
                text = "添加到歌单",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
            )
            if (playlists.size >= 8 || query.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    placeholder = { Text("搜索我的歌单") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "清除搜索")
                        } }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    .padding(bottom = 16.dp),
            ) {
                item(key = "create") {
                    ListItem(
                        headlineContent = { Text("新建歌单") },
                        leadingContent = {
                            Icon(Icons.Default.Add, contentDescription = null,
                                modifier = Modifier.size(48.dp).padding(12.dp))
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable(onClick = onCreateNewPlaylist),
                    )
                }
                item(key = "divider") {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
                if (matchingPlaylists.isEmpty() && query.isNotBlank()) {
                    item(key = "empty") {
                        Text(
                            text = "没有匹配的歌单",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                        )
                    }
                }
                items(matchingPlaylists, key = Playlist::id) { playlist ->
                    PlaylistSelectionItem(playlist) { onSelectPlaylist(playlist) }
                }
            }
        }
    }
    if (embedded) {
        sheetContent()
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            sheetContent()
        }
    }
}

@Composable
private fun PlaylistSelectionItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            AsyncImage(
                model = playlist.cover.smallImage(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        },
        headlineContent = {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
        },
        supportingContent = {
            Text(
                text = "${playlist.count}首歌曲",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}
