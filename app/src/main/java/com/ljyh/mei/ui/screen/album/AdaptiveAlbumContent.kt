package com.ljyh.mei.ui.screen.album

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.artwork.CenteredArtworkPane
import com.ljyh.mei.ui.component.artwork.album.*
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.menu.MeiMenuItem
import com.ljyh.mei.ui.component.sheet.AlbumArtistBottomSheet
import com.ljyh.mei.ui.component.sheet.DescriptionSheet
import com.ljyh.mei.ui.tablet.*
import com.ljyh.mei.ui.tablet.component.*

@Composable
internal fun AdaptiveAlbumLayout(
    compact: @Composable () -> Unit,
    expanded: @Composable (TabletContentMetrics) -> Unit,
) {
    val landscape = LocalPhoneLandscape.current
    AdaptiveTabletContent(
        sideWidthFraction = if (landscape) 0.44f else 0.34f,
        minSideWidthDp = if (landscape) 252f else 336f,
        maxSideWidthDp = if (landscape) 400f else 420f,
        minContentWidthDp = if (landscape) 280f else 480f,
        compact = compact,
        expanded = expanded,
    )
}

@Composable
internal fun AdaptiveAlbumContent(state: AlbumDetailUiState, actions: AlbumDetailActions) {
    val staticBackdrop = usesPhoneStaticAlbumBackdrop(state)
    var staticColor by remember(state.album.id, state.album.picUrl, staticBackdrop) { mutableStateOf<Color?>(null) }
    val report by rememberUpdatedState(actions.onStaticArtworkChanged)
    val pageActions = actions.copy(onStaticArtworkChanged = { url, image ->
        if (!staticBackdrop || url != state.album.picUrl) null else {
            staticColor = if (image == null) null else state.coverColor ?: Color(0xFF252A30)
            if (image == null) null else report(url, image)?.also { staticColor = it }
        }
    })
    AlbumArtworkTheme(state, staticColor) { AlbumMediaPage(state, pageActions) }
}

@Composable
private fun AlbumMediaPage(state: AlbumDetailUiState, actions: AlbumDetailActions) {
    val landscape = LocalPhoneLandscape.current
    AdaptiveAlbumLayout(compact = { AlbumDetailContent(state, actions) }) { metrics ->
        val album = state.album
        val songs = rememberLazyListState()
        val artists =
            remember(album.artistIds, album.artistNames, state.songs) { albumDisplayArtists(album, state.songs) }
        val indices = remember(state.songs) { state.songs.withIndex().associate { it.value.id to it.index } }
        var description by rememberSaveable(album.id) { mutableStateOf(false) }
        var artistSheet by rememberSaveable(album.id) { mutableStateOf(false) }
        val firstArtist = artists.firstOrNull()
        val canOpenArtist = artists.size > 1 || (firstArtist?.id ?: 0) > 0
        fun openArtist() {
            if (artists.size > 1) artistSheet = true
            else firstArtist?.id?.takeIf { it > 0 }?.let(actions.onArtistClick)
        }
        val year = remember(album.publishTime) {
            albumReleaseYear(album.publishTime)?.toString()
        }
        val trackSummary = listOfNotNull(year, "${state.songs.size} 首").joinToString(" · ")
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
        val moreButton: @Composable () -> Unit = {
            AlbumDetailMoreButton(
                state = state, actions = actions, onSearchActiveChange = ::changeSearch,
                additionalItems = if (landscape) { dismiss ->
                    if (canOpenArtist) MeiMenuItem("查看歌手", Icons.Rounded.Person) {
                        dismiss(); openArtist()
                    }
                    MeiMenuItem("专辑信息", Icons.Rounded.Info) {
                        dismiss(); description = true
                    }
                } else null,
            )
        }
        TabletDetailPage(
            title = album.name, sideWidth = metrics.sideWidthDp.dp,
            onBack = { if (state.isSearchActive) changeSearch(false) else actions.onBack() },
            showToolbar = !landscape,
            actions = { moreButton() },
            sidePane = { padding ->
                if (landscape) {
                    CenteredArtworkPane(padding = padding, modifier = Modifier.fillMaxSize()) { availableHeight ->
                        AlbumLandscapeArtworkPane(state, actions,
                            modifier = Modifier.fillMaxWidth().height(availableHeight.coerceAtLeast(160.dp))
                                .padding(horizontal = 8.dp),
                            more = { moreButton() },
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(), contentPadding = padding,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item(key = "album_header") {
                            AlbumHeader(state, actions, artists, onShowArtists = { artistSheet = true })
                        }
                        item(key = "album_actions") {
                            AlbumActions(
                                hasSongs = state.songs.isNotEmpty(), canPlay = state.canPlay,
                                onShuffle = actions.onShuffle, onPlayAll = actions.onPlayAll,
                                onDownload = actions.onDownload,
                            )
                        }
                        if (album.description.isNotBlank()) item(key = "album_description") {
                            AlbumDescription(
                                album.id, album.description, maxLines = 1,
                            ) { description = true }
                        }
                    }
                }
            },
            mainPane = { padding ->
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(state = songs, modifier = Modifier.fillMaxSize(), contentPadding = padding) {
                        item(key = "album_controls") {
                            Column {
                                TabletSectionHeading(if (state.searchQuery.isNotBlank()) "搜索结果 · ${state.displayedSongs.size}" else "歌曲 · ${state.songs.size}")
                                if (state.isSearchActive) OutlinedTextField(
                                    value = state.searchQuery, onValueChange = actions.onSearchQueryChange,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 8.dp)
                                        .focusRequester(focus),
                                    label = { Text("搜索歌名、歌手或专辑") }, singleLine = true,
                                )
                            }
                        }
                        itemsIndexed(state.displayedSongs, key = { _, song -> song.id }) { index, song ->
                            AlbumTrackRow(
                                song, indices[song.id] ?: index, onClick = { actions.onTrackClick(song, index) },
                                onMore = { actions.onTrackMore(song, it) }, canPlay = state.canPlay
                            )
                        }
                        if (state.displayedSongs.isEmpty()) item {
                            Text(
                                if (state.searchQuery.isNotBlank()) "没有找到匹配的歌曲" else "暂无歌曲",
                                Modifier.padding(horizontal = gutter, vertical = 32.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
        )
        if (description) {
            val details = if (landscape) listOf(
                artists.joinToString(" / ") { it.name }, trackSummary, album.description,
            ).filter(String::isNotBlank).joinToString("\n\n") else album.description
            DescriptionSheet(album.name, details) { description = false }
        }
        if (artistSheet) AlbumArtistBottomSheet(
            albumInfo = MediaMetadata.Album(album.id, album.name), artistList = artists, coverUrl = album.picUrl,
            onAlbumClick = {}, onArtistClick = { artistSheet = false; actions.onArtistClick(it) },
            onDismissRequest = { artistSheet = false }, showAlbumDestination = false,
        )
    }
}
