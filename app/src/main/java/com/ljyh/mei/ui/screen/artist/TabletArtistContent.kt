package com.ljyh.mei.ui.screen.artist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.card.VinylAlbumCard
import com.ljyh.mei.ui.component.glass.*
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.sheet.DescriptionSheet
import com.ljyh.mei.ui.component.track.Track
import com.ljyh.mei.ui.component.track.TrackPlaceholder
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.utils.preferences.rememberPreference
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun TabletArtistContent(state: ArtistDetailUiState, actions: ArtistDetailActions) {
    val artwork = state.appleArtwork?.takeIf { state.appleArtworkEnabled }
    var presentation by remember(state.id, artwork) { mutableStateOf(ArtistArtworkPresentation()) }
    var description by rememberSaveable(state.id) { mutableStateOf(false) }
    val debugEnabled by rememberPreference(DebugKey, defaultValue = false)
    val list = rememberLazyListState()
    val bar = rememberMeiArtworkTopBarState(list, state.id)
    val progress by animateFloatAsState(if (bar.artworkPassed) 1f else 0f, label = "tabletArtistBar")
    val haze = rememberHazeState()
    ArtistArtworkTheme(artwork, presentation.usesApplePhoto) {
        val background = MaterialTheme.colorScheme.background
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val photoHeight = minOf(maxWidth * 0.4f, maxHeight * 0.45f).coerceIn(240.dp, 400.dp)
            Scaffold(containerColor = background, contentWindowInsets = WindowInsets(0), topBar = {
                MeiHazeTopBar(haze, state.artist?.name.orEmpty(), progress,
                    modifier = bar.toolbarModifier, onNavigateBack = actions.onBack,
                    backgroundProgress = progress,
                    backgroundMode = if (bar.artworkPassed) MeiTopBarBackground.Auto else MeiTopBarBackground.Transparent,
                    actions = { ArtistMoreButton(state, actions) { description = true } })
            }) { padding ->
                val bottom = maxOf(padding.calculateBottomPadding(),
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding())
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().hazeSource(haze).background(background),
                    contentPadding = PaddingValues(bottom = bottom + 24.dp)) {
                    item(key = "artist_header") {
                        if (state.artist == null && !state.detailLoading) {
                            TabletArtistBody { MeiStatePanel("歌手信息暂未加载", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetryDetail) }
                        } else TabletArtistHeader(state, artwork, photoHeight, actions, onInfo = { description = true },
                            onPresentationChanged = { presentation = it }, artworkModifier = bar.artworkModifier)
                    }
                    if (debugEnabled && state.artist != null) item(key = "artist_artwork_debug") {
                        TabletArtistBody { ArtistArtworkDebugInfo(artwork, presentation, state.appleArtworkEnabled,
                            Modifier.padding(meiPageHorizontalPadding())) }
                    }
                    item(key = "songs_heading") { TabletArtistBody {
                        Spacer(Modifier.height(8.dp))
                        SectionTitle("热门单曲", actions.onBrowseSongs)
                    } }
                    when {
                        state.songsLoading -> item { TabletArtistBody { SkeletonShimmerHost(true) {
                            Column { repeat(6) { TrackPlaceholder(it, isTablet = true) } }
                        } } }
                        state.songsError != null -> item { TabletArtistBody {
                            MeiStatePanel("热门歌曲暂未加载", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetrySongs)
                        } }
                        state.songs.isEmpty() -> item { TabletArtistBody { EmptyArtistSection("暂无热门单曲") } }
                        else -> itemsIndexed(state.songs.take(12), key = { _, song -> "song_${song.id}" }) { index, song ->
                            TabletArtistBody { Track(song, index = index, isTablet = true,
                                onClick = { actions.onTrackClick(song, index) }, onMoreClick = { actions.onTrackMore(song, it) }) }
                        }
                    }
                    item(key = "albums_heading") { TabletArtistBody {
                        SectionTitle("专辑", actions.onBrowseAlbums)
                        Spacer(Modifier.height(12.dp))
                    } }
                    when {
                        state.albumsLoading -> item { TabletArtistBody { SkeletonShimmerHost(true) {
                            LazyRow(contentPadding = meiPageHorizontalPadding(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(4) { VinylAlbumCard("", "", null,
                                    Modifier.width(164.dp * VinylAlbumArtworkRatio), isLoading = true, onClick = {}) }
                            }
                        } } }
                        state.albumsError != null -> item { TabletArtistBody {
                            MeiStatePanel("专辑暂未加载", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetryAlbums)
                        } }
                        state.albums.isEmpty() -> item { TabletArtistBody { EmptyArtistSection("暂无专辑") } }
                        else -> item(key = "albums") { TabletArtistBody {
                            LazyRow(contentPadding = meiPageHorizontalPadding(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(state.albums.take(12), key = { it.id }) { album ->
                                    VinylAlbumCard(album.title, "${album.size} 首", album.cover,
                                        Modifier.width(164.dp * VinylAlbumArtworkRatio), onClick = { actions.onAlbumClick(album.id) })
                                }
                            }
                        } }
                    }
                }
            }
        }
        if (description && state.artist != null) DescriptionSheet(state.artist.name,
            state.artist.briefDesc.ifBlank { "暂无简介" }) { description = false }
    }
}

@Composable
private fun TabletArtistBody(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 960.dp).fillMaxWidth(), content = content)
    }
}
