package com.ljyh.mei.ui.tablet.artist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.component.card.VinylAlbumCard
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.glass.*
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.sheet.DescriptionSheet
import com.ljyh.mei.ui.component.track.Track
import com.ljyh.mei.ui.component.track.TrackPlaceholder
import com.ljyh.mei.ui.screen.artist.*
import com.ljyh.mei.ui.tablet.*
import com.ljyh.mei.ui.tablet.component.*

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun TabletArtistContent(state: ArtistDetailUiState, actions: ArtistDetailActions) {
    AdaptiveTabletContent(compact = { ArtistDetailContent(state, actions) }) { metrics ->
        val side = rememberLazyListState()
        val bar = rememberMeiArtworkTopBarState(side, state.id)
        var description by rememberSaveable(state.id) { mutableStateOf(false) }
        val artist = state.artist
        val gutter = meiPageGutter()
        TabletDetailPage(
            title = artist?.name ?: "歌手", sideWidth = metrics.sideWidthDp.dp, onBack = actions.onBack,
            backgroundMode = meiArtworkTopBarBackground(bar.artworkPassed), toolbarModifier = bar.toolbarModifier,
            sidePane = { padding ->
                LazyColumn(state = side, modifier = Modifier.fillMaxSize(), contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item(key = "artist_artwork") {
                        SkeletonShimmerHost(state.detailLoading, Modifier.padding(horizontal = gutter)) {
                            TabletArtwork(artist?.cover?.takeIf(String::isNotBlank) ?: artist?.avatar,
                                Modifier.then(bar.artworkModifier), aspectRatio = 0.82f, placeholder = Icons.Rounded.Person)
                        }
                    }
                    item(key = "artist_identity") {
                        Column(Modifier.padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(artist?.name ?: if (state.detailLoading) "正在加载" else "歌手信息暂不可用",
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
                        Button(actions.onPlayHotSongs,
                            enabled = state.canPlay && state.songs.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = gutter).heightIn(min = 48.dp)) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp)); Text("播放热门歌曲")
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
            },
            mainPane = { padding ->
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val columns = tabletArtworkColumns(maxWidth.value, LocalDensity.current.fontScale, gutter.value, VinylAlbumArtworkRatio)
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item(key = "songs_heading") { TabletSectionHeading("热门单曲", actions.onBrowseSongs) }
                        when {
                            state.songsLoading -> item { SkeletonShimmerHost(true) { Column { repeat(6) { TrackPlaceholder(it, true) } } } }
                            state.songsError != null -> item { MeiStatePanel("热门歌曲暂未加载", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetrySongs) }
                            state.songs.isEmpty() -> item { Text("暂无热门单曲", Modifier.padding(horizontal = gutter, vertical = 24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            else -> itemsIndexed(state.songs.take(12), key = { _, song -> "song_${song.id}" }) { index, song ->
                                Track(song, index = index, isTablet = true,
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
