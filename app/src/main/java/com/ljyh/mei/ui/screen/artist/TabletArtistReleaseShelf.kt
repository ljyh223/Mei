package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.card.VinylAlbumCard
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.model.Album

@Composable
internal fun TabletArtistReleaseShelf(
    title: String,
    releases: List<Album>,
    state: ArtistDetailUiState,
    actions: ArtistDetailActions,
    onBrowse: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        SectionTitle(title, onBrowse)
        Spacer(Modifier.height(12.dp))
        when {
            state.albumsLoading -> SkeletonShimmerHost(true) {
                LazyRow(contentPadding = meiPageHorizontalPadding(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(4) { VinylAlbumCard("", "", null,
                        Modifier.width(164.dp * VinylAlbumArtworkRatio), isLoading = true, onClick = {}) }
                }
            }
            releases.isNotEmpty() -> LazyRow(contentPadding = meiPageHorizontalPadding(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(releases.take(12), key = { it.id }) { album ->
                    VinylAlbumCard(album.title, artistReleaseSubtitle(album), album.cover,
                        Modifier.width(164.dp * VinylAlbumArtworkRatio), onClick = { actions.onAlbumClick(album.id) })
                }
            }
            state.albumsError != null -> MeiStatePanel("发行作品暂未加载", Icons.Rounded.CloudOff,
                actionLabel = "重试", onAction = actions.onRetryAlbums)
            state.albumsHasMore -> Column {
                EmptyArtistSection("当前预览中暂无$title")
                TextButton(onClick = onBrowse, modifier = Modifier.padding(meiPageHorizontalPadding())) {
                    Text("浏览全部$title")
                }
            }
            else -> EmptyArtistSection("暂无$title")
        }
    }
}
