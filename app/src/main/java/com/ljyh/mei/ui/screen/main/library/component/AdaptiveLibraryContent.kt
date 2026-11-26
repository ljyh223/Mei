package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.MeiSectionTitle
import com.ljyh.mei.ui.component.MeiShowAllButton
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.component.glass.LocalMeiHazeState
import com.ljyh.mei.ui.component.card.ArtworkCard
import com.ljyh.mei.ui.component.card.VinylAlbumCard
import com.ljyh.mei.ui.component.artwork.ExpandedLikedCoverActionAlignment
import com.ljyh.mei.ui.component.artwork.ExpandedLikedCoverStageRatio
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.screen.main.library.*
import com.ljyh.mei.ui.screen.profile.PersonalCenterEntry
import com.ljyh.mei.ui.component.artwork.MeiArtworkActions
import com.ljyh.mei.ui.component.artwork.MeiArtworkPlayButton
import com.ljyh.mei.ui.component.artwork.PlaylistCoverStage
import com.ljyh.mei.ui.component.shimmer.skeleton
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ljyh.mei.ui.tablet.*
import com.ljyh.mei.ui.tablet.component.*

@Composable
internal fun AdaptiveLibraryContent(state: LibraryPageState, actions: LibraryPageActions) {
    val landscape = LocalPhoneLandscape.current
    AdaptiveTabletContent(
        sideWidthFraction = 0.4f,
        maxSideWidthDp = if (landscape) 300f else 480f,
        minSideWidthDp = if (landscape) 200f else 336f,
        minContentWidthDp = if (landscape) 300f else 480f,
        compact = { LibrarySinglePaneContent(state, actions) },
    ) { metrics ->
        val content = (state.state as? LibraryUiState.Content)?.data
        val liked = content?.likedPlaylist()
        val created = content?.createdPlaylistOverview().orEmpty()
        val collected = content?.playlistsIn(LibraryCollection.Collected).orEmpty()
        val loading = state.state is LibraryUiState.Loading
        val avatarUrl = content?.profile?.avatarUrl.takeIf { state.signedIn }
        TabletDetailPage(
            title = "音乐库", sideWidth = metrics.sideWidthDp.dp,
            showToolbar = !landscape,
            actions = { PersonalCenterEntry(actions.onOpenProfile, avatarUrl) },
            sidePane = { padding ->
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val availableHeight = maxHeight - padding.calculateTopPadding() - padding.calculateBottomPadding()
                    val artworkLimit = if (landscape) {
                        availableHeight.coerceAtLeast(80.dp) / ExpandedLikedCoverStageRatio
                    } else Dp.Infinity
                    LazyColumn(
                        contentPadding = padding,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = if (landscape) Arrangement.Top else Arrangement.Center,
                    ) {
                        item(key = "liked") {
                            LibraryLikedSection(
                                liked, state, actions,
                                metadataLoading = state.signedIn &&
                                    (loading || (content?.playlistsLoading == true && liked == null)),
                                artworkLimit = artworkLimit,
                            )
                        }
                    }
                }
            },
            mainPane = { padding ->
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val fontScale = LocalDensity.current.fontScale
                    val gutter = meiPageGutter().value
                    val minArtworkWidth = if (landscape) 104f else 156f
                    val columns = tabletArtworkColumns(maxWidth.value, fontScale, gutter, minArtworkWidthDp = minArtworkWidth)
                    val albumColumns = tabletArtworkColumns(
                        maxWidth.value, fontScale, gutter, VinylAlbumArtworkRatio, minArtworkWidthDp = minArtworkWidth,
                    )
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding,
                        verticalArrangement = Arrangement.spacedBy(if (landscape) 8.dp else 16.dp)) {
                        if (landscape) item(key = "library_heading") {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = meiPageGutter()).heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MeiSectionTitle(if (content != null || loading) "我创建的" else "音乐库", Modifier.weight(1f))
                                if (content != null) {
                                    MeiShowAllButton(onClick = { actions.onOpenCollection(LibraryCollection.Created) })
                                }
                                CompositionLocalProvider(LocalMeiHazeState provides null) {
                                    PersonalCenterEntry(actions.onOpenProfile, avatarUrl)
                                }
                            }
                        }
                        when {
                            !state.signedIn -> item { MeiStatePanel("登录查看歌单与收藏", Icons.Rounded.LibraryMusic,
                                actionLabel = "登录账号", onAction = actions.onLogin) }
                            state.state is LibraryUiState.Error -> item { MeiStatePanel("暂时无法同步音乐库", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetry) }
                            loading -> {
                                if (!landscape) item { TabletSectionHeading("我创建的") }
                                items(2) { TabletArtworkRow((0 until columns).toList(), columns) { _, modifier ->
                                    ArtworkCard("", null, modifier, isLoading = true, onClick = {})
                                } }
                            }
                            content != null -> {
                                if (!landscape) item(key = "created_heading") {
                                    TabletSectionHeading("我创建的", if (created.size > 12) ({ actions.onOpenCollection(LibraryCollection.Created) }) else null)
                                }
                                if (created.isEmpty()) {
                                    if (content.playlistsLoading) item {
                                        TabletArtworkRow((0 until columns).toList(), columns) { _, modifier -> ArtworkCard("", null, modifier, isLoading = true, onClick = {}) }
                                    } else item { LibraryEmpty("还没有其他自建歌单") }
                                }
                                items(created.take(12).chunked(columns), key = { "created_${it.first().id}" }) { row ->
                                    TabletArtworkRow(row, columns) { playlist, modifier ->
                                        ArtworkCard(playlist.title, playlist.cover, modifier, subtitle = "${playlist.count} 首",
                                            onClick = { actions.onOpenPlaylist(playlist.id) })
                                    }
                                }
                                if (content.playlistsError != null) item { MeiStatePanel("部分歌单暂未同步", Icons.Rounded.CloudOff,
                                    actionLabel = "重试", onAction = actions.onRetry) }
                                item(key = "collected_heading") { TabletSectionHeading("收藏歌单", if (landscape || collected.size > 8) ({ actions.onOpenCollection(LibraryCollection.Collected) }) else null) }
                                if (collected.isEmpty()) item { LibraryEmpty(if (content.playlistsLoading) "正在同步收藏歌单" else "还没有收藏歌单") }
                                items(collected.take(8).chunked(columns), key = { "collected_${it.first().id}" }) { row ->
                                    TabletArtworkRow(row, columns) { playlist, modifier ->
                                        ArtworkCard(playlist.title, playlist.cover, modifier, subtitle = playlist.authorName,
                                            onClick = { actions.onOpenPlaylist(playlist.id) })
                                    }
                                }
                                item(key = "albums_heading") { TabletSectionHeading("收藏专辑", if (landscape || content.albums.size > albumColumns) ({ actions.onOpenCollection(LibraryCollection.Albums) }) else null) }
                                when {
                                    content.albumsLoading && content.albums.isEmpty() -> item {
                                        TabletArtworkRow((0 until albumColumns).toList(), albumColumns) { _, modifier -> VinylAlbumCard("", "", null, modifier, isLoading = true, onClick = {}) }
                                    }
                                    content.albumsError != null && content.albums.isEmpty() -> item { MeiStatePanel("专辑暂未同步", Icons.Rounded.CloudOff,
                                        actionLabel = "重试", onAction = actions.onRetry) }
                                    content.albums.isEmpty() -> item { LibraryEmpty("还没有收藏专辑") }
                                    else -> item { TabletArtworkRow(content.albums.take(albumColumns), albumColumns) { album, modifier ->
                                        VinylAlbumCard(album.title, album.artist.joinToString(" / ") { it.name }, album.cover, modifier,
                                            onClick = { actions.onOpenAlbum(album.id) })
                                    } }
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun LibraryEmpty(message: String) {
    Text(message, Modifier.padding(horizontal = meiPageGutter(), vertical = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}


@Composable
private fun LibraryLikedSection(
    liked: com.ljyh.mei.data.model.room.Playlist?,
    state: LibraryPageState,
    actions: LibraryPageActions,
    metadataLoading: Boolean,
    artworkLimit: Dp,
) {
    val artworkLoading = state.signedIn && (metadataLoading || state.likedArtworkLoading)
    val landscape = LocalPhoneLandscape.current
    val openLiked: () -> Unit = { liked?.let { actions.onOpenPlaylist(it.id) } }
    val playLiked: () -> Unit = { liked?.let { actions.onPlayLiked(it.id) } }
    val canPlay = liked != null && liked.count > 0 && state.canPlay && !state.preparingPlayback && !metadataLoading
    val playLabel = if (state.preparingPlayback) "正在准备播放我喜欢的音乐" else "播放我喜欢的音乐"
    Column(
        Modifier.fillMaxWidth().padding(horizontal = if (landscape) 12.dp else meiPageGutter()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val artwork: @Composable () -> Unit = {
            val openModifier = if (landscape) {
                Modifier.clickable(
                    enabled = liked != null && !metadataLoading,
                    role = Role.Button,
                    onClick = openLiked,
                ).semantics { contentDescription = "打开我喜欢的音乐" }
            } else Modifier
            PlaylistCoverStage(
                covers = state.likedArtworks,
                liked = true,
                onCoverClick = if (landscape) null else { _ -> openLiked() },
                modifier = Modifier.fillMaxWidth().then(openModifier),
                clickLabel = "打开我喜欢的音乐",
                isLoading = artworkLoading,
                placeholderCoverCount = liked?.count?.coerceIn(1, 3) ?: 3,
                expandedLikedArtwork = landscape,
            )
        }
        if (landscape) {
            MeiArtworkActions(
                modifier = Modifier.widthIn(max = artworkLimit).fillMaxWidth(),
                actionAlignment = ExpandedLikedCoverActionAlignment,
                artwork = { artwork() },
            ) {
                MeiArtworkPlayButton(
                    enabled = canPlay,
                    onPlayAll = playLiked,
                    preparing = state.preparingPlayback,
                    accessibilityLabel = playLabel,
                )
            }
        } else {
            artwork()
            Spacer(Modifier.height(20.dp))
            SkeletonShimmerHost(metadataLoading) {
                Column(
                    Modifier.widthIn(max = 320.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))
                        .fillMaxWidth()
                        .clickable(enabled = liked != null && !metadataLoading,
                            onClickLabel = "打开我喜欢的音乐", onClick = openLiked),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("我喜欢的音乐", modifier = Modifier.skeleton(metadataLoading),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Button(
                            onClick = playLiked,
                            enabled = canPlay,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                                .skeleton(metadataLoading, RoundedCornerShape(50)).semantics {
                                    contentDescription = playLabel
                                },
                        ) {
                            LikedPlaybackIcon(state.preparingPlayback)
                            Spacer(Modifier.width(6.dp))
                            Text("播放", maxLines = 1)
                        }
                    }
                    if (liked != null || metadataLoading) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (metadataLoading) "正在加载歌曲信息" else buildString {
                                append("${liked?.count ?: 0} 首")
                                state.likedSongTitle?.takeIf(String::isNotBlank)?.let { append(" · 最近喜欢《$it》") }
                            }, modifier = Modifier.fillMaxWidth().skeleton(metadataLoading),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LikedPlaybackIcon(preparing: Boolean) {
    if (preparing) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
    }
}
