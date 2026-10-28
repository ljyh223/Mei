package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.MeiSectionTitle
import com.ljyh.mei.ui.component.MeiShowAllButton
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.component.card.ArtworkCard
import com.ljyh.mei.ui.component.glass.*
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.main.library.*
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Artwork composition leads the overview; secondary collections retain their own browsing rhythm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryBrowseLayout(
    state: LibraryUiState,
    likedArtwork: String?,
    likedSongTitle: String?,
    signedIn: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    preparingPlayback: Boolean,
    canPlay: Boolean,
    onPlayLiked: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenCollection: (LibraryCollection) -> Unit,
    onLogin: () -> Unit,
    onRetry: () -> Unit,
) {
    val source = rememberHazeState()
    val listState = rememberLazyListState()
    val tablet = rememberDeviceInfo().isTablet
    val gutter = meiPageGutter()
    val rowPadding = PaddingValues(horizontal = gutter)
    val bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val distance = with(LocalDensity.current) { 56.dp.toPx() }
    val progress by remember(listState, distance) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / distance).coerceIn(0f, 1f)
        }
    }
    val data = (state as? LibraryUiState.Content)?.data
    val composition = remember(data) { data?.dashboardSelection() }
    val collected = remember(data) { data?.playlistsIn(LibraryCollection.Collected).orEmpty() }
    val createdLimit = if (tablet) 8 else 4
    val remainingLimit = createdLimit - (if (composition?.topCreated != null) 1 else 0)
    val collectedLimit = if (tablet) 8 else 6
    val loading = state is LibraryUiState.Loading || data?.let {
        it.playlistsLoading && it.createdPlaylists.isEmpty() && it.collectedPlaylists.isEmpty()
    } == true
    val failed = state is LibraryUiState.Error || data?.let {
        it.playlistsError != null && it.createdPlaylists.isEmpty() && it.collectedPlaylists.isEmpty()
    } == true

    BoxWithConstraints(Modifier
        .fillMaxSize()
        .nestedScroll(scrollBehavior.nestedScrollConnection)) {
        val columns =
            if (tablet) ((maxWidth - gutter * 2 + 16.dp) / 184.dp).toInt().coerceIn(2, 6) else 2
        val createdRows = remember(composition, remainingLimit, columns) {
            composition?.remainingCreated.orEmpty().take(remainingLimit).chunked(columns)
        }
        CompositionLocalProvider(
            LocalMeiHazeState provides source,
            LocalMeiPageProgress provides progress
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(source)
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(top = top + 12.dp, bottom = bottom + 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "title") {
                    MeiLargeTitle(
                        "音乐库",
                        Modifier.padding(rowPadding),
                        startPadding = 0.dp
                    )
                }
                when {
                    !signedIn -> item(key = "login") {
                        MeiStatePanel(
                            "登录查看歌单与收藏",
                            Icons.Rounded.LibraryMusic,
                            actionLabel = "登录账号",
                            onAction = onLogin
                        )
                    }

                    loading -> item(key = "loading") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            Alignment.Center
                        ) { CircularProgressIndicator() }
                    }

                    failed -> item(key = "error") {
                        MeiStatePanel(
                            "暂时无法同步音乐库",
                            Icons.Rounded.CloudOff,
                            actionLabel = "重试",
                            onAction = onRetry
                        )
                    }

                    else -> {
                        if (!tablet && data != null) {
                            mobileLibraryContent(
                                data,
                                likedArtwork,
                                likedSongTitle,
                                preparingPlayback,
                                canPlay,
                                rowPadding,
                                onOpenPlaylist,
                                onPlayLiked,
                                onOpenAlbum,
                                onOpenCollection
                            )
                        } else {
                            if (composition != null) item(key = "library_dashboard") {
                                LibraryDashboard(
                                    composition,
                                    likedArtwork,
                                    preparingPlayback,
                                    canPlay,
                                    loadingRecent = data?.playlistsLoading == true,
                                    recentSyncFailed = data?.playlistsError != null,
                                    onOpenPlaylist = onOpenPlaylist,
                                    onPlayLiked = onPlayLiked,
                                    modifier = Modifier.padding(rowPadding)
                                )
                            }
                            if (!composition?.remainingCreated.isNullOrEmpty() || composition?.topCreated == null) item(
                                key = "created_heading"
                            ) {
                                LibrarySectionHeading(
                                    "我创建的",
                                    data?.createdPlaylistOverview().orEmpty().size > createdLimit,
                                    modifier = Modifier.padding(rowPadding)
                                ) { onOpenCollection(LibraryCollection.Created) }
                            }
                            if (composition?.remainingCreated.isNullOrEmpty() && composition?.topCreated == null) item(
                                key = "created_empty"
                            ) {
                                LibraryEmptyLabel("还没有创建歌单", Modifier.padding(rowPadding))
                            }
                            items(createdRows, key = { "created_row_${it.first().id}" }) { row ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(rowPadding),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    row.forEach { playlist ->
                                        ArtworkCard(
                                            playlist.title,
                                            playlist.cover,
                                            Modifier.weight(1f),
                                            subtitle = "${playlist.count} 首",
                                            onClick = { onOpenPlaylist(playlist.id) })
                                    }
                                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            item(key = "collected_heading") {
                                LibrarySectionHeading(
                                    "收藏歌单", collected.size > collectedLimit,
                                    modifier = Modifier
                                        .padding(rowPadding)
                                        .padding(top = 16.dp)
                                ) { onOpenCollection(LibraryCollection.Collected) }
                            }
                            if (collected.isEmpty()) item(key = "collected_empty") {
                                LibraryEmptyLabel("还没有收藏歌单", Modifier.padding(rowPadding))
                            } else item(key = "collected_shelf") {
                                LazyRow(
                                    contentPadding = rowPadding,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        collected.take(collectedLimit),
                                        key = { it.id }) { playlist ->
                                        ArtworkCard(
                                            playlist.title,
                                            playlist.cover,
                                            Modifier.width(if (tablet) 144.dp else 128.dp),
                                            subtitle = playlist.authorName,
                                            reserveSubtitleSpace = true,
                                            onClick = { onOpenPlaylist(playlist.id) })
                                    }
                                }
                            }
                            item(key = "albums_entry") {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(rowPadding)
                                        .padding(top = 16.dp)
                                        .clickable(role = Role.Button) {
                                            onOpenCollection(
                                                LibraryCollection.Albums
                                            )
                                        }
                                        .heightIn(min = 64.dp)
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(
                                        Icons.Rounded.Album,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "收藏专辑",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        when {
                                            data?.albumsLoading == true -> "同步中"
                                            data?.albumsError != null -> "同步失败"
                                            else -> "${data?.albums?.size ?: 0} 张"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (state is LibraryUiState.Content && state.warning != null) item(key = "warning") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(rowPadding),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "部分收藏同步失败",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onRetry) { Text("重试") }
                            }
                        }
                    }
                }
            }
        }
        MeiHazeTopBar(state = source, title = "音乐库", progress = progress)
    }
}

