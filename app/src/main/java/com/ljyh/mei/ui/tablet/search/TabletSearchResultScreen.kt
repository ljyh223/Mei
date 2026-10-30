package com.ljyh.mei.ui.tablet.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.domain.*
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtwork
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.ui.component.track.TrackPlaceholder
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.search.*
import kotlinx.coroutines.flow.distinctUntilChanged

/** Only presentation differs. The route still owns the shared search ViewModel and actions. */
@Composable
internal fun TabletSearchResultScreen(query: String, type: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        SearchResultScreen(query, type, modifier = Modifier.widthIn(max = 1280.dp).fillMaxSize(),
            searchFieldMaxWidth = 640.dp, resultsContent = { TabletSearchResults(it) })
    }
}

@Composable
private fun TabletSearchResults(state: SearchResultContentState) = with(state) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val metrics = tabletSearchLayout(maxWidth.value, LocalDensity.current.fontScale, meiPageGutter().value)
        val columns = when (type) {
            SearchType.Artist -> metrics.artistColumns
            SearchType.Album -> metrics.albumColumns
            SearchType.Playlist -> metrics.artworkColumns
            else -> 1
        }
        val grid = rememberLazyGridState()
        val currentPage by rememberUpdatedState(page)
        val loadMore by rememberUpdatedState(onLoadMore)
        val initialLoading = page.loading && page.data == null
        val showLoading = rememberSearchLoading(page, type, keyword)
        LaunchedEffect(grid, columns) {
            snapshotFlow {
                val data = currentPage
                val last = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                val count = grid.layoutInfo.totalItemsCount
                !data.loading && !data.appending && !data.appendError && data.data?.hasMore == true &&
                    count > 0 && last >= count - columns * 2 - 1
            }.distinctUntilChanged().collect { if (it) loadMore() }
        }
        val words = page.data?.highlightWords.orEmpty().ifEmpty { listOf(keyword) }
        val bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 24.dp
        val gutter = meiPageGutter()
        Column(Modifier.fillMaxSize()) {
            if (type != SearchType.All && ((initialLoading && showLoading) || page.data?.isEmpty == false)) {
                SearchCategoryHeading(type, page.data?.totalCount)
                if (type == SearchType.Song && metrics.songColumns) TabletSearchSongHeading()
            }
            SkeletonShimmerHost(initialLoading && showLoading && type != SearchType.All, Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns), state = grid, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (columns == 1 && type in listOf(SearchType.All, SearchType.Song)) 0.dp else gutter,
                        end = if (columns == 1 && type in listOf(SearchType.All, SearchType.Song)) 0.dp else gutter,
                        top = 8.dp, bottom = bottom,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(if (type in listOf(SearchType.Album, SearchType.Playlist, SearchType.Artist)) 24.dp else 0.dp),
                ) {
                    when {
                        initialLoading -> if (showLoading) {
                            if (type == SearchType.All) item("overview_loading", span = { GridItemSpan(maxLineSpan) }) { SearchOverviewLoading() }
                            else items(if (type == SearchType.Song) 8 else columns * 3, key = { "loading-$it" }) { index ->
                                if (type == SearchType.Song) TrackPlaceholder(index, isTablet = metrics.songColumns)
                                else TabletSearchTile("", "", null, words = emptyList(), round = type == SearchType.Artist,
                                    vinyl = type == SearchType.Album, isLoading = true, onClick = {})
                            }
                        }
                        page.error && page.data == null -> item("error", span = { GridItemSpan(maxLineSpan) }) {
                            SearchStatusPanel("搜索暂时不可用", "检查网络连接后再试一次", "重新加载", onRetry)
                        }
                        page.data?.isEmpty == true -> item("empty", span = { GridItemSpan(maxLineSpan) }) {
                            SearchStatusPanel("未找到“$keyword”相关${if (type == SearchType.All) "结果" else type.displayName}",
                                "试试其他名称、译名或更短的关键词", if (type == SearchType.All) null else "查看综合结果",
                                { actions.onType(SearchType.All) })
                        }
                        page.data != null -> {
                            val data = page.data
                            when (type) {
                                SearchType.All -> item("overview", span = { GridItemSpan(maxLineSpan) }) {
                                    TabletSearchOverview(data, words, actions, metrics)
                                }
                                SearchType.Song -> itemsIndexed(data.songs, key = { _, song -> "song-${song.id}" }) { index, song ->
                                    SearchSongRow(song, words, actions, { actions.onSong(data.songs, index, "songs") },
                                        showColumns = metrics.songColumns, index = index)
                                }
                                SearchType.Artist -> items(data.artists, key = { "artist-${it.id}" }) { artist ->
                                    TabletSearchTile(artist.name, artist.aliases.joinToString(" / ").ifBlank { "歌手" }, artist.imageUrl,
                                        words, round = true, onClick = { actions.onArtist(artist) })
                                }
                                SearchType.Album -> items(data.albums, key = { "album-${it.id}" }) { album ->
                                    TabletSearchTile(album.title, albumSubtitle(album), album.coverUrl, words, vinyl = true,
                                        onClick = { actions.onAlbum(album) })
                                }
                                SearchType.Playlist -> items(data.playlists, key = { "playlist-${it.id}" }) { playlist ->
                                    TabletSearchTile(playlist.title, "${playlist.trackCount} 首 · ${playlist.creatorName}", playlist.coverUrl,
                                        words, onClick = { actions.onPlaylist(playlist) })
                                }
                                SearchType.History -> Unit
                            }
                            if (page.appending || page.appendError || data.hasMore) item("pagination", span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    if (page.appending) CircularProgressIndicator(Modifier.size(24.dp))
                                    else TextButton(onClick = onLoadMore) { Text(if (page.appendError) "加载失败，点击重试" else "加载更多") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletSearchOverview(data: SearchResults, words: List<String>, actions: SearchActions, metrics: TabletSearchLayout) {
    val best = data.bestMatches.take(2)
    val albums = data.albums.filter { album -> best.none { it is SearchMatch.Album && it.album.id == album.id } }.take(6)
    val playlists = data.playlists.filter { playlist -> best.none { it is SearchMatch.Playlist && it.playlist.id == playlist.id } }.take(3)
    val artists = data.artists.filter { artist -> best.none { it is SearchMatch.Artist && it.artist.id == artist.id } }.take(3)
    val twoPanes = metrics.twoPanes && (albums.isNotEmpty() || playlists.isNotEmpty() || artists.isNotEmpty()) &&
        (best.isNotEmpty() || data.songs.isNotEmpty() || data.recommendations.isNotEmpty())
    val primary: @Composable () -> Unit = {
        if (best.isNotEmpty()) {
            SearchSectionHeader("最佳匹配")
            best.forEach { match -> when (match) {
                is SearchMatch.Artist -> SearchArtistRow(match.artist, words, actions.onArtist, bestMatch = true)
                is SearchMatch.Album -> SearchAlbumRow(match.album, words, actions.onAlbum, bestMatch = true)
                is SearchMatch.Playlist -> SearchPlaylistRow(match.playlist, words, actions.onPlaylist)
                is SearchMatch.Song -> SearchSongRow(match.song, words, actions, { actions.onSong(listOf(match.song), 0, "best") })
            } }
        }
        if (data.songs.isNotEmpty()) {
            SearchSectionHeader("单曲", { actions.onType(SearchType.Song) })
            TabletSearchSongPreview(data.songs.take(5), "songs", words, actions)
        }
    }
    val secondary: @Composable () -> Unit = {
        if (albums.isNotEmpty()) {
            SearchSectionHeader("专辑", { actions.onType(SearchType.Album) })
            LazyRow(contentPadding = PaddingValues(horizontal = meiPageGutter()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(albums, key = { it.id }) { album ->
                    TabletSearchTile(album.title, albumSubtitle(album), album.coverUrl, words,
                        modifier = Modifier.width(144.dp * VinylAlbumArtworkRatio), vinyl = true,
                        onClick = { actions.onAlbum(album) })
                }
            }
        }
        if (playlists.isNotEmpty()) {
            SearchSectionHeader("歌单", { actions.onType(SearchType.Playlist) })
            playlists.forEach { SearchPlaylistRow(it, words, actions.onPlaylist) }
        }
        if (artists.isNotEmpty()) {
            SearchSectionHeader("歌手", { actions.onType(SearchType.Artist) })
            artists.forEach { SearchArtistRow(it, words, actions.onArtist) }
        }
    }
    if (twoPanes) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.width(metrics.primaryWidthDp.dp)) {
            primary()
            TabletSearchRecommendations(data.recommendations, words, actions)
        }
        Column(Modifier.weight(1f)) { secondary() }
    } else Column(Modifier.fillMaxWidth()) {
        primary()
        secondary()
        TabletSearchRecommendations(data.recommendations, words, actions)
    }
}

@Composable
private fun TabletSearchRecommendations(songs: List<MediaMetadata>, words: List<String>, actions: SearchActions) {
    if (songs.isNotEmpty()) {
        SearchSectionHeader("你可能也喜欢")
        TabletSearchSongPreview(songs.take(3), "recommendations", words, actions)
    }
}

@Composable
private fun TabletSearchSongPreview(songs: List<MediaMetadata>, section: String, words: List<String>, actions: SearchActions) {
    songs.forEachIndexed { index, song -> SearchSongRow(song, words, actions, { actions.onSong(songs, index, section) }) }
}

/** Shares every measured box between loaded and skeleton grid cells. */
@Composable
private fun TabletSearchTile(
    title: String, subtitle: String, image: String?, words: List<String>, modifier: Modifier = Modifier,
    round: Boolean = false, vinyl: Boolean = false, isLoading: Boolean = false, onClick: () -> Unit,
) {
    Column(modifier.fillMaxWidth().clickable(enabled = !isLoading, onClick = onClick)
        .then(if (isLoading) Modifier.clearAndSetSemantics { } else Modifier).padding(bottom = 8.dp),
        horizontalAlignment = if (round) Alignment.CenterHorizontally else Alignment.Start) {
        if (vinyl) {
            VinylAlbumArtwork(image, isLoading = isLoading)
        } else {
            val shape = if (round) CircleShape else MaterialTheme.shapes.medium
            val artwork = (if (round) Modifier.size(96.dp) else Modifier.fillMaxWidth().aspectRatio(1f))
                .clip(shape).background(MaterialTheme.colorScheme.surfaceContainer)
            if (isLoading) Spacer(artwork.skeleton(true, shape))
            else AsyncImage(image, contentDescription = null, modifier = artwork, contentScale = ContentScale.Crop)
        }
        Text(highlight(if (isLoading) "正在加载的标题" else title, words),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).skeleton(isLoading),
            style = MaterialTheme.typography.titleSmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = if (round) TextAlign.Center else TextAlign.Start)
        Text(highlight(if (isLoading) "名称与相关信息" else subtitle, words),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).skeleton(isLoading),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = if (round) TextAlign.Center else TextAlign.Start)
    }
}

@Composable
private fun TabletSearchSongHeading() {
    Row(Modifier.fillMaxWidth().padding(horizontal = meiPageGutter(), vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(36.dp))
        Row(Modifier.weight(4f)) {
            Spacer(Modifier.width(56.dp))
            Text("歌曲", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("专辑", Modifier.weight(3f).padding(horizontal = 8.dp), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("时长", Modifier.width(60.dp), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(56.dp))
    }
}
