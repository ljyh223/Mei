package com.ljyh.mei.ui.screen.album

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.combinedClickable
import com.ljyh.mei.ui.component.MeiCollectionHeader
import com.ljyh.mei.ui.component.dialog.FullScreenImageViewer
import com.ljyh.mei.utils.image.largeImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntRect
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.domain.AlbumOverview
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.sheet.AlbumArtistBottomSheet
import com.ljyh.mei.ui.component.sheet.DescriptionSheet
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumDetailContent(
    album: AlbumOverview,
    coverColor: Color?,
    songs: List<MediaMetadata>,
    displayedSongs: List<MediaMetadata>,
    isSubscribed: Boolean?,
    isSubscriptionLoading: Boolean,
    canSubscribe: Boolean,
    canPlay: Boolean,
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit,
    onPlayAll: () -> Unit,
    onSubscribe: () -> Unit,
    onDownload: () -> Unit,
    onTrackClick: (MediaMetadata, Int) -> Unit,
    onTrackMore: (MediaMetadata, IntRect) -> Unit,
) {
    val listState = rememberLazyListState()
    val titleInBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val surface = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val tintStrength = when {
        coverColor == null -> 0f
        surface.luminance() > 0.5f && coverColor.luminance() < 0.18f -> 0.08f
        surface.luminance() <= 0.5f && coverColor.luminance() > 0.65f -> 0.08f
        surface.luminance() > 0.5f -> 0.14f
        else -> 0.18f
    }
    val albumTint by animateColorAsState(
        targetValue = coverColor?.let { lerp(surface, it, tintStrength) } ?: surface,
        label = "AlbumCoverTint",
    )
    val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val songIndexById = remember(songs) { songs.withIndex().associate { it.value.id to it.index } }
    val participatingArtists = remember(album.artistIds, songs) {
        val albumArtistIds = album.artistIds.toSet()
        songs.flatMap { it.artists }
            .filter { it.name.isNotBlank() && it.id !in albumArtistIds }
            .distinctBy { if (it.id > 0) it.id.toString() else it.name }
    }
    val allArtists = remember(album.artistIds, album.artistNames, participatingArtists) {
        album.artistNames.mapIndexed { index, name ->
            MediaMetadata.Artist(album.artistIds.getOrNull(index) ?: 0L, name)
        }.filter { it.name.isNotBlank() } + participatingArtists
    }
    var showCoverPreview by remember(album.id) { mutableStateOf(false) }
    var showDescriptionSheet by remember(album.id) { mutableStateOf(false) }
    var showArtistsSheet by remember(album.id) { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(surface)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
        ) {
            item(key = "album_header") {
                AlbumHeader(
                    album = album,
                    trackCount = songs.size,
                    artists = allArtists,
                    tint = albumTint,
                    onArtistClick = onArtistClick,
                    onShowArtists = { showArtistsSheet = true },
                    onCoverClick = { showCoverPreview = true },
                )
            }
            item(key = "album_actions") {
                AlbumActions(
                    hasSongs = songs.isNotEmpty(),
                    canPlay = canPlay,
                    isSubscribed = isSubscribed,
                    isSubscriptionLoading = isSubscriptionLoading,
                    canSubscribe = canSubscribe,
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
            if (searchQuery.isNotBlank()) {
                item(key = "album_search_results_title") {
                    Text(
                        text = "搜索结果 · ${displayedSongs.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
                    )
                }
            } else {
                item(key = "album_tracks_spacing") { Spacer(Modifier.height(16.dp)) }
            }
            itemsIndexed(displayedSongs, key = { _, song -> song.id }) { index, song ->
                AlbumTrackRow(
                    song = song,
                    index = songIndexById[song.id] ?: index,
                    onClick = { onTrackClick(song, index) },
                    onMore = { anchor -> onTrackMore(song, anchor) },
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
                containerColor = surface.copy(alpha = 0.96f),
                navigationIconContentColor = textColor,
                actionIconContentColor = textColor,
                titleContentColor = textColor,
            ),
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    if (showCoverPreview && album.picUrl.isNotBlank()) {
        FullScreenImageViewer(album.picUrl) { showCoverPreview = false }
    }
    if (showDescriptionSheet) {
        DescriptionSheet(album.name, album.description) { showDescriptionSheet = false }
    }
    if (showArtistsSheet) {
        AlbumArtistBottomSheet(
            albumInfo = MediaMetadata.Album(album.id, album.name),
            artistList = allArtists,
            coverUrl = album.picUrl,
            onAlbumClick = {},
            onArtistClick = { id ->
                showArtistsSheet = false
                onArtistClick(id)
            },
            onDismissRequest = { showArtistsSheet = false },
            showAlbumDestination = false,
        )
    }
}

@Composable
private fun AlbumHeader(
    album: AlbumOverview,
    trackCount: Int,
    artists: List<MediaMetadata.Artist>,
    tint: Color,
    onArtistClick: (Long) -> Unit,
    onShowArtists: () -> Unit,
    onCoverClick: () -> Unit,
) {
    MeiCollectionHeader(
        title = album.name,
        tint = tint,
        modifier = Modifier.statusBarsPadding().padding(top = 64.dp),
        cover = {
            if (album.picUrl.isNotBlank()) AsyncImage(
                model = album.picUrl.largeImage(),
                contentDescription = "预览专辑《${album.name}》封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().combinedClickable(
                    onClick = onCoverClick, onLongClick = onCoverClick,
                    onClickLabel = "预览专辑封面", onLongClickLabel = "预览专辑封面",
                ),
            )
        },
        metadata = {
            if (artists.isNotEmpty()) {
                val firstArtist = artists.first()
                Text(
                    text = if (artists.size > 1) "${firstArtist.name} 等" else firstArtist.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (artists.size > 1 || firstArtist.id > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.heightIn(min = 48.dp)
                        .clickable(enabled = artists.size > 1 || firstArtist.id > 0) {
                            if (artists.size > 1) onShowArtists() else onArtistClick(firstArtist.id)
                        }.wrapContentHeight(Alignment.CenterVertically),
                )
            }
            val year = remember(album.publishTime) {
                album.publishTime.takeIf { it > 0 }?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).year
                }
            }
            Text(
                text = listOfNotNull("专辑", year?.toString(),
                    "${album.size.takeIf { it > 0 } ?: trackCount} 首").joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun AlbumActions(
    hasSongs: Boolean,
    canPlay: Boolean,
    isSubscribed: Boolean?,
    isSubscriptionLoading: Boolean,
    canSubscribe: Boolean,
    onPlayAll: () -> Unit,
    onSubscribe: () -> Unit,
    onDownload: () -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier.fillMaxWidth().background(surface),
        contentAlignment = Alignment.CenterStart,
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onPlayAll,
                enabled = hasSongs && canPlay,
                modifier = Modifier.widthIn(min = 144.dp).heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("播放全部")
            }
            FilledTonalIconButton(onClick = onSubscribe, enabled = canSubscribe, modifier = Modifier.size(48.dp)) {
                if (isSubscribed == null && isSubscriptionLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        if (isSubscribed == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = when (isSubscribed) {
                            true -> "取消收藏专辑"
                            false -> "收藏专辑"
                            null -> "重试获取收藏状态"
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            FilledTonalIconButton(onClick = onDownload, enabled = hasSongs, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Download, contentDescription = "下载专辑", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AlbumDescription(albumId: Long, description: String, onExpand: () -> Unit) {
    var canExpand by remember(albumId, description) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
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
    onMore: (IntRect) -> Unit,
    canPlay: Boolean,
) {
    var moreBounds by remember(song.id) { mutableStateOf(IntRect.Zero) }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(enabled = canPlay, onClick = onClick)
            .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
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
        IconButton(
            onClick = { onMore(moreBounds) },
            modifier = Modifier.size(48.dp).onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                moreBounds = IntRect(
                    bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt()
                )
            },
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "更多：${song.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