@Composable
private fun LibrarySectionHeading(
    title: String,
    showAll: Boolean,
    modifier: Modifier = Modifier,
    onShowAll: () -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MeiSectionTitle(title, Modifier.weight(1f))
        if (showAll) MeiShowAllButton(onClick = onShowAll)
    }
}

@Composable
private fun LibraryEmptyLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}


private fun androidx.compose.foundation.lazy.LazyListScope.mobileLibraryContent(
    data: LibraryContentUiState,
    likedArtwork: String?,
    likedSongTitle: String?,
    preparingPlayback: Boolean,
    canPlay: Boolean,
    rowPadding: PaddingValues,
    onOpenPlaylist: (String) -> Unit,
    onPlayLiked: (String) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenCollection: (LibraryCollection) -> Unit,
) {
    val liked = data.likedPlaylist()
    val created = data.createdPlaylistOverview()
    val collected = data.playlistsIn(LibraryCollection.Collected)
    item(key = "mobile_liked") {
        LibraryLikedPlaylist(
            liked, likedArtwork, preparingPlayback, canPlay,
            modifier = Modifier
                .fillMaxWidth()
                .padding(rowPadding)
                .height(190.dp),
            onOpen = { liked?.let { onOpenPlaylist(it.id) } },
            onPlay = { liked?.let { onPlayLiked(it.id) } },
            songTitle = likedSongTitle, playAtBottom = true
        )
    }
    item(key = "mobile_created_heading") {
        LibrarySectionHeading(
            "我创建的歌单",
            true,
            Modifier
                .padding(rowPadding)
                .padding(top = 8.dp)
        ) {
            onOpenCollection(LibraryCollection.Created)
        }
    }
    if (created.isEmpty()) item(key = "mobile_created_empty") {
        LibraryEmptyLabel(
            if (data.playlistsLoading) "同步中" else if (data.playlistsError != null) "暂时无法同步歌单" else "还没有创建歌单",
            Modifier.padding(rowPadding)
        )
    }
    items(created.take(4).chunked(2), key = { "mobile_created_${it.first().id}" }) { row ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(rowPadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            row.forEach { playlist ->
                ArtworkCard(
                    playlist.title,
                    playlist.cover,
                    Modifier.weight(1f),
                    subtitle = "${playlist.count} 首",
                ) {
                    onOpenPlaylist(playlist.id)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    item(key = "mobile_collected_heading") {
        LibrarySectionHeading(
            "收藏歌单",
            true,
            Modifier
                .padding(rowPadding)
                .padding(top = 8.dp)
        ) {
            onOpenCollection(LibraryCollection.Collected)
        }
    }
    if (collected.isEmpty()) item(key = "mobile_collected_empty") {
        LibraryEmptyLabel(
            if (data.playlistsLoading) "同步中" else if (data.playlistsError != null) "暂时无法同步歌单" else "还没有收藏歌单",
            Modifier.padding(rowPadding)
        )
    }
    items(collected.take(4).chunked(2), key = { "mobile_collected_${it.first().id}" }) { row ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(rowPadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            row.forEach { playlist ->
                ArtworkCard(
                    playlist.title,
                    playlist.cover,
                    Modifier.weight(1f),
                    subtitle = playlist.authorName,
                ) {
                    onOpenPlaylist(playlist.id)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    item(key = "mobile_albums_heading") {
        LibrarySectionHeading("收藏专辑", true, Modifier
            .padding(rowPadding)
            .padding(top = 8.dp)) {
            onOpenCollection(LibraryCollection.Albums)
        }
    }
    if (data.albums.isEmpty()) item(key = "mobile_albums_empty") {
        LibraryEmptyLabel(
            if (data.albumsLoading) "同步中" else if (data.albumsError != null) "暂时无法同步专辑" else "还没有收藏专辑",
            Modifier.padding(rowPadding)
        )
    } else item(key = "mobile_albums_shelf") {
        LazyRow(contentPadding = rowPadding, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(data.albums, key = { it.id }) { album ->
                ArtworkCard(
                    album.title, album.cover, Modifier.width(144.dp),
                    subtitle = album.artist.joinToString(" / ") { it.name },
                    reserveSubtitleSpace = true
                ) { onOpenAlbum(album.id) }
            }
        }
    }
}
