package com.ljyh.mei.ui.screen.main.findmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.domain.*
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.MeiFilterPill
import com.ljyh.mei.ui.component.MeiSectionTitle
import com.ljyh.mei.ui.component.collection.MeiCollectionStatus
import com.ljyh.mei.ui.component.card.PlaylistCard
import com.ljyh.mei.ui.component.card.ArtworkCard
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.utils.image.sizedNeteaseImage
import java.util.Locale
import com.ljyh.mei.ui.component.layout.meiPageGutter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscoverCategorySheet(
    categories: Resource<List<DiscoverCategory>>,
    selectedCategory: String,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val gutter = meiPageGutter()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { Text("歌单分类", style = MaterialTheme.typography.headlineSmall) }
            item {
                MeiFilterPill("全部", selectedCategory == "全部", {
                    onDismiss()
                    onCategorySelected("全部")
                }, compact = true)
            }
            when (val state = categories) {
                is Resource.Success -> {
                    if (state.data.isEmpty()) item { DiscoverEmpty(false, onRetry) }
                    state.data.groupBy { it.group }.forEach { (group, values) ->
                        item(key = group) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(group, style = MaterialTheme.typography.titleMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                    values.forEach { category ->
                                        MeiFilterPill(category.name, selectedCategory == category.name, {
                                            onDismiss()
                                            onCategorySelected(category.name)
                                        }, compact = true)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> item { DiscoverResource(state, onRetry) { } }
            }
        }
    }

}

@Composable
internal fun DiscoverSectionHeader(title: String, action: String = "查看全部", onAction: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MeiSectionTitle(title, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
internal fun <T> DiscoverSection(
    title: String, state: Resource<List<T>>, onRetry: () -> Unit, onMore: () -> Unit,
    horizontalPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (List<T>) -> Unit,
) {
    if (state is Resource.Success && state.data.isEmpty()) return
    Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DiscoverSectionHeader(title, onAction = onMore, modifier = Modifier.padding(horizontalPadding))
        DiscoverResource(state, onRetry, content)
    }
}

@Composable
internal fun <T> DiscoverResource(
    state: Resource<List<T>>, onRetry: () -> Unit, content: @Composable (List<T>) -> Unit,
) {
    when (state) {
        is Resource.Success -> if (state.data.isEmpty()) DiscoverEmpty(false, onRetry) else content(state.data)
        is Resource.Error -> DiscoverEmpty(true, onRetry)
        Resource.Loading -> MeiCollectionStatus(loading = true, failed = false, onRetry = onRetry)
    }
}

@Composable
internal fun DiscoverEmpty(failed: Boolean, onRetry: () -> Unit) {
    MeiCollectionStatus(loading = false, failed = failed, onRetry = onRetry)
}

@Composable
internal fun DiscoverPlaylistCard(playlist: DiscoverPlaylist, modifier: Modifier, onClick: () -> Unit) {
    PlaylistCard(id = playlist.id.toString(), title = playlist.name, coverImg = playlist.cover,
        extInfo = formatDiscoverPlayCount(playlist.playCount), modifier = modifier, onClick = onClick)
}

@Composable
internal fun DiscoverAlbumCard(album: DiscoverAlbum, modifier: Modifier, onClick: () -> Unit) {
    ArtworkCard(title = album.name, subtitle = album.artist, coverUrl = album.cover, modifier = modifier, onClick = onClick)
}

@Composable
internal fun DiscoverChartRow(chart: DiscoverChart, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AsyncImage(model = chart.cover.sizedNeteaseImage(200), contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(chart.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (chart.updateFrequency.isNotBlank()) Text(chart.updateFrequency, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            chart.tracks.forEachIndexed { index, track ->
                Text("${index + 1}  $track", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

internal fun formatDiscoverPlayCount(count: Long): String = when {
    count >= 100_000_000 -> String.format(Locale.getDefault(), "%.1f亿", count / 100_000_000.0)
    count >= 10_000 -> String.format(Locale.getDefault(), "%.1f万", count / 10_000.0)
    else -> count.coerceAtLeast(0).toString()
}
