package com.ljyh.mei.ui.screen.search

import androidx.activity.compose.BackHandler
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.domain.*
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.ui.component.glass.MeiGlassBackButton
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.component.glass.meiGlassContentColor
import com.ljyh.mei.ui.component.glass.meiGlassAccentColor
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtwork
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.ui.component.track.*
import com.ljyh.mei.ui.local.*
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.backToMain
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchResultScreen(
    query: String,
    type: Int,
    viewModel: SearchViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    searchFieldMaxWidth: Dp = Dp.Infinity,
    resultsContent: @Composable (SearchResultContentState) -> Unit = { state -> SearchResultPage(state) },
) {
    val page by viewModel.pageState.collectAsState()
    val selected by viewModel.currentTab.collectAsState()
    val keyword by viewModel.currentQuery.collectAsState()
    LaunchedEffect(query, type) { viewModel.onSearchInit(query, type) }

    val navController = LocalNavController.current
    val player = LocalPlayerConnection.current
    val metadata by player?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
    val playing by player?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val inputFocus = remember { FocusRequester() }
    var input by rememberSaveable(query, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(query)) }
    var editing by rememberSaveable(query) { mutableStateOf(false) }
    var menuTrack by remember { mutableStateOf<TrackMenuTarget?>(null) }
    val source = remember(keyword) { QueueSource(QueueSource.Type.SEARCH, keyword, keyword) }
    val leaveEditing = {
        editing = false
        input = TextFieldValue(keyword.ifBlank { query })
        focus.clearFocus()
        keyboard?.hide()
    }
    val submit: (String, Int) -> Unit = { text, searchType ->
        val next = text.trim()
        if (next.isNotBlank()) {
            editing = false
            focus.clearFocus()
            keyboard?.hide()
            input = TextFieldValue(next)
            scope.launch { context.recordSearchHistory(next) }
            if (next == keyword) {
                viewModel.onTabChange(SearchType.entries.firstOrNull { it.type == searchType } ?: SearchType.All)
            } else {
                Screen.SearchResult.navigate(navController) {
                    addPath(Uri.encode(next))
                    addPath(searchType.toString())
                }
            }
        }
    }
    BackHandler(enabled = editing) { leaveEditing() }
    val playSongs: (List<MediaMetadata>, Int, String) -> Unit = { songs, index, section ->
        player?.playQueue(ListQueue(
            id = "SearchQueue-$keyword-$section",
            title = "搜索：$keyword",
            items = songs.map { it.id.toString() to it.toMediaItem() },
            startIndex = index, position = 0, source = source,
        ))
    }
    val actions = SearchActions(
        onSong = playSongs,
        onMore = { track, anchor -> menuTrack = TrackMenuTarget(track, anchor, source) },
        onArtist = { Screen.Artist.navigate(navController) { addPath(it.id.toString()) } },
        onAlbum = { Screen.Album.navigate(navController) { addPath(it.id.toString()) } },
        onPlaylist = { Screen.PlayList.navigate(navController) { addPath(it.id.toString()) } },
        onType = viewModel::onTabChange,
        playingId = metadata?.id?.takeIf { playing },
    )
    val stateHolder = rememberSaveableStateHolder()
    SearchResultChrome(
        modifier = modifier.then(if (editing) Modifier.imePadding() else Modifier),
        toolbar = {
            Row(
                Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = meiPageGutter(), vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MeiGlassBackButton(
                    onClick = { if (editing) leaveEditing() else navController.navigateUp() },
                    onLongClick = { if (editing) leaveEditing() else navController.backToMain() },
                )
                MeiGlassSurface(
                    modifier = Modifier.weight(1f, fill = false).widthIn(max = searchFieldMaxWidth).fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    val ink = meiGlassContentColor()
                    val secondaryInk = meiGlassContentColor(secondary = true)
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth()
                            .focusRequester(inputFocus).onFocusChanged { if (it.isFocused) editing = true }
                            .semantics { contentDescription = "搜索歌曲、歌手、专辑或歌单" },
                        singleLine = true,
                        placeholder = { Text("搜索歌曲、歌手、专辑", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = ink,
                            unfocusedTextColor = ink,
                            focusedPlaceholderColor = secondaryInk,
                            unfocusedPlaceholderColor = secondaryInk,
                            focusedTrailingIconColor = secondaryInk,
                            unfocusedTrailingIconColor = secondaryInk,
                            cursorColor = meiGlassAccentColor(),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submit(input.text, SearchType.All.type) }),
                        trailingIcon = {
                            if (input.text.isNotEmpty()) IconButton(onClick = {
                                input = TextFieldValue()
                                editing = true
                                inputFocus.requestFocus()
                                keyboard?.show()
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = "清空搜索")
                            }
                        },
                    )
                }
            }
        },
        tabs = if (editing) null else ({ SearchTypeFilterRow(selected, viewModel::onTabChange) }),
    ) { contentTopPadding ->
        if (editing) {
            SearchScreen(
                modifier = Modifier.fillMaxSize()
                    .padding(bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()),
                query = input.text, onQueryChange = { input = it }, onSearch = submit,
                onDismiss = { editing = false; focus.clearFocus(); keyboard?.hide() }, viewModel = viewModel,
                contentTopPadding = contentTopPadding,
            )
        } else {
            key(keyword) {
                stateHolder.SaveableStateProvider("$keyword-${selected.name}") {
                    resultsContent(SearchResultContentState(page, selected, keyword, actions, viewModel::retry,
                        viewModel::loadMore, contentTopPadding))
                }
            }
        }
    }
    BasicTrackActionMenu(menuTrack) { menuTrack = null }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchTypeFilterRow(selected: SearchType, onSelect: (SearchType) -> Unit) {
    val types = remember { SearchType.entries.filter { it != SearchType.History } }
    val ink = meiGlassContentColor()
    val secondaryInk = meiGlassContentColor(secondary = true)
    val accent = meiGlassAccentColor()
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
    val minTabWidth = maxOf(90.dp, with(density) {
        types.maxOf { measurer.measure(it.displayName, labelStyle, maxLines = 1).size.width }.toDp()
    } + 32.dp)
    MeiGlassSurface(
        modifier = Modifier.padding(horizontal = meiPageGutter()).padding(top = 4.dp, bottom = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val selectedIndex = types.indexOf(selected).coerceAtLeast(0)
            val tabs: @Composable () -> Unit = {
                types.forEach { type ->
                    Tab(
                        selected = selected == type,
                        onClick = { onSelect(type) },
                        selectedContentColor = ink,
                        unselectedContentColor = secondaryInk,
                        text = {
                            Text(type.displayName, style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selected == type) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1, softWrap = false)
                        },
                    )
                }
            }
            val indicator: @Composable TabIndicatorScope.() -> Unit = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = true),
                    width = Dp.Unspecified,
                    color = accent,
                )
            }
            if (maxWidth >= minTabWidth * types.size) {
                PrimaryTabRow(selectedTabIndex = selectedIndex, containerColor = Color.Transparent,
                    indicator = indicator, divider = {}, tabs = tabs)
            } else {
                PrimaryScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 0.dp,
                    minTabWidth = minTabWidth, containerColor = Color.Transparent,
                    indicator = indicator, divider = {}, tabs = tabs)
            }
        }
    }
}

