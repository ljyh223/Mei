package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.screen.artist.component.ArtistLatestReleaseCard
import com.ljyh.mei.ui.screen.artist.component.TabletArtistSongRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The first content section shares the page's vertical scroll; songs only scroll horizontally. */
@Composable
internal fun TabletArtistOverview(
    state: ArtistDetailUiState,
    latest: Album?,
    actions: ArtistDetailActions,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val latestWidth = (maxWidth * .45f).coerceAtMost(480.dp * fontScale)
        if (maxWidth >= 840.dp * fontScale) {
            // The square release image spans the natural height of the song rows beside it.
            Layout(modifier = Modifier.fillMaxWidth(), content = {
                LatestArtistRelease(state, latest, actions, Modifier.fillMaxSize(), matchHeight = true)
                ArtistTopSongs(state, actions, Modifier.fillMaxWidth())
            }) { children, constraints ->
                val leftWidth = latestWidth.roundToPx()
                val songs = children[1].measure(Constraints.fixedWidth(constraints.maxWidth - leftWidth))
                val release = children[0].measure(Constraints.fixed(leftWidth, songs.height))
                layout(constraints.maxWidth, songs.height) {
                    release.placeRelative(0, 0)
                    songs.placeRelative(leftWidth, 0)
                }
            }
        } else Column {
            LatestArtistRelease(state, latest, actions, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            ArtistTopSongs(state, actions, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LatestArtistRelease(
    state: ArtistDetailUiState,
    album: Album?,
    actions: ArtistDetailActions,
    modifier: Modifier,
    matchHeight: Boolean = false,
) {
    Column(modifier) {
        SectionTitle("Latest Release", modifier = Modifier.heightIn(min = 64.dp))
        Spacer(Modifier.height(12.dp))
        val bodyModifier = if (matchHeight) Modifier.weight(1f) else Modifier
        Box(bodyModifier.fillMaxWidth().padding(meiPageHorizontalPadding())) {
            val cardModifier = if (matchHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
            when {
                state.albumsLoading -> SkeletonShimmerHost(true) {
                    ArtistLatestReleaseCard(null, cardModifier, loading = true, onClick = {})
                }
                album != null -> ArtistLatestReleaseCard(album, cardModifier,
                    onClick = { actions.onAlbumClick(album.id) })
                state.albumsError != null -> MeiStatePanel("发行作品暂未加载", Icons.Rounded.CloudOff,
                    actionLabel = "重试", onAction = actions.onRetryAlbums)
                else -> EmptyArtistSection("暂无发行作品")
            }
        }
    }
}

@Composable
private fun ArtistTopSongs(state: ArtistDetailUiState, actions: ArtistDetailActions, modifier: Modifier) {
    Column(modifier) {
        SectionTitle("热门单曲", actions.onBrowseSongs, Modifier.heightIn(min = 64.dp))
        Spacer(Modifier.height(12.dp))
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val visibleColumns = (maxWidth / (340.dp * fontScale)).toInt().coerceIn(1, 2)
            val columnWidth = maxWidth / visibleColumns
            when {
                state.songsLoading -> SkeletonShimmerHost(true) {
                    Row {
                        repeat(visibleColumns) {
                            Column(Modifier.width(columnWidth)) {
                                repeat(3) { TabletArtistSongRow(emptyList(), 1, actions, loading = true) }
                            }
                        }
                    }
                }
                state.songsError != null -> MeiStatePanel("热门歌曲暂未加载", Icons.Rounded.CloudOff,
                    actionLabel = "重试", onAction = actions.onRetrySongs)
                state.songs.isEmpty() -> EmptyArtistSection("暂无热门单曲")
                else -> LazyRow {
                    itemsIndexed(state.songs.take(12).withIndex().chunked(3),
                        key = { _, songs -> songs.first().value.id }) { _, songs ->
                        Column(Modifier.width(columnWidth)) {
                            songs.forEach { song -> TabletArtistSongRow(listOf(song), 1, actions) }
                        }
                    }
                }
            }
        }
    }
}

internal fun artistReleaseDate(album: Album, fullDate: Boolean = false): String? =
    album.publishTime.takeIf { it > 0 }?.let {
        SimpleDateFormat(if (fullDate) "yyyy/MM/dd" else "yyyy", Locale.getDefault()).format(Date(it))
    }

internal fun artistReleaseSubtitle(album: Album, fullDate: Boolean = false): String {
    val date = artistReleaseDate(album, fullDate)
    val type = album.releaseType.takeIf(String::isNotBlank) ?: "${album.size} 首"
    return listOfNotNull(date, type).joinToString(" · ")
}
