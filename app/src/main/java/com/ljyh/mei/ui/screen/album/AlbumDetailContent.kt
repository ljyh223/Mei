package com.ljyh.mei.ui.screen.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.domain.AlbumOverview
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumDetailContent(
    album: AlbumOverview,
    songs: List<MediaMetadata>,
    displayedSongs: List<MediaMetadata>,
    isSubscribed: Boolean,
    canPlay: Boolean,
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onSubscribe: () -> Unit,
    onDownload: () -> Unit,
    onTrackClick: (MediaMetadata, Int) -> Unit,
    onTrackMore: (MediaMetadata) -> Unit,
) {
    val listState = rememberLazyListState()
    val titleInBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val surface = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val songIndexById = remember(songs) { songs.withIndex().associate { it.value.id to it.index } }
    var showDescriptionSheet by remember(album.id) { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(surface)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
        ) {
            item(key = "album_header") {
                AlbumHeader(album = album, trackCount = songs.size)
            }
            item(key = "album_actions") {
                AlbumActions(
                    hasSongs = songs.isNotEmpty(),
                    canPlay = canPlay,
                    isSubscribed = isSubscribed,
                    onPlayAll = onPlayAll,
                    onSubscribe = onSubscribe,
                    onDownload = onDownload,
                )
            }
            if (album.description.isNotBlank()) {
                item(key = "album_description") {
                    AlbumDescription(album.id, album.description, onExpand = { showDescriptionSheet = true })
                }
            }
            item(key = "album_tracks_title") {
                Text(
                    text = if (searchQuery.isBlank()) "歌曲 · ${songs.size}" else "搜索结果 · ${displayedSongs.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
                )
            }
            itemsIndexed(displayedSongs, key = { _, song -> song.id }) { index, song ->
                AlbumTrackRow(
                    song = song,
                    index = songIndexById[song.id] ?: index,
                    onClick = { onTrackClick(song, index) },
                    onMore = { onTrackMore(song) },
                    canPlay = canPlay,
                )
            }
            if (displayedSongs.isEmpty()) {
                item(key = "album_empty") {
                    Text(
                        text = if (searchQuery.isNotBlank()) "没有找到匹配的歌曲" else "暂无歌曲",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        CenterAlignedTopAppBar(
            title = {
                if (isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("搜索专辑歌曲") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                } else if (titleInBar) {
                    Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (isSearchActive) onSearchActiveChange(false) else onBack()
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = {
                    if (isSearchActive) onSearchQueryChange("")
                    onSearchActiveChange(!isSearchActive)
                }) {
                    Icon(
                        if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchActive) "关闭搜索" else "搜索专辑歌曲",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (titleInBar || isSearchActive) surface else Color.Transparent,
                navigationIconContentColor = if (titleInBar || isSearchActive) textColor else Color.White,
                actionIconContentColor = if (titleInBar || isSearchActive) textColor else Color.White,
                titleContentColor = textColor,
            ),
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    if (showDescriptionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDescriptionSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            modifier = Modifier.heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.7f),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                )
                Text(
                    text = album.description.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AlbumHeader(album: AlbumOverview, trackCount: Int) {
    val surface = MaterialTheme.colorScheme.surface
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val heroHeight = (maxWidth * 1.24f).coerceIn(400.dp, 560.dp)
        Box(Modifier.fillMaxWidth().height(heroHeight)) {
            if (album.picUrl.isNotBlank()) {
                AsyncImage(
                    model = album.picUrl,
                    contentDescription = "专辑《${album.name}》封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        0.82f to surface.copy(alpha = 0.78f),
                        1f to surface,
                    )
                )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (album.artistNames.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = album.artistNames.joinToString(" / "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                val year = remember(album.publishTime) {
                    album.publishTime.takeIf { it > 0 }?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).year
                    }
                }
                Text(
                    text = listOfNotNull("专辑", year?.toString(), "${album.size.takeIf { it > 0 } ?: trackCount} 首").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AlbumActions(
    hasSongs: Boolean,
    canPlay: Boolean,
    isSubscribed: Boolean,
    onPlayAll: () -> Unit,
    onSubscribe: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            .widthIn(max = 300.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onPlayAll,
            enabled = hasSongs && canPlay,
            modifier = Modifier.weight(1f).height(48.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("播放全部")
        }
        FilledTonalIconButton(onClick = onSubscribe, modifier = Modifier.size(48.dp)) {
            Icon(
                if (isSubscribed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isSubscribed) "取消收藏专辑" else "收藏专辑",
                modifier = Modifier.size(20.dp),
            )
        }
        FilledTonalIconButton(onClick = onDownload, enabled = hasSongs, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Download, contentDescription = "下载专辑", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AlbumDescription(albumId: Long, description: String, onExpand: () -> Unit) {
    var canExpand by remember(albumId, description) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = description.trim(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { canExpand = it.hasVisualOverflow },
        )
        if (canExpand) {
            TextButton(
                onClick = onExpand,
                modifier = Modifier.size(width = 48.dp, height = 48.dp),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                Text("展开")
            }
        }
    }
}

@Composable
private fun AlbumTrackRow(
    song: MediaMetadata,
    index: Int,
    onClick: () -> Unit,
    onMore: () -> Unit,
    canPlay: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(enabled = canPlay, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (index + 1).toString(),
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artists.joinToString(" / ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMore, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "更多：${song.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