internal data class SearchActions(
    val onSong: (List<MediaMetadata>, Int, String) -> Unit,
    val onMore: (MediaMetadata, IntRect) -> Unit,
    val onArtist: (SearchArtist) -> Unit,
    val onAlbum: (SearchAlbum) -> Unit,
    val onPlaylist: (SearchPlaylist) -> Unit,
    val onType: (SearchType) -> Unit,
    val playingId: Long?,
)

internal data class SearchResultContentState(
    val page: SearchPageState,
    val type: SearchType,
    val keyword: String,
    val actions: SearchActions,
    val onRetry: () -> Unit,
    val onLoadMore: () -> Unit,
    val topPadding: Dp = 0.dp,
)

@Composable
private fun SearchResultPage(state: SearchResultContentState) = with(state) {
    SearchResultPage(page, type, keyword, actions, onRetry, onLoadMore, Modifier.fillMaxSize(), topPadding)
}

@Composable
internal fun rememberSearchLoading(page: SearchPageState, type: SearchType, keyword: String): Boolean {
    val initialLoading = page.loading && page.data == null
    var visible by remember(keyword, type, initialLoading) { mutableStateOf(false) }
    LaunchedEffect(keyword, type, initialLoading) {
        if (initialLoading) {
            delay(250)
            visible = true
        }
    }
    return visible
}

