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
import com.ljyh.mei.ui.component.home.PlaylistCard
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.main.library.LibraryProfileUi
import com.ljyh.mei.ui.screen.main.library.LibrarySection
import com.ljyh.mei.ui.screen.main.library.LibraryEvent
import com.ljyh.mei.ui.screen.main.library.LibraryContentUiState

@Composable
fun LibraryTabletLayout(
    state: LibraryContentUiState,
    onEvent: (LibraryEvent) -> Unit,
) {
    val displayItems = when (state.section) {
        LibrarySection.Created -> state.createdPlaylists.map { LibraryAsset(it.id, it.title, it.cover) }
        LibrarySection.Collected -> state.collectedPlaylists.map { LibraryAsset(it.id, it.title, it.cover) }
        LibrarySection.Albums -> state.albums.map { LibraryAsset(it.id.toString(), it.title, it.cover) }
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
                profile = state.profile,
                onEvent = onEvent,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LibrarySectionHeader(state, onEvent)
        }
        if (displayItems.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { LibraryTabletEmptyState(state.section) }
        else items(displayItems, key = { it.id }) { item ->
            PlaylistCard(
                id = item.id,
                title = item.title,
                coverImg = item.cover,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onEvent(
                        if (state.section == LibrarySection.Albums) LibraryEvent.OpenAlbum(item.id)
                        else LibraryEvent.OpenPlaylist(item.id)
                    )
                },
            )
        }
    }
}

@Composable
private fun LibraryProfileOverview(
    profile: LibraryProfileUi,
    onEvent: (LibraryEvent) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = "更换头像背景",
                modifier = Modifier
                    .size(156.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .clickable { onEvent(LibraryEvent.ChangeProfilePhoto) },
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(profile.nickname.ifBlank { "Music Lover" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (!profile.membershipIconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.membershipIconUrl,
                            contentDescription = profile.membershipLabel,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .width(72.dp)
                                .height(26.dp),
                        )
                    } else if (profile.membershipLabel != null) {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(profile.membershipLabel, Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    profile.signature.ifBlank { "还没有填写个人签名" },
                    Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(Modifier.padding(top = 15.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Text("${profile.follows ?: "—"} 关注", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("${profile.followers ?: "—"} 粉丝", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Lv.${profile.level ?: "—"} 等级", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("${profile.listenSongs ?: "—"} 首听歌", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibraryQuickAction(Icons.Rounded.History, "最近", { onEvent(LibraryEvent.OpenHistory) }, Modifier.weight(1f))
                    LibraryQuickAction(Icons.Rounded.Folder, "本地", { onEvent(LibraryEvent.OpenLocalMusic) }, Modifier.weight(1f))
                    LibraryQuickAction(Icons.Rounded.Download, "下载", { onEvent(LibraryEvent.OpenDownloads) }, Modifier.weight(1f))
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
private fun LibrarySectionHeader(state: LibraryContentUiState, onEvent: (LibraryEvent) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "我的音乐",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(24.dp))
        listOf(
            Triple(LibrarySection.Created, "创建", state.createdCount),
            Triple(LibrarySection.Collected, "收藏", state.collectedCount),
            Triple(LibrarySection.Albums, "专辑", state.albumCount),
        ).forEach { (section, title, count) ->
            val selected = state.section == section
            Text("$title $count", Modifier.clip(RoundedCornerShape(14.dp)).clickable { onEvent(LibraryEvent.SelectSection(section)) }.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class LibraryAsset(val id: String, val title: String, val cover: String)

@Composable private fun LibraryTabletEmptyState(section: LibrarySection) {
    val label = when (section) {
        LibrarySection.Created -> "暂无创建歌单"
        LibrarySection.Collected -> "暂无收藏歌单"
        LibrarySection.Albums -> "暂无收藏专辑"
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 72.dp), contentAlignment = Alignment.Center) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}
