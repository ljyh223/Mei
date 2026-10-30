package com.ljyh.mei.ui.screen.artist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.ui.component.glass.*
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.sheet.DescriptionSheet
import com.ljyh.mei.ui.component.track.Track
import com.ljyh.mei.ui.component.track.TrackPlaceholder
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun ArtistDetailContent(state: ArtistDetailUiState, actions: ArtistDetailActions) {
    var description by rememberSaveable(state.id) { mutableStateOf(false) }
    val base = MaterialTheme.colorScheme.background
    val tint by animateColorAsState(state.coverColor?.let {
        lerp(base, it, if (base.luminance() < 0.5f) 0.38f else 0.24f)
    } ?: base, label = "artistHeaderColor")
    val list = rememberLazyListState()
    val bar = rememberMeiArtworkTopBarState(list, state.id)
    val progress by animateFloatAsState(if (bar.artworkPassed) 1f else 0f, label = "artistBar")
    val source = rememberHazeState()
    Scaffold(topBar = {
        MeiHazeTopBar(source, state.artist?.name.orEmpty(), progress,
            modifier = bar.toolbarModifier, onNavigateBack = actions.onBack,
            backgroundMode = meiArtworkTopBarBackground(bar.artworkPassed))
    }) { padding ->
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().hazeSource(source),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() +
                LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding())) {
            item(key = "artist_header") {
                when {
                    state.artist != null -> ArtistHeader(
                        state.artist, headerSurface = tint, artworkModifier = bar.artworkModifier,
                        canPlayHotSongs = state.canPlay && state.songs.isNotEmpty(),
                        onBrowseSongs = actions.onBrowseSongs, onPlayHotSongs = actions.onPlayHotSongs,
                    )
                    state.detailLoading -> ArtistHeaderShimmer(tint, bar.artworkModifier)
                    else -> ErrorItem(state.detailError ?: "歌手信息暂不可用")
                }
            }
            state.artist?.let { artist ->
                item(key = "artist_about") {
                    if (artist.briefDesc.isNotBlank() || state.expertIdentities.any { it.expertIdentiyCount > 0 }) {
                        ArtistAbout(artist, tint, state.expertIdentities.filter { it.expertIdentiyCount > 0 }) { description = true }
                    } else Spacer(Modifier.fillMaxWidth().height(48.dp).background(Brush.verticalGradient(listOf(tint, base))))
                }
            }
            item { SectionTitle("热门单曲", actions.onBrowseSongs) }
            when {
                state.songsLoading -> item { SkeletonShimmerHost(true) { Column { repeat(5) { TrackPlaceholder(it) } } } }
                state.songsError != null -> item { ErrorItem(state.songsError) }
                state.songs.isEmpty() -> item { EmptyArtistSection("暂无热门单曲") }
                else -> itemsIndexed(state.songs.take(8), key = { _, song -> song.id }) { index, song ->
                    Track(song, onClick = { actions.onTrackClick(song, index) }, onMoreClick = { actions.onTrackMore(song, it) })
                }
            }
            item { SectionTitle("专辑", actions.onBrowseAlbums) }
            when {
                state.albumsLoading -> item { SkeletonShimmerHost(true) {
                    LazyRow(contentPadding = PaddingValues(horizontal = meiPageGutter()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(4) { AlbumCardShimmer() }
                    }
                } }
                state.albumsError != null -> item { ErrorItem(state.albumsError) }
                state.albums.isEmpty() -> item { EmptyArtistSection("暂无专辑") }
                else -> item { LazyRow(contentPadding = PaddingValues(horizontal = meiPageGutter()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.albums.take(10), key = { it.id }) { AlbumCard(it, onClick = actions.onAlbumClick) }
                } }
            }
        }
    }
    if (description && state.artist != null) DescriptionSheet(state.artist.name, state.artist.briefDesc) { description = false }
}