@Composable
private fun SearchResultPage(
    page: SearchPageState, type: SearchType, keyword: String, actions: SearchActions,
    onRetry: () -> Unit, onLoadMore: () -> Unit, modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
) {
    val listState = rememberLazyListState()
    val currentPage by rememberUpdatedState(page)
    val currentLoadMore by rememberUpdatedState(onLoadMore)
    val initialLoading = page.loading && page.data == null
    val showInitialLoading = rememberSearchLoading(page, type, keyword)
    LaunchedEffect(listState) {
        snapshotFlow {
            val state = currentPage
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            !state.loading && !state.appending && !state.appendError && state.data?.hasMore == true &&
                total > 0 && last >= total - 4
        }.distinctUntilChanged().collect { if (it) currentLoadMore() }
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(), state = listState,
        contentPadding = PaddingValues(top = topPadding + 8.dp,
            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 16.dp),
    ) {
        when {
            initialLoading -> {
                if (showInitialLoading) {
                    if (type == SearchType.All) {
                        item("overview_loading") { SearchOverviewLoading() }
                    } else {
                        item("loading_title") { SearchCategoryHeading(type) }
                        items(7, key = { "placeholder-$it" }) { index ->
                            if (type == SearchType.Song) TrackPlaceholder(index = index)
                            else SearchEntityRow(
                                title = "${type.displayName}名称占位文字",
                                subtitle = "相关信息占位文字",
                                image = null,
                                words = emptyList(),
                                round = type == SearchType.Artist,
                                albumArtwork = type == SearchType.Album,
                                titleLines = if (type == SearchType.Playlist) 2 else 1,
                                isLoading = true,
                                onClick = {},
                            )
                        }
                    }
                }
            }
            page.error && page.data == null -> item("error") {
                SearchStatusPanel("搜索暂时不可用", "检查网络连接后再试一次", "重新加载", onRetry)
            }
            page.data?.isEmpty == true -> item("empty") {
                SearchStatusPanel("未找到“$keyword”相关${if (type == SearchType.All) "结果" else type.displayName}",
                    "试试其他名称、译名或更短的关键词",
                    if (type == SearchType.All) null else "查看综合结果",
                    { actions.onType(SearchType.All) })
            }
            page.data != null -> {
                val data = page.data
                val words = data.highlightWords.ifEmpty { listOf(keyword) }
                if (type == SearchType.All) overview(data, words, actions) else {
                    item("result_count") {
                        SearchCategoryHeading(type, data.totalCount)
                    }
                    when (type) {
                        SearchType.Song -> songs(data.songs, "songs", words, actions)
                        SearchType.Artist -> items(data.artists, key = { "artist-${it.id}" }) { SearchArtistRow(it, words, actions.onArtist) }
                        SearchType.Album -> items(data.albums, key = { "album-${it.id}" }) { SearchAlbumRow(it, words, actions.onAlbum) }
                        SearchType.Playlist -> items(data.playlists, key = { "playlist-${it.id}" }) { SearchPlaylistRow(it, words, actions.onPlaylist) }
                        else -> Unit
                    }
                    if (page.appending || page.appendError || data.hasMore) item("pagination") {
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

@Composable
internal fun SearchOverviewLoading() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = meiPageGutter(), vertical = 48.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(Modifier.size(24.dp).clearAndSetSemantics { }, strokeWidth = 2.dp)
        Text("正在搜索…", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SearchCategoryHeading(type: SearchType, totalCount: Int? = null, horizontalPadding: Dp = meiPageGutter()) {
    Text(totalCount?.let { "${type.displayName} · $it" } ?: type.displayName,
        modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 12.dp).semantics { heading() },
        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun LazyListScope.overview(data: SearchResults, words: List<String>, actions: SearchActions) {
    val best = data.bestMatches.take(2)
    if (best.isNotEmpty()) {
        item("best_header") { SearchSectionHeader("最佳匹配") }
        items(best, key = { "best-${it.key}" }) { match ->
            when (match) {
                is SearchMatch.Artist -> SearchArtistRow(match.artist, words, actions.onArtist, bestMatch = true)
                is SearchMatch.Album -> SearchAlbumRow(match.album, words, actions.onAlbum, bestMatch = true)
                is SearchMatch.Playlist -> SearchPlaylistRow(match.playlist, words, actions.onPlaylist)
                is SearchMatch.Song -> SearchSongRow(match.song, words, actions,
                    onClick = { actions.onSong(listOf(match.song), 0, "best") })
            }
        }
    }
    if (data.songs.isNotEmpty()) {
        item("songs_header") { SearchSectionHeader("单曲", { actions.onType(SearchType.Song) }) }
        songs(data.songs.take(5), "songs", words, actions)
    }
    val playlists = data.playlists.filter { candidate -> best.none { it is SearchMatch.Playlist && it.playlist.id == candidate.id } }.take(3)
    if (playlists.isNotEmpty()) {
        item("playlists_header") { SearchSectionHeader("歌单", { actions.onType(SearchType.Playlist) }) }
        items(playlists, key = { "playlist-${it.id}" }) { SearchPlaylistRow(it, words, actions.onPlaylist) }
    }
    val albums = data.albums.filter { candidate -> best.none { it is SearchMatch.Album && it.album.id == candidate.id } }.take(6)
    if (albums.isNotEmpty()) {
        item("albums_header") { SearchSectionHeader("专辑", { actions.onType(SearchType.Album) }) }
        item("albums") {
            LazyRow(contentPadding = PaddingValues(horizontal = meiPageGutter()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(albums, key = { it.id }) { album ->
                    Column(Modifier.width(136.dp * VinylAlbumArtworkRatio).clickable { actions.onAlbum(album) }
                        .padding(bottom = 8.dp)) {
                        VinylAlbumArtwork(album.coverUrl)
                        Text(highlight(album.title, words), modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(albumSubtitle(album), modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
    val artists = data.artists.filter { candidate -> best.none { it is SearchMatch.Artist && it.artist.id == candidate.id } }.take(3)
    if (artists.isNotEmpty()) {
        item("artists_header") { SearchSectionHeader("歌手", { actions.onType(SearchType.Artist) }) }
        items(artists, key = { "artist-${it.id}" }) { SearchArtistRow(it, words, actions.onArtist) }
    }
    if (data.recommendations.isNotEmpty()) {
        item("recommendations_header") { SearchSectionHeader("你可能也喜欢") }
        songs(data.recommendations.take(3), "recommendations", words, actions)
    }
}

private fun LazyListScope.songs(songs: List<MediaMetadata>, section: String, words: List<String>, actions: SearchActions) {
    itemsIndexed(songs, key = { _, song -> "$section-${song.id}" }) { index, song ->
        SearchSongRow(song, words, actions, onClick = { actions.onSong(songs, index, section) })
    }
}

@Composable
internal fun SearchSongRow(
    song: MediaMetadata, words: List<String>, actions: SearchActions, onClick: () -> Unit,
    showColumns: Boolean = false, index: Int? = null,
) {
    val subtitle = listOf(song.artists.joinToString(" / ") { it.name }, song.album.title.takeUnless { showColumns }.orEmpty())
        .filter(String::isNotBlank).joinToString(" · ")
    // Track owns menu positioning and the shared playing artwork; search adds only text presentation.
    Track(track = song, index = index, isTablet = showColumns, isPlaying = actions.playingId == song.id, onClick = onClick,
        onMoreClick = { actions.onMore(song, it) }, moreContentDescription = "更多操作：${song.title}",
        title = highlight(song.title, words), subtitle = highlight(subtitle, words), supportingText = song.tns)
}

@Composable
internal fun SearchArtistRow(artist: SearchArtist, words: List<String>, onClick: (SearchArtist) -> Unit, bestMatch: Boolean = false) {
    SearchEntityRow(artist.name, artist.aliases.joinToString(" / ").ifBlank { "歌手" }, artist.imageUrl,
        words, round = true, bestMatch = bestMatch, onClick = { onClick(artist) })
}

@Composable
internal fun SearchAlbumRow(album: SearchAlbum, words: List<String>, onClick: (SearchAlbum) -> Unit, bestMatch: Boolean = false) {
    SearchEntityRow(album.title, listOf(albumSubtitle(album), if (bestMatch) "专辑" else "${album.size} 首")
        .filter(String::isNotBlank).joinToString(" · "), album.coverUrl, words,
        bestMatch = bestMatch, albumArtwork = true, onClick = { onClick(album) })
}

@Composable
internal fun SearchPlaylistRow(playlist: SearchPlaylist, words: List<String>, onClick: (SearchPlaylist) -> Unit) {
    val subtitle = listOf("${playlist.trackCount} 首", playlist.creatorName,
        playlist.playCount?.takeIf { it > 0 }?.let { "${formatSearchPlayCount(it)}次播放" }.orEmpty())
        .filter(String::isNotBlank).joinToString(" · ")
    SearchEntityRow(playlist.title, subtitle, playlist.coverUrl, words, titleLines = 2, onClick = { onClick(playlist) })
}

@Composable
private fun SearchEntityRow(
    title: String, subtitle: String, image: String?, words: List<String>, round: Boolean = false,
    bestMatch: Boolean = false, titleLines: Int = 1, isLoading: Boolean = false,
    albumArtwork: Boolean = false, onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().clickable(enabled = !isLoading, onClick = onClick)
        .then(if (isLoading) Modifier.clearAndSetSemantics { } else Modifier)
        .padding(horizontal = meiPageGutter(), vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        val coverSize = if (bestMatch) 64.dp else 56.dp
        if (albumArtwork) {
            VinylAlbumArtwork(image, Modifier.size(width = coverSize * VinylAlbumArtworkRatio, height = coverSize),
                isLoading = isLoading)
        } else {
            val artworkShape = if (round) CircleShape else MaterialTheme.shapes.small
            val artworkModifier = Modifier.size(coverSize).clip(artworkShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
            if (isLoading) Spacer(artworkModifier.skeleton(true, artworkShape))
            else AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = artworkModifier)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(highlight(title, words), modifier = Modifier.skeleton(isLoading), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium, maxLines = titleLines, overflow = TextOverflow.Ellipsis)
            Text(highlight(subtitle, words), modifier = Modifier.skeleton(isLoading), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (bestMatch) Icon(Icons.Rounded.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun SearchSectionHeader(title: String, onMore: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = meiPageGutter(), end = meiPageGutter(), top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (onMore != null) TextButton(onClick = onMore, modifier = Modifier.semantics { contentDescription = "查看全部$title" }) {
            Text("查看全部")
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun SearchStatusPanel(title: String, description: String, action: String?, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = meiPageGutter(), vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
internal fun highlight(text: String, words: List<String>): AnnotatedString {
    val color = MaterialTheme.colorScheme.primary
    return remember(text, words, color) {
        buildAnnotatedString {
            append(text)
            words.filter(String::isNotBlank).distinct().forEach { word ->
                var start = text.indexOf(word, ignoreCase = true)
                while (start >= 0) {
                    addStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold), start, start + word.length)
                    start = text.indexOf(word, start + word.length, ignoreCase = true)
                }
            }
        }
    }
}

internal fun albumSubtitle(album: SearchAlbum): String = listOf(
    album.artists.joinToString(" / ") { it.name },
    album.publishTime?.let { SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(it)) }.orEmpty(),
).filter(String::isNotBlank).joinToString(" · ")

private fun formatSearchPlayCount(count: Long): String = when {
    count >= 100_000_000 -> String.format(Locale.getDefault(), "%.1f 亿", count / 100_000_000.0)
    count >= 10_000 -> String.format(Locale.getDefault(), "%.1f 万", count / 10_000.0)
    else -> "$count "
}
