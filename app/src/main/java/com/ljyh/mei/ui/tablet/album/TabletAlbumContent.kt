package com.ljyh.mei.ui.tablet.album

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.glass.*
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.menu.MeiDetailMoreButton
import com.ljyh.mei.ui.component.sheet.AlbumArtistBottomSheet
import com.ljyh.mei.ui.component.sheet.DescriptionSheet
import com.ljyh.mei.ui.screen.album.*
import com.ljyh.mei.ui.tablet.*
import com.ljyh.mei.ui.tablet.component.*
import com.ljyh.mei.utils.share.MediaShareLink
import java.time.Instant
import java.time.ZoneId

@Composable
internal fun TabletAlbumContent(state: AlbumDetailUiState, actions: AlbumDetailActions) {
    AdaptiveTabletContent(compact = { AlbumDetailContent(state, actions) }) { metrics ->
        val album = state.album
        val side = rememberLazyListState()
        val songs = rememberLazyListState()
        val bar = rememberMeiArtworkTopBarState(side, album.id)
        val artists = remember(album.artistIds, album.artistNames, state.songs) { albumDisplayArtists(album, state.songs) }
        val indices = remember(state.songs) { state.songs.withIndex().associate { it.value.id to it.index } }
        var description by rememberSaveable(album.id) { mutableStateOf(false) }
        var artistSheet by rememberSaveable(album.id) { mutableStateOf(false) }
        val focus = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        fun changeSearch(active: Boolean) {
            if (!active) focusManager.clearFocus()
            actions.onSearchActiveChange(active)
        }
        LaunchedEffect(state.isSearchActive) {
            if (state.isSearchActive) {
                songs.scrollToItem(0)
                withFrameNanos { }
                focus.requestFocus()
            }
        }
        val gutter = meiPageGutter()
        TabletDetailPage(
            title = album.name, sideWidth = metrics.sideWidthDp.dp,
            onBack = { if (state.isSearchActive) changeSearch(false) else actions.onBack() },
            backgroundMode = meiArtworkTopBarBackground(bar.artworkPassed), toolbarModifier = bar.toolbarModifier,
            actions = {
                MeiDetailMoreButton(shareContent = MediaShareLink.album(album), searchActive = state.isSearchActive,
                    searchLabel = "搜索专辑歌曲", onSearchActiveChange = ::changeSearch)
            },
            sidePane = { padding ->
                LazyColumn(state = side, modifier = Modifier.fillMaxSize(), contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item(key = "album_artwork") {
                        Box(Modifier.padding(horizontal = gutter)) { TabletArtwork(album.picUrl, bar.artworkModifier) }
                    }
                    item(key = "album_identity") {
                        Column(Modifier.padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(album.name, style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            artists.firstOrNull()?.let { artist ->
                                TextButton(
                                    onClick = { if (artists.size > 1) artistSheet = true else actions.onArtistClick(artist.id) },
                                    enabled = artists.size > 1 || artist.id > 0,
                                    contentPadding = PaddingValues(0.dp),
                                ) { Text(if (artists.size > 1) "${artist.name} 等" else artist.name) }
                            }
                            val year = remember(album.publishTime) { album.publishTime.takeIf { it > 0 }?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).year.toString()
                            } }
                            Text(listOfNotNull(year, "${state.songs.size} 首歌曲").joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item(key = "album_actions") {
                        AlbumActions(
                            tint = MaterialTheme.colorScheme.surface, hasSongs = state.songs.isNotEmpty(), canPlay = state.canPlay,
                            isSubscribed = state.isSubscribed, isSubscriptionLoading = state.isSubscriptionLoading,
                            canSubscribe = state.canSubscribe, onPlayAll = actions.onPlayAll,
                            onSubscribe = actions.onSubscribe, onDownload = actions.onDownload,
                        )
                    }
                    if (album.description.isNotBlank()) item(key = "album_description") {
                        AlbumDescription(album.id, album.description, maxLines = 6) { description = true }
                    }
                }
            },
            mainPane = { padding ->
                LazyColumn(state = songs, modifier = Modifier.fillMaxSize(), contentPadding = padding) {
                    item(key = "album_controls") {
                        Column {
                            TabletSectionHeading(if (state.searchQuery.isNotBlank()) "搜索结果 · ${state.displayedSongs.size}" else "歌曲 · ${state.songs.size}")
                            if (state.isSearchActive) OutlinedTextField(
                                value = state.searchQuery, onValueChange = actions.onSearchQueryChange,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 8.dp).focusRequester(focus),
                                label = { Text("搜索歌名、歌手或专辑") }, singleLine = true,
                            )
                        }
                    }
                    itemsIndexed(state.displayedSongs, key = { _, song -> song.id }) { index, song ->
                        AlbumTrackRow(song, indices[song.id] ?: index, onClick = { actions.onTrackClick(song, index) },
                            onMore = { actions.onTrackMore(song, it) }, canPlay = state.canPlay)
                    }
                    if (state.displayedSongs.isEmpty()) item {
                        Text(if (state.searchQuery.isNotBlank()) "没有找到匹配的歌曲" else "暂无歌曲",
                            Modifier.padding(horizontal = gutter, vertical = 32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
        )
        if (description) DescriptionSheet(album.name, album.description) { description = false }
        if (artistSheet) AlbumArtistBottomSheet(
            albumInfo = MediaMetadata.Album(album.id, album.name), artistList = artists, coverUrl = album.picUrl,
            onAlbumClick = {}, onArtistClick = { artistSheet = false; actions.onArtistClick(it) },
            onDismissRequest = { artistSheet = false }, showAlbumDestination = false,
        )
    }
}
