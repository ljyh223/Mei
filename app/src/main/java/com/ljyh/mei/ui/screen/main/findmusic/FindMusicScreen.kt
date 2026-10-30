package com.ljyh.mei.ui.screen.main.findmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.MeiFilterPill
import com.ljyh.mei.ui.component.glass.LocalMeiPageProgress
import com.ljyh.mei.ui.component.glass.MeiHazeTopBar
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.discoverPlaylistsRoute
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.card.VinylAlbumCard
import com.ljyh.mei.ui.component.card.VinylAlbumArtworkRatio

/** Discovery owns the overview only; complete collections are navigation destinations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindMusicScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: FindMusicViewModel = hiltViewModel()
) {
    val nav = LocalNavController.current
    val categories by viewModel.categories.collectAsState()
    val hotCategories by viewModel.hotCategories.collectAsState()
    val featured by viewModel.featured.collectAsState()
    val albums by viewModel.albums.collectAsState()
    var categorySheet by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val source = rememberHazeState()
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val tablet = rememberDeviceInfo().isTablet
    val gutter = meiPageGutter()
    val rowInsets = PaddingValues(horizontal = gutter)
    val collapseDistance = with(LocalDensity.current) { 56.dp.toPx() }
    val progress by remember(listState, collapseDistance) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseDistance).coerceIn(0f, 1f)
        }
    }
    val openPlaylists: (String) -> Unit = { nav.navigate(discoverPlaylistsRoute(it)) }
    val openPlaylist: (Long) -> Unit =
        { id -> Screen.PlayList.navigate(nav) { addPath(id.toString()) } }
    val openAlbum: (Long) -> Unit = { id -> Screen.Album.navigate(nav) { addPath(id.toString()) } }

    Box(Modifier
        .fillMaxSize()
        .nestedScroll(scrollBehavior.nestedScrollConnection)) {
        CompositionLocalProvider(LocalMeiPageProgress provides progress) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(source)
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(
                    top = top + 12.dp,
                    bottom = insets.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "discover_title") {
                    MeiLargeTitle(
                        "发现",
                        modifier = Modifier.padding(rowInsets),
                        startPadding = 0.dp
                    )
                }
                item(key = "discover_playlists") {
                    Column(
                        Modifier.padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DiscoverSectionHeader(
                            "探索歌单",
                            { openPlaylists("全部") },
                            Modifier.padding(rowInsets)
                        )
                        LazyRow(
                            contentPadding = rowInsets,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                MeiFilterPill(
                                    "全部分类",
                                    false,
                                    { categorySheet = true },
                                    role = Role.Button,
                                    compact = true
                                )
                            }
                            val tags = (hotCategories as? Resource.Success)?.data.orEmpty()
                            items(tags.take(8), key = { it }) { category ->
                                MeiFilterPill(
                                    category,
                                    false,
                                    { openPlaylists(category) },
                                    role = Role.Button,
                                    compact = true
                                )
                            }
                            if (hotCategories is Resource.Error) item {
                                TextButton(onClick = viewModel::loadHotCategories) { Text("重试热门分类") }
                            }
                        }
                        DiscoverResource(featured, viewModel::loadFeatured) { entries ->
                            LazyRow(
                                contentPadding = rowInsets,
                                horizontalArrangement = Arrangement.spacedBy(if (tablet) 24.dp else 20.dp)
                            ) {
                                items(entries.take(8), key = { it.id }) { playlist ->
                                    DiscoverPlaylistCard(
                                        playlist,
                                        Modifier.width(if (tablet) 180.dp else 152.dp),
                                        onClick = {
                                            openPlaylist(playlist.id)
                                        },
                                        onPlayClick = {}
                                    )
                                }
                            }
                        }
                    }
                }
                item(key = "discover_albums") {
                    DiscoverSection(
                        "新碟上架",
                        albums,
                        viewModel::loadAlbums,
                        { Screen.DiscoverAlbums.navigate(nav) },
                        rowInsets
                    ) { entries ->
                        LazyRow(
                            contentPadding = rowInsets,
                            horizontalArrangement = Arrangement.spacedBy(if (tablet) 24.dp else 20.dp)
                        ) {
                            items(entries.take(8), key = { it.id }) { album ->
                                VinylAlbumCard(
                                    title = album.name,
                                    artist = album.artist,
                                    coverUrl = album.cover,
                                    modifier = Modifier.width((if (tablet) 168.dp else 136.dp) * VinylAlbumArtworkRatio),
                                ) { openAlbum(album.id) }
                            }
                        }
                    }
                }
            }
        }
        MeiHazeTopBar(state = source, title = "发现", progress = progress)
    }
    if (categorySheet) DiscoverCategorySheet(
        categories,
        "全部",
        { categorySheet = false },
        openPlaylists,
        viewModel::loadCategories
    )
}
