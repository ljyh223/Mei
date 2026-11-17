package com.ljyh.mei.ui.screen.album

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.artwork.CenteredArtworkPane
import com.ljyh.mei.ui.component.artwork.LandscapeArtworkHeader
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtwork
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.menu.MeiDetailMoreButton
import com.ljyh.mei.ui.component.menu.MeiMenuItem
import com.ljyh.mei.ui.component.sheet.AlbumArtistBottomSheet
import com.ljyh.mei.ui.component.sheet.DescriptionSheet
import com.ljyh.mei.ui.tablet.*
import com.ljyh.mei.ui.tablet.component.*
import com.ljyh.mei.utils.share.MediaShareLink
import java.time.Instant
import java.time.ZoneId

@Composable
internal fun AdaptiveAlbumLayout(
    compact: @Composable () -> Unit,
    expanded: @Composable (TabletContentMetrics) -> Unit,
) {
    val landscape = LocalPhoneLandscape.current
    AdaptiveTabletContent(
        sideWidthFraction = if (landscape) 0.40f else 0.34f,
        minSideWidthDp = if (landscape) 252f else 336f,
        maxSideWidthDp = if (landscape) 300f else 420f,
        minContentWidthDp = if (landscape) 280f else 480f,
        compact = compact,
        expanded = expanded,
    )
}

@Composable
internal fun AdaptiveAlbumContent(state: AlbumDetailUiState, actions: AlbumDetailActions) {
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
            album.publishTime.takeIf { it > 0 }?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).year.toString()
            }
        }
        val trackSummary = listOfNotNull(year, "${state.songs.size} 首歌曲").joinToString(" · ")
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
            MeiDetailMoreButton(
                shareContent = MediaShareLink.album(album), searchActive = state.isSearchActive,
                searchLabel = "搜索专辑歌曲", onSearchActiveChange = ::changeSearch,
                additionalItems = if (landscape) { dismiss ->
                    val label = when (state.isSubscribed) {
                        true -> "取消收藏专辑"
                        false -> "收藏专辑"
                        null -> if (state.isSubscriptionLoading) "正在获取收藏状态" else "重试获取收藏状态"
                    }
                    MeiMenuItem(
                        label,
                        if (state.isSubscribed == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        enabled = state.canSubscribe,
                    ) {
                        dismiss()
                        actions.onSubscribe()
                    }
                    MeiMenuItem("下载专辑", Icons.Rounded.Download, enabled = state.songs.isNotEmpty()) {
                        dismiss()
                        actions.onDownload()
                    }
                    if (canOpenArtist) MeiMenuItem("查看歌手", Icons.Rounded.Person) {
                        dismiss()
                        openArtist()
                    }
                    MeiMenuItem("专辑信息", Icons.Rounded.Info) {
                        dismiss()
                        description = true
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
                        LandscapeArtworkHeader(
                            maxArtworkHeight = (availableHeight - 8.dp).coerceAtLeast(160.dp),
                            canPlay = state.canPlay && state.songs.isNotEmpty(),
                            onPlayAll = actions.onPlayAll,
                            artworkAspectRatio = VinylAlbumArtworkRatio,
                            artwork = { modifier ->
                                VinylAlbumArtwork(album.picUrl, modifier.semantics { contentDescription = "专辑封面" })
                            },
                            more = moreButton,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(), contentPadding = padding,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item(key = "album_artwork") { DetailSideArtwork(album.picUrl) }
                        item(key = "album_identity") {
                            Column(
                                Modifier.padding(horizontal = gutter),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    album.name, style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold, maxLines = 4, overflow = TextOverflow.Ellipsis,
                                )
                                firstArtist?.let { artist ->
                                    TextButton(
                                        onClick = ::openArtist, enabled = canOpenArtist, contentPadding = PaddingValues(0.dp),
                                    ) { Text(if (artists.size > 1) "${artist.name} 等" else artist.name) }
                                }
                                Text(
                                    trackSummary, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        item(key = "album_actions") {
                            AlbumActions(
                                tint = MaterialTheme.colorScheme.surface,
                                hasSongs = state.songs.isNotEmpty(),
                                canPlay = state.canPlay,
                                isSubscribed = state.isSubscribed,
                                isSubscriptionLoading = state.isSubscriptionLoading,
                                canSubscribe = state.canSubscribe,
                                onPlayAll = actions.onPlayAll,
                                onSubscribe = actions.onSubscribe,
                                onDownload = actions.onDownload,
                            )
                        }
                        if (album.description.isNotBlank()) item(key = "album_description") {
                            AlbumDescription(
                                album.id, album.description, maxLines = 3, expandBelowDescription = true,
                            ) { description = true }
                        }
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
