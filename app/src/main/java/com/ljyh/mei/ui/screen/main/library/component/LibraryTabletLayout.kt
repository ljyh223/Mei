package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.ljyh.mei.ui.component.home.PlaylistCard
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.Album

@Composable
fun LibraryTabletLayout(
    userNickname: String,
    userAvatarUrl: String,
    signature: String,
    membershipLabel: String?,
    membershipIconUrl: String?,
    follows: Int,
    followers: Int,
    level: Int,
    listenSongs: Int,
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
        0 -> createdPlaylists.map { LibraryAsset(it.id, it.title, it.cover) }
        1 -> collectedPlaylists.map { LibraryAsset(it.id, it.title, it.cover) }
        else -> albums.map { LibraryAsset(it.id.toString(), it.title, it.cover) }
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
            LibraryProfileOverview(
                userNickname, userAvatarUrl, signature, membershipLabel, membershipIconUrl,
                follows, followers, level, listenSongs,
                onAvatarClick, onHistoryClick, onLocalClick, onDownloadClick,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LibrarySectionHeader(selectedTabIndex, onTabSelect, createdCount, collectedCount, albumCount)
        }
        if (displayItems.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { LibraryTabletEmptyState(selectedTabIndex) }
        else items(displayItems, key = { it.id }) { item ->
            PlaylistCard(
                id = item.id,
                title = item.title,
                coverImg = item.cover,
                cardSize = null,
                onClick = { if (selectedTabIndex == 2) onAlbumClick(item.id) else onPlaylistClick(item.id) },
            )
        }
    }
}

@Composable
private fun LibraryProfileOverview(
    userNickname: String, userAvatarUrl: String, signature: String, membershipLabel: String?, membershipIconUrl: String?,
    follows: Int, followers: Int, level: Int, listenSongs: Int,
    onAvatarClick: () -> Unit, onHistoryClick: () -> Unit, onLocalClick: () -> Unit, onDownloadClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            AsyncImage(
                model = userAvatarUrl,
                contentDescription = "更换头像背景",
                modifier = Modifier
                    .size(156.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .clickable(onClick = onAvatarClick),
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(userNickname.ifBlank { "Music Lover" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (!membershipIconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = membershipIconUrl,
                            contentDescription = membershipLabel,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .width(72.dp)
                                .height(26.dp),
                        )
                    } else if (membershipLabel != null) {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(membershipLabel, Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    signature.ifBlank { "还没有填写个人签名" },
                    Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(Modifier.padding(top = 15.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Text("$follows 关注", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("$followers 粉丝", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Lv.$level 等级", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("$listenSongs 首听歌", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibraryQuickAction(Icons.Rounded.History, "最近", onHistoryClick, Modifier.weight(1f))
                    LibraryQuickAction(Icons.Rounded.Folder, "本地", onLocalClick, Modifier.weight(1f))
                    LibraryQuickAction(Icons.Rounded.Download, "下载", onDownloadClick, Modifier.weight(1f))
                    LibraryQuickAction(Icons.Rounded.Cloud, "云盘", {}, Modifier.weight(1f), enabled = false)
                }
            }
        }
    }
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
        Text(
            text = "我的音乐",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(24.dp))
        listOf("创建" to createdCount, "收藏" to collectedCount, "专辑" to albumCount).forEachIndexed { index, (title, count) ->
            Text("$title $count", Modifier.clip(RoundedCornerShape(14.dp)).clickable { onTabSelect(index) }.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class LibraryAsset(val id: String, val title: String, val cover: String)

@Composable private fun LibraryTabletEmptyState(selectedTabIndex: Int) {
    val label = when (selectedTabIndex) { 0 -> "暂无创建歌单"; 1 -> "暂无收藏歌单"; else -> "暂无收藏专辑" }
    Box(Modifier.fillMaxWidth().padding(vertical = 72.dp), contentAlignment = Alignment.Center) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}
