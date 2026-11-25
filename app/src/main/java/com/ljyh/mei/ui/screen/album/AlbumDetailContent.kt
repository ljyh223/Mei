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
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.res.painterResource
import com.ljyh.mei.R
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.ljyh.mei.ui.component.glass.MeiHazeTopBar
import com.ljyh.mei.ui.component.glass.MeiTopBarBackground
import com.ljyh.mei.ui.component.glass.meiTopBarHeight
import com.ljyh.mei.ui.component.glass.rememberMeiArtworkTopBarState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntRect
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.sheet.AlbumArtistBottomSheet
import com.ljyh.mei.ui.component.sheet.DescriptionSheet
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumDetailContent(state: AlbumDetailUiState, actions: AlbumDetailActions) {
    with(state) {
        with(actions) {
            val landscape = LocalPhoneLandscape.current
            val listState = rememberLazyListState()
            val artworkBar = rememberMeiArtworkTopBarState(listState, album.id)
            val surface = MaterialTheme.colorScheme.surface
            val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
            val modelHeader = albumArtworkKind(artworkMode, album.publishTime, motionArtwork != null, modelMode, allowMotion = !landscape) == AlbumArtworkKind.Model
            val topPadding = when {
                landscape -> WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                modelHeader -> WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + meiTopBarHeight(hasCustomTitle = isSearchActive)
                else -> 0.dp
            }
            val songIndexById = remember(songs) { songs.withIndex().associate { it.value.id to it.index } }
            val allArtists = remember(album.artistIds, album.artistNames, songs) { albumDisplayArtists(album, songs) }
            var showDescriptionSheet by remember(album.id) { mutableStateOf(false) }
            var showArtistsSheet by remember(album.id) { mutableStateOf(false) }

            val pageHaze = rememberHazeState()
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().hazeSource(pageHaze).background(surface),
                    // The header shares the model's built-in empty edge with the toolbar fade.
                    // Keep system/bar insets in the scroll content without reducing the viewport.
                    contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding + 16.dp),
                ) {
                    item(key = "album_header") {
                        if (landscape) BoxWithConstraints(Modifier.fillMaxWidth().padding(meiPageHorizontalPadding())) {
                            AlbumLandscapeArtworkPane(state, actions,
                                modifier = Modifier.fillMaxWidth().height(minOf(maxWidth, 320.dp)),
                                more = { AlbumDetailMoreButton(state, actions) },
                            )
                        } else AlbumHeader(
                            state = state, actions = actions, artists = allArtists,
                            onShowArtists = { showArtistsSheet = true },
                            artworkModifier = artworkBar.artworkModifier,
                        )
                    }
                    if (!landscape) item(key = "album_actions") {
                        AlbumActions(
                            hasSongs = songs.isNotEmpty(), canPlay = canPlay,
                            onShuffle = onShuffle, onPlayAll = onPlayAll, onDownload = onDownload,
                        )
                    }
                    if (album.description.isNotBlank()) {
                        item(key = "album_description") {
                            AlbumDescription(album.id, album.description, maxLines = 1, onExpand = { showDescriptionSheet = true })
                        }
                    }
                    if (searchQuery.isNotBlank()) {
                        item(key = "album_search_results_title") {
                            Text(
                                text = "搜索结果 · ${displayedSongs.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(meiPageHorizontalPadding()).padding(top = 16.dp, bottom = 8.dp),
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

                if (!landscape) MeiHazeTopBar(
                    modifier = artworkBar.toolbarModifier,
                    state = pageHaze,
                    title = album.name,
                    progress = if (artworkBar.artworkPassed || isSearchActive) 1f else 0f,
                    backgroundProgress = 1f,
                    backgroundMode = if (!modelHeader && !isSearchActive && !artworkBar.artworkPassed) {
                        MeiTopBarBackground.Transparent
                    } else MeiTopBarBackground.Auto,
                    onNavigateBack = { if (isSearchActive) onSearchActiveChange(false) else onBack() },
                    actions = {
                        AlbumDetailMoreButton(
                            state = state, actions = actions,
                            onSearchActiveChange = { active ->
                                if (!active) onSearchQueryChange("")
                                onSearchActiveChange(active)
                            },
                        )
                    },
                    titleContent = if (isSearchActive) {
                        {
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
                        }
                    } else null,
                )
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
    }
}

@Composable
internal fun AlbumActions(
    hasSongs: Boolean,
    canPlay: Boolean,
    onShuffle: () -> Unit,
    onPlayAll: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val secondaryColors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = colors.onSurface.copy(alpha = .08f), contentColor = colors.onSurface,
    )
    BoxWithConstraints(modifier.fillMaxWidth().padding(if (compact) PaddingValues(0.dp) else meiPageHorizontalPadding()).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        val spacing = if (compact) 8.dp else 12.dp
        val playWidth = minOf(144.dp, (maxWidth - 104.dp - spacing * 2).coerceAtLeast(48.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onShuffle, enabled = hasSongs && canPlay,
                modifier = Modifier.size(52.dp), shape = androidx.compose.foundation.shape.CircleShape, colors = secondaryColors) {
                Icon(painterResource(R.drawable.ic_player_shuffle), contentDescription = "随机播放专辑", modifier = Modifier.size(24.dp))
            }
            Button(
                onClick = onPlayAll, enabled = hasSongs && canPlay,
                modifier = Modifier.width(playWidth).heightIn(min = 52.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = colors.onSurface, contentColor = colors.surface),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(6.dp))
                Text("播放", textAlign = TextAlign.Center)
            }
            FilledTonalIconButton(onClick = onDownload, enabled = hasSongs,
                modifier = Modifier.size(52.dp), shape = androidx.compose.foundation.shape.CircleShape, colors = secondaryColors) {
                Icon(Icons.Rounded.Download, contentDescription = "下载专辑", modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
internal fun AlbumDescription(
    albumId: Long,
    description: String,
    maxLines: Int = 1,
    expandBelowDescription: Boolean = false,
    onExpand: () -> Unit,
) {
    var canExpand by remember(albumId, description, maxLines, expandBelowDescription) { mutableStateOf(false) }
    @Composable
    fun DescriptionText(modifier: Modifier) {
        Text(
            text = description.trim(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { canExpand = it.hasVisualOverflow },
        )
    }
    val modifier = Modifier.fillMaxWidth().padding(meiPageHorizontalPadding()).padding(vertical = 2.dp)
    if (expandBelowDescription) {
        Column(modifier) {
            DescriptionText(Modifier.fillMaxWidth())
            if (canExpand) TextButton(
                onClick = onExpand,
                modifier = Modifier.align(Alignment.Start),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) { Text("展开") }
        }
    } else {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            DescriptionText(Modifier.weight(1f))
            if (canExpand) TextButton(
                onClick = onExpand,
                modifier = Modifier.size(width = 48.dp, height = 48.dp),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) { Text("展开") }
        }
    }
}

@Composable
internal fun AlbumTrackRow(
    song: MediaMetadata,
    index: Int,
    onClick: () -> Unit,
    onMore: (IntRect) -> Unit,
    canPlay: Boolean,
) {
    var moreBounds by remember(song.id) { mutableStateOf(IntRect.Zero) }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(enabled = canPlay, onClick = onClick)
            .padding(meiPageHorizontalPadding()).padding(vertical = 8.dp),
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
