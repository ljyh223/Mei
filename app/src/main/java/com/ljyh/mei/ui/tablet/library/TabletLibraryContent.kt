package com.ljyh.mei.ui.tablet.library

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.component.card.ArtworkCard
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.screen.main.library.*
import com.ljyh.mei.ui.component.artwork.PlaylistCoverStage
import com.ljyh.mei.ui.component.shimmer.skeleton
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ljyh.mei.ui.tablet.*
import com.ljyh.mei.ui.tablet.component.*

@Composable
internal fun TabletLibraryContent(state: LibraryPageState, actions: LibraryPageActions) {
    AdaptiveTabletContent(
        sideWidthFraction = 0.4f, maxSideWidthDp = 480f,
        compact = { LibraryPageContent(state, actions) },
    ) { metrics ->
        val content = (state.state as? LibraryUiState.Content)?.data
        val liked = content?.likedPlaylist()
        val created = content?.createdPlaylistOverview().orEmpty()
        val collected = content?.playlistsIn(LibraryCollection.Collected).orEmpty()
        val loading = state.state is LibraryUiState.Loading
        TabletDetailPage(
            title = "音乐库", sideWidth = metrics.sideWidthDp.dp,
            sidePane = { padding ->
                LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center) {
                    item(key = "liked") {
                        LibraryLikedSection(
                            liked, state, actions,
                            metadataLoading = state.signedIn && (loading ||
                                (content?.playlistsLoading == true && liked == null)),
                        )
                    }
                }
            },
            mainPane = { padding ->
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val columns = tabletArtworkColumns(maxWidth.value, LocalDensity.current.fontScale, meiPageGutter().value)
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when {
                            !state.signedIn -> item { MeiStatePanel("登录查看歌单与收藏", Icons.Rounded.LibraryMusic,
                                actionLabel = "登录账号", onAction = actions.onLogin) }
                            state.state is LibraryUiState.Error -> item { MeiStatePanel("暂时无法同步音乐库", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetry) }
                            loading -> {
                                item { TabletSectionHeading("我创建的") }
                                items(2) { TabletArtworkRow((0 until columns).toList(), columns) { _, modifier ->
                                    ArtworkCard("", null, modifier, isLoading = true, onClick = {})
                                } }
                            }
                            content != null -> {
                                item(key = "created_heading") { TabletSectionHeading("我创建的", if (created.size > 12) ({ actions.onOpenCollection(LibraryCollection.Created) }) else null) }
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
                                item(key = "collected_heading") { TabletSectionHeading("收藏歌单", if (collected.size > 8) ({ actions.onOpenCollection(LibraryCollection.Collected) }) else null) }
                                if (collected.isEmpty()) item { LibraryEmpty(if (content.playlistsLoading) "正在同步收藏歌单" else "还没有收藏歌单") }
                                items(collected.take(8).chunked(columns), key = { "collected_${it.first().id}" }) { row ->
                                    TabletArtworkRow(row, columns) { playlist, modifier ->
                                        ArtworkCard(playlist.title, playlist.cover, modifier, subtitle = playlist.authorName,
                                            onClick = { actions.onOpenPlaylist(playlist.id) })
                                    }
                                }
                                item(key = "albums_heading") { TabletSectionHeading("收藏专辑", if (content.albums.size > columns) ({ actions.onOpenCollection(LibraryCollection.Albums) }) else null) }
                                when {
                                    content.albumsLoading && content.albums.isEmpty() -> item {
                                        TabletArtworkRow((0 until columns).toList(), columns) { _, modifier -> ArtworkCard("", null, modifier, isLoading = true, onClick = {}) }
                                    }
                                    content.albumsError != null && content.albums.isEmpty() -> item { MeiStatePanel("专辑暂未同步", Icons.Rounded.CloudOff,
                                        actionLabel = "重试", onAction = actions.onRetry) }
                                    content.albums.isEmpty() -> item { LibraryEmpty("还没有收藏专辑") }
                                    else -> item { TabletArtworkRow(content.albums.take(columns), columns) { album, modifier ->
                                        ArtworkCard(album.title, album.cover, modifier, subtitle = album.artist.joinToString(" / ") { it.name },
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
) {
    val artworkLoading = state.signedIn && (metadataLoading || state.likedArtworkLoading)
    Column(Modifier.fillMaxWidth().padding(horizontal = meiPageGutter()),
        horizontalAlignment = Alignment.CenterHorizontally) {
        PlaylistCoverStage(
            covers = state.likedArtworks, liked = true,
            onCoverClick = { liked?.let { actions.onOpenPlaylist(it.id) } },
            clickLabel = "打开我喜欢的音乐", modifier = Modifier.fillMaxWidth(),
            isLoading = artworkLoading,
            placeholderCoverCount = liked?.count?.coerceIn(1, 3) ?: 3,
        )
        Spacer(Modifier.height(20.dp))
        SkeletonShimmerHost(metadataLoading) {
            Column(
                Modifier.widthIn(max = 320.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))
                    .fillMaxWidth()
                    .clickable(enabled = liked != null && !metadataLoading,
                        onClickLabel = "打开我喜欢的音乐") { liked?.let { actions.onOpenPlaylist(it.id) } },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("我喜欢的音乐", modifier = Modifier.skeleton(metadataLoading),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Button(
                        onClick = { liked?.let { actions.onPlayLiked(it.id) } },
                        enabled = liked != null && liked.count > 0 && state.canPlay && !state.preparingPlayback && !metadataLoading,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                            .skeleton(metadataLoading, RoundedCornerShape(50)).semantics {
                                contentDescription = if (state.preparingPlayback) "正在准备播放我喜欢的音乐" else "播放我喜欢的音乐"
                            },
                    ) {
                        if (state.preparingPlayback) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
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
