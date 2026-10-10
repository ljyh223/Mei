package com.ljyh.mei.ui.screen.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
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
    val isLightSurface = surface.luminance() > 0.5f
    val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val topProtectionHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 96.dp
    val songIndexById = remember(songs) { songs.withIndex().associate { it.value.id to it.index } }

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
                    AlbumDescription(album.id, album.description)
                }
            }
            item(key = "album_tracks_title") {
                Text(
                    text = if (searchQuery.isBlank()) "歌曲 · ${songs.size}" else "搜索结果 · ${displayedSongs.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
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

        if (!titleInBar && !isSearchActive) {
            val scrimColor = if (isLightSurface) surface else Color.Black
            val scrimAlpha = if (isLightSurface) 0.84f else 0.72f
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().height(topProtectionHeight)
                    .background(
                        Brush.verticalGradient(
                            0f to scrimColor.copy(alpha = scrimAlpha),
                            0.42f to scrimColor.copy(alpha = scrimAlpha),
                            1f to scrimColor.copy(alpha = 0f),
                        )
                    )
            )
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
                navigationIconContentColor = if (titleInBar || isSearchActive || isLightSurface) textColor else Color.White,
                actionIconContentColor = if (titleInBar || isSearchActive || isLightSurface) textColor else Color.White,
                titleContentColor = textColor,
            ),
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun AlbumHeader(album: AlbumOverview, trackCount: Int) {
    val surface = MaterialTheme.colorScheme.surface
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val heroHeight = (maxWidth * 1.08f).coerceIn(360.dp, 480.dp)
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onPlayAll,
            enabled = hasSongs && canPlay,
            modifier = Modifier.weight(1f).height(52.dp),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("播放全部")
        }
        FilledTonalIconButton(onClick = onSubscribe, modifier = Modifier.size(52.dp)) {
            Icon(
                if (isSubscribed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isSubscribed) "取消收藏专辑" else "收藏专辑",
            )
        }
        FilledTonalIconButton(onClick = onDownload, enabled = hasSongs, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Default.Download, contentDescription = "下载专辑")
        }
    }
}

@Composable
private fun AlbumDescription(albumId: Long, description: String) {
    var expanded by remember(albumId) { mutableStateOf(false) }
    var canExpand by remember(albumId, description) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            text = description.trim(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) canExpand = it.hasVisualOverflow },
        )
        if (canExpand || expanded) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                Text(if (expanded) "收起介绍" else "展开介绍")
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
