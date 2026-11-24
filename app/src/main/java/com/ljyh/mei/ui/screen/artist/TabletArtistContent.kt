package com.ljyh.mei.ui.screen.artist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.artist.component.TabletArtistSongRow
import com.ljyh.mei.utils.preferences.rememberPreference
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun TabletArtistContent(state: ArtistDetailUiState, actions: ArtistDetailActions) {
    val artwork = state.appleArtwork?.takeIf { state.appleArtworkEnabled }
    var presentation by remember(state.id, artwork) { mutableStateOf(ArtistArtworkPresentation()) }
    var portraitPainter by remember(state.id, artwork) { mutableStateOf<Painter?>(null) }
    var description by rememberSaveable(state.id) { mutableStateOf(false) }
    val debugEnabled by rememberPreference(DebugKey, defaultValue = false)
    val list = rememberLazyListState()
    val bar = rememberMeiArtworkTopBarState(list, state.id)
    val progress by animateFloatAsState(if (bar.artworkPassed) 1f else 0f, label = "tabletArtistBar")
    val haze = rememberHazeState()
    val density = LocalDensity.current.density
    val scrollPixels = remember(list, density) { {
        list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "artist_header" }?.let {
            (-it.offset).coerceAtLeast(0).toFloat()
        } ?: if (list.firstVisibleItemIndex > 0) 380f * density else 0f
    } }
    ArtistArtworkTheme(artwork, presentation.usesApplePhoto, immersive = true, backgroundOverride = presentation.background) {
        val background = MaterialTheme.colorScheme.background
        val bodyWidth = if (presentation.usesApplePhoto) 1280.dp else 960.dp
        BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().background(background)) {
            if (artwork != null) TabletArtistBackdrop(artwork, scrollPixels) { status, photo, painter ->
                presentation = presentation.copy(photo = status, background = photo.image.background,
                    tabletPhotoKind = photo.kind)
                portraitPainter = painter.takeIf { !photo.hasHero }
            }
            val preferredHeroHeight = minOf(maxWidth * 0.58f, maxHeight * 0.68f).coerceIn(320.dp, 600.dp)
            val viewportHeight = maxHeight
            val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
            val songColumns = (minOf(maxWidth, bodyWidth) / (360.dp * fontScale))
                .toInt().coerceIn(1, 3)
            val songRows = state.songs.take(12).withIndex().chunked(songColumns)
            Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, contentWindowInsets = WindowInsets(0), topBar = {
                MeiHazeTopBar(haze, state.artist?.name.orEmpty(), progress,
                    modifier = bar.toolbarModifier, onNavigateBack = actions.onBack,
                    backgroundProgress = progress,
                    backgroundMode = if (bar.artworkPassed) MeiTopBarBackground.Auto else MeiTopBarBackground.Transparent,
                    actions = { ArtistMoreButton(state, actions) { description = true } })
            }) { padding ->
                val bottom = maxOf(padding.calculateBottomPadding(),
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding())
                // Leave a heading and one complete song row above the floating bottom controls.
                val heroHeight = minOf(preferredHeroHeight, (viewportHeight - bottom - 144.dp).coerceAtLeast(320.dp))
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().hazeSource(haze),
                    contentPadding = PaddingValues(bottom = bottom + 24.dp)) {
                    item(key = "artist_header") {
                        if (state.artist == null && !state.detailLoading) {
                            TabletArtistBody(bodyWidth) { MeiStatePanel("歌手信息暂未加载", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetryDetail) }
                        } else TabletArtistHeader(state, artwork, presentation, portraitPainter, heroHeight, padding.calculateTopPadding() + 16.dp, actions, onInfo = { description = true },
                            onWordmarkChanged = { presentation = presentation.copy(wordmark = it) }, artworkModifier = bar.artworkModifier)
                    }
                    if (debugEnabled && state.artist != null) item(key = "artist_artwork_debug") {
                        TabletArtistBody(bodyWidth) { ArtistArtworkDebugInfo(artwork, presentation, state.appleArtworkEnabled,
                            Modifier.padding(meiPageHorizontalPadding())) }
                    }
                    item(key = "songs_heading") { TabletArtistBody(bodyWidth) {
                        SectionTitle("热门单曲", actions.onBrowseSongs)
                    } }
                    when {
                        state.songsLoading -> item { TabletArtistBody(bodyWidth) { SkeletonShimmerHost(true) {
                            Column { repeat(3) { TabletArtistSongRow(emptyList(), songColumns, actions, loading = true) } }
                        } } }
                        state.songsError != null -> item { TabletArtistBody(bodyWidth) {
                            MeiStatePanel("热门歌曲暂未加载", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetrySongs)
                        } }
                        state.songs.isEmpty() -> item { TabletArtistBody(bodyWidth) { EmptyArtistSection("暂无热门单曲") } }
                        else -> items(songRows, key = { row -> "songs_${row.first().value.id}" }) { row ->
                            TabletArtistBody(bodyWidth) { TabletArtistSongRow(row, songColumns, actions) }
                        }
                    }
                    item(key = "albums_heading") { TabletArtistBody(bodyWidth) {
                        SectionTitle("专辑", actions.onBrowseAlbums)
                        Spacer(Modifier.height(12.dp))
                    } }
                    when {
                        state.albumsLoading -> item { TabletArtistBody(bodyWidth) { SkeletonShimmerHost(true) {
                            LazyRow(contentPadding = meiPageHorizontalPadding(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(4) { VinylAlbumCard("", "", null,
                                    Modifier.width(164.dp * VinylAlbumArtworkRatio), isLoading = true, onClick = {}) }
                            }
                        } } }
                        state.albumsError != null -> item { TabletArtistBody(bodyWidth) {
                            MeiStatePanel("专辑暂未加载", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetryAlbums)
                        } }
                        state.albums.isEmpty() -> item { TabletArtistBody(bodyWidth) { EmptyArtistSection("暂无专辑") } }
                        else -> item(key = "albums") { TabletArtistBody(bodyWidth) {
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
private fun TabletArtistBody(maxWidth: Dp, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = maxWidth).fillMaxWidth(), content = content)
    }
}
