package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.Album

@Composable
fun LibraryTabletLayout(
    userNickname: String,
    userAvatarUrl: String,
    signature: String,
    createdCount: Int,
    collectedCount: Int,
    albumCount: Int,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    onAvatarClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLocalClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onPlaylistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    createdPlaylists: List<Playlist>,
    collectedPlaylists: List<Playlist>,
    albums: List<Album>,
) {
    val displayItems = when (selectedTabIndex) {
        0 -> createdPlaylists.map { LibraryAsset(it.id, it.title, it.cover, "${it.count} 首 · ${it.authorName}") }
        1 -> collectedPlaylists.map { LibraryAsset(it.id, it.title, it.cover, "${it.count} 首 · ${it.authorName}") }
        else -> albums.map { LibraryAsset(it.id.toString(), it.title, it.cover, it.artist.joinToString { artist -> artist.name }) }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 144.dp), modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 28.dp,
            top = 28.dp,
            end = 28.dp,
            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 28.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LibraryProfileOverview(userNickname, userAvatarUrl, signature, createdCount, collectedCount, albumCount, onAvatarClick, onHistoryClick, onLocalClick, onDownloadClick)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LibrarySectionHeader(selectedTabIndex, onTabSelect, createdCount, collectedCount, albumCount)
        }
        if (displayItems.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { LibraryTabletEmptyState(selectedTabIndex) }
        else items(displayItems, key = { it.id }) { item ->
            LibraryAssetCard(item) { if (selectedTabIndex == 2) onAlbumClick(item.id) else onPlaylistClick(item.id) }
        }
    }
}

@Composable
private fun LibraryProfileOverview(
    userNickname: String, userAvatarUrl: String, signature: String, createdCount: Int, collectedCount: Int,
    albumCount: Int, onAvatarClick: () -> Unit, onHistoryClick: () -> Unit, onLocalClick: () -> Unit, onDownloadClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = userAvatarUrl, contentDescription = "更换头像背景", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(92.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onAvatarClick))
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(userNickname.ifBlank { "Music Lover" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (signature.isNotBlank()) Text(signature, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        ProfileStat(createdCount, "创建歌单"); ProfileStat(collectedCount, "收藏歌单"); ProfileStat(albumCount, "专辑")
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibraryQuickAction(Icons.Rounded.History, "最近", onHistoryClick, Modifier.weight(1f))
                LibraryQuickAction(Icons.Rounded.Folder, "本地", onLocalClick, Modifier.weight(1f))
                LibraryQuickAction(Icons.Rounded.Download, "下载", onDownloadClick, Modifier.weight(1f))
                LibraryQuickAction(Icons.Rounded.Cloud, "云盘", {}, Modifier.weight(1f), enabled = false)
            }
        }
    }
}

@Composable private fun ProfileStat(value: Int, label: String) = Column {
    Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LibraryQuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true) {
    Surface(modifier = modifier.height(58.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, enabled = enabled, onClick = onClick) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun LibrarySectionHeader(selectedTabIndex: Int, onTabSelect: (Int) -> Unit, createdCount: Int, collectedCount: Int, albumCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("我的音乐", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.width(24.dp))
        listOf("创建" to createdCount, "收藏" to collectedCount, "专辑" to albumCount).forEachIndexed { index, (title, count) ->
            Text("$title $count", Modifier.clip(RoundedCornerShape(14.dp)).clickable { onTabSelect(index) }.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class LibraryAsset(val id: String, val title: String, val cover: String, val subtitle: String)

@Composable private fun LibraryAssetCard(item: LibraryAsset, onClick: () -> Unit) = Column(Modifier.clickable(onClick = onClick)) {
    AsyncImage(item.cover, item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)))
    Text(item.title, Modifier.padding(top = 9.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(item.subtitle, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable private fun LibraryTabletEmptyState(selectedTabIndex: Int) {
    val label = when (selectedTabIndex) { 0 -> "暂无创建歌单"; 1 -> "暂无收藏歌单"; else -> "暂无收藏专辑" }
    Box(Modifier.fillMaxWidth().padding(vertical = 72.dp), contentAlignment = Alignment.Center) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}
