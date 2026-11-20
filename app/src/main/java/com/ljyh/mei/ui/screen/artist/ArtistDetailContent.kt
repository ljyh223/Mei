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
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.utils.preferences.rememberPreference
import com.ljyh.mei.ui.component.glass.*
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
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
    var presentation by remember(state.id, state.appleArtwork) { mutableStateOf(ArtistArtworkPresentation()) }
    val appleReady = presentation.usesApplePhoto
    val debugEnabled by rememberPreference(DebugKey, defaultValue = false)
    ArtistArtworkTheme(state.appleArtwork, appleReady) {
        var description by rememberSaveable(state.id) { mutableStateOf(false) }
        val base = MaterialTheme.colorScheme.background
        val tint by animateColorAsState(if (appleReady) base else state.coverColor?.let {
            lerp(base, it, if (base.luminance() < 0.5f) 0.38f else 0.24f)
        } ?: base, label = "artistHeaderColor")
        val list = rememberLazyListState()
        val bar = rememberMeiArtworkTopBarState(list, state.id)
        val progress by animateFloatAsState(if (bar.artworkPassed) 1f else 0f, label = "artistBar")
        val source = rememberHazeState()
        val compactHeader = LocalPageStartInset.current > 0.dp
        Scaffold(containerColor = base, topBar = {
            MeiHazeTopBar(source, state.artist?.name.orEmpty(), progress,
                modifier = bar.toolbarModifier, onNavigateBack = actions.onBack,
                backgroundProgress = progress,
                backgroundMode = if (bar.artworkPassed) MeiTopBarBackground.Auto else MeiTopBarBackground.Transparent,
                actions = { ArtistMoreButton(state, actions) { description = true } })
        }) { padding ->
            LazyColumn(state = list, modifier = Modifier.fillMaxSize().hazeSource(source).background(base),
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() +
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding())) {
                item(key = "artist_header") {
                    when {
                        state.artist != null -> {
                            val legacy: @Composable () -> Unit = {
                                ArtistHeader(state.artist, headerSurface = tint, artworkModifier = bar.artworkModifier,
                                    canPlayHotSongs = state.canPlay && state.songs.isNotEmpty(), compact = compactHeader,
                                    onBrowseSongs = actions.onBrowseSongs, onPlayHotSongs = actions.onPlayHotSongs,
                                    onInfo = { description = true }, subscription = state.subscription,
                                    onToggleSubscription = actions.onToggleSubscription)
                            }
                            val artwork = state.appleArtwork
                            if (artwork == null) legacy() else AppleArtistHeader(state, artwork, actions,
                                onInfo = { description = true }, onPresentationChanged = { presentation = it },
                                artworkModifier = bar.artworkModifier, compact = compactHeader, fallback = legacy)
                        }
                        state.detailLoading -> ArtistHeaderShimmer(tint, bar.artworkModifier, compact = compactHeader)
                        else -> ErrorItem(state.detailError ?: "歌手信息暂不可用")
                    }
                }
                if (debugEnabled && state.artist != null) item(key = "artist_artwork_debug") {
                    ArtistArtworkDebugInfo(state.appleArtwork, presentation,
                        Modifier.padding(meiPageHorizontalPadding()))
                }
                item(key = "artist_catalog_spacing") { Spacer(Modifier.height(32.dp)) }
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
                        LazyRow(contentPadding = meiPageHorizontalPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(4) { AlbumCardShimmer() }
                        }
                    } }
                    state.albumsError != null -> item { ErrorItem(state.albumsError) }
                    state.albums.isEmpty() -> item { EmptyArtistSection("暂无专辑") }
                    else -> item { LazyRow(contentPadding = meiPageHorizontalPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.albums.take(10), key = { it.id }) { AlbumCard(it, onClick = actions.onAlbumClick) }
                    } }
                }
            }
        }
        if (description && state.artist != null) DescriptionSheet(state.artist.name, state.artist.briefDesc.ifBlank { "暂无简介" }) { description = false }
    }
}
