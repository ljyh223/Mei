package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.component.card.VinylAlbumCard
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.ui.component.sheet.DescriptionSheet
import com.ljyh.mei.ui.component.track.Track
import com.ljyh.mei.ui.component.track.TrackPlaceholder
import com.ljyh.mei.ui.tablet.*
import com.ljyh.mei.ui.tablet.component.*

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun AdaptiveArtistContent(state: ArtistDetailUiState, actions: ArtistDetailActions) {
    val landscape = LocalPhoneLandscape.current
    AdaptiveTabletContent(
        sideWidthFraction = if (landscape) 0.46f else 0.34f,
        minSideWidthDp = if (landscape) 252f else 336f,
        maxSideWidthDp = if (landscape) 360f else 420f,
        minContentWidthDp = if (landscape) 280f else 480f,
        compact = { ArtistDetailContent(state, actions) },
    ) { metrics ->
        var description by rememberSaveable(state.id) { mutableStateOf(false) }
        val artist = state.artist
        val gutter = meiPageGutter()
        val canPlay = state.canPlay && state.songs.isNotEmpty()
        TabletDetailPage(
            title = artist?.name ?: "歌手", sideWidth = metrics.sideWidthDp.dp, onBack = actions.onBack,
            showToolbar = !landscape,
            sidePane = { padding ->
                if (landscape) {
                    val portraitModifier = Modifier.fillMaxSize().padding(padding).padding(start = gutter)
                    if (artist == null && state.detailError != null) {
                        Box(portraitModifier, contentAlignment = Alignment.Center) {
                            MeiStatePanel("歌手信息暂未加载", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetryDetail)
                        }
                    } else {
                        LandscapeArtistPortrait(
                            url = artist?.cover?.takeIf(String::isNotBlank) ?: artist?.avatar,
                            name = artist?.name.orEmpty(),
                            loading = state.detailLoading,
                            modifier = portraitModifier,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item(key = "artist_artwork") {
                            DetailSideArtwork(
                                artist?.cover?.takeIf(String::isNotBlank) ?: artist?.avatar,
                                aspectRatio = 0.82f, placeholder = Icons.Rounded.Person, loading = state.detailLoading,
                            )
                        }
                        item(key = "artist_identity") {
                            Column(Modifier.padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    artist?.name ?: if (state.detailLoading) "正在加载" else "歌手信息暂不可用",
                                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                                if (artist != null) {
                                    val aliases = artistDisplayAliases(artist)
                                    if (aliases.isNotEmpty()) Text(aliases.joinToString(" / "), maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                    val identity = (artist.identifyTag ?: artist.identities).filter(String::isNotBlank).take(3)
                                    if (identity.isNotEmpty()) Text(identity.joinToString(" · "),
                                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${artist.musicSize} 首歌曲 · ${artist.albumSize} 张专辑",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        item(key = "artist_play") {
                            Button(
                                onClick = actions.onPlayHotSongs,
                                enabled = canPlay,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = gutter).heightIn(min = 48.dp),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("播放热门歌曲")
                            }
                        }
                        if (state.detailError != null) item { MeiStatePanel("歌手信息暂未加载", Icons.Rounded.CloudOff,
                            actionLabel = "重试", onAction = actions.onRetryDetail) }
                        if (artist != null && (artist.briefDesc.isNotBlank() || state.expertIdentities.any { it.expertIdentiyCount > 0 })) {
                            item(key = "artist_bio") {
                                ArtistAbout(artist, MaterialTheme.colorScheme.surface,
                                    state.expertIdentities.filter { it.expertIdentiyCount > 0 },
                                    maxDescriptionLines = 3, expandBelowDescription = true) { description = true }
                            }
                        }
                    }
                }
            },
            mainPane = { padding ->
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val columns = tabletArtworkColumns(
                        maxWidth.value, LocalDensity.current.fontScale, gutter.value, VinylAlbumArtworkRatio,
                        minArtworkWidthDp = if (landscape) 104f else 156f,
                    )
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item(key = "songs_heading") {
                            TabletSectionHeading("热门单曲", actions.onBrowseSongs) {
                                if (landscape) IconButton(onClick = actions.onPlayHotSongs, enabled = canPlay) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "播放热门歌曲")
                                }
                            }
                        }
                        when {
                            state.songsLoading -> item {
                                SkeletonShimmerHost(true) {
                                    Column { repeat(6) { TrackPlaceholder(it, isTablet = !landscape) } }
                                }
                            }
                            state.songsError != null -> item { MeiStatePanel("热门歌曲暂未加载", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetrySongs) }
                            state.songs.isEmpty() -> item { Text("暂无热门单曲", Modifier.padding(horizontal = gutter, vertical = 24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            else -> itemsIndexed(state.songs.take(12), key = { _, song -> "song_${song.id}" }) { index, song ->
                                Track(song, index = index, isTablet = !landscape,
                                    onClick = { actions.onTrackClick(song, index) }, onMoreClick = { actions.onTrackMore(song, it) })
                            }
                        }
                        item(key = "albums_heading") { TabletSectionHeading("专辑", actions.onBrowseAlbums) }
                        when {
                            state.albumsLoading -> item { TabletArtworkRow((0 until columns).toList(), columns) { _, modifier ->
                                VinylAlbumCard("", "", null, modifier, isLoading = true, onClick = {})
                            } }
                            state.albumsError != null -> item { MeiStatePanel("专辑暂未加载", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetryAlbums) }
                            state.albums.isEmpty() -> item { Text("暂无专辑", Modifier.padding(horizontal = gutter, vertical = 24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            else -> items(state.albums.take(12).chunked(columns), key = { "albums_${it.first().id}" }) { row ->
                                TabletArtworkRow(row, columns) { album, modifier ->
                                    VinylAlbumCard(album.title, "${album.size} 首", album.cover, modifier,
                                        onClick = { actions.onAlbumClick(album.id) })
                                }
                            }
                        }
                    }
                }
            },
        )
        if (description && artist != null) DescriptionSheet(artist.name, artist.briefDesc) { description = false }
    }
}

@Composable
private fun LandscapeArtistPortrait(url: String?, name: String, loading: Boolean, modifier: Modifier) {
    Box(modifier) {
        if (url.isNullOrBlank()) {
            ArtistPortraitFrame(name = name, loading = loading)
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = "歌手照片",
                contentScale = ContentScale.Crop,
            ) imageContent@{
                val state by painter.state.collectAsState()
                val loaded = state is AsyncImagePainter.State.Success
                ArtistPortraitFrame(
                    name = name,
                    loading = !loaded && state !is AsyncImagePainter.State.Error,
                ) {
                    if (loaded) {
                        this@imageContent.SubcomposeAsyncImageContent(modifier = Modifier.matchParentSize())
                    } else {
                        Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistPortraitFrame(
    name: String,
    loading: Boolean,
    artwork: @Composable BoxScope.() -> Unit = {
        Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(48.dp))
    },
) {
    val shape = RoundedCornerShape(16.dp)
    SkeletonShimmerHost(
        enabled = loading,
        modifier = Modifier.fillMaxSize().clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (loading) {
            Spacer(Modifier.matchParentSize().skeleton(true, shape))
        } else {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center, content = artwork)
            if (name.isNotBlank()) {
                Text(
                    text = name,
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .background(Brush.verticalGradient(
                            0f to Color.Transparent,
                            .38f to Color.Black.copy(alpha = .65f),
                            1f to Color.Black.copy(alpha = .84f),
                        ))
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
