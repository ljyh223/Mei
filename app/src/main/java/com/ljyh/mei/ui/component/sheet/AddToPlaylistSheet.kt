package com.ljyh.mei.ui.component.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.room.sortedBySmartScore
import com.ljyh.mei.ui.component.card.StableArtworkText

internal const val AddToPlaylistTitle = "添加到歌单"

@Composable
fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    embedded: Boolean = false,
    trackId: Long? = null,
    containingPlaylistIds: Set<String> = emptySet(),
    onRefreshPlaylist: suspend (String) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val sortedPlaylists = remember(playlists) { playlists.sortedBySmartScore(System.currentTimeMillis()) }
    val matchingPlaylists = if (query.isBlank()) sortedPlaylists else sortedPlaylists.filter {
        it.title.contains(query.trim(), ignoreCase = true)
    }

    val sheetContent: @Composable () -> Unit = {
        val compact = isCompactSideSheet()
        val showSearch = playlists.size >= 8 || query.isNotEmpty()
        val search: @Composable () -> Unit = {
            PlaylistSheetSearch(query, { query = it }, compact)
        }
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = meiSheetContentHeight(.68f)),
        ) {
            if (!compact) {
                Text(
                    text = AddToPlaylistTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
                )
                if (showSearch) search()
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = compact)
                    .padding(bottom = if (compact) 0.dp else 16.dp),
                contentPadding = PaddingValues(bottom = if (compact) 8.dp else 0.dp),
            ) {
                if (compact && showSearch) item(key = "search") { search() }
                item(key = "create") {
                    PlaylistChoiceRow(
                        title = "新建歌单",
                        compact = compact,
                        onClick = onCreateNewPlaylist,
                        leading = {
                            Box(Modifier.size(if (compact) 36.dp else 48.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        },
                    )
                }
                if (!compact) item(key = "divider") {
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
                    LaunchedEffect(playlist.id, trackId) {
                        if (trackId != null && trackId > 0) onRefreshPlaylist(playlist.id)
                    }
                    val alreadyContains = playlist.id in containingPlaylistIds
                    PlaylistChoiceRow(
                        title = playlist.title,
                        subtitle = if (alreadyContains) "${playlist.count}首歌曲 · 已在歌单中" else "${playlist.count}首歌曲",
                        compact = compact,
                        muted = alreadyContains,
                        onClick = { onSelectPlaylist(playlist) },
                        leading = {
                            AsyncImage(
                                model = playlist.cover,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(if (compact) 36.dp else 48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        },
                    )
                }
            }
        }
    }
    if (embedded) {
        sheetContent()
    } else {
        MeiAdaptiveSheet(onDismissRequest = onDismiss, compactTitle = AddToPlaylistTitle) {
            sheetContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistSheetSearch(query: String, onQueryChange: (String) -> Unit, compact: Boolean) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = if (compact) 16.dp else 20.dp, vertical = 4.dp),
        placeholder = { Text("搜索我的歌单") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "清除搜索")
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(if (compact) 24.dp else 12.dp),
        colors = if (compact) OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
        ) else OutlinedTextFieldDefaults.colors(),
    )
}

@Composable
private fun PlaylistChoiceRow(
    title: String,
    compact: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    subtitle: String? = null,
    muted: Boolean = false,
) {
    if (compact) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).alpha(if (muted) 0.5f else 1f)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Column(Modifier.weight(1f)) {
                StableArtworkText(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) StableArtworkText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        ListItem(
            modifier = Modifier.alpha(if (muted) 0.5f else 1f).clickable(onClick = onClick),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = leading,
            headlineContent = {
                Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = if (subtitle == null) null else {
                {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            },
        )
    }
}
