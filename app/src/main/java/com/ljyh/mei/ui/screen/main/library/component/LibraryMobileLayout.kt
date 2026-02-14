package com.ljyh.mei.ui.screen.main.library.component

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.toLong
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.item.AlbumItem
import com.ljyh.mei.ui.component.item.PlaylistItem
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.model.toAlbum
import com.ljyh.mei.ui.model.toAlbumEntity
import com.ljyh.mei.ui.screen.Screen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMobileLayout(
    userNickname: String,
    userAvatarUrl: String,
    userPhoto: String,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    tabTitles: List<String>,
    createdPlaylists: List<Playlist>,
    collectedPlaylists: List<Playlist>,
    albums: List<Album>,
    onAvatarClick: () -> Unit,
    onPlaylistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 80.dp
        )
    ) {
        item {
            BigUserHeader(userNickname, userAvatarUrl, onAvatarClick)
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            BentoDashboard(
                lastPlayedCover = userPhoto,
                onHistoryClick = { Screen.History.navigate(navController)},
                onLocalClick = {
                    Toast.makeText(context, "尽请期待", Toast.LENGTH_SHORT).show()
                },
                onCloudClick = {
                    Toast.makeText(context, "尽请期待", Toast.LENGTH_SHORT).show()
                },
                onDownloadClick = {
                    Toast.makeText(context, "尽请期待", Toast.LENGTH_SHORT).show()
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        stickyHeader {
            // 之前的吸顶 Tab 逻辑
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(50),
                    tonalElevation = 2.dp
                ) {
                    ModernCapsuleTabs(tabTitles, selectedTabIndex, onTabSelect)
                }
            }
        }

        when (selectedTabIndex) {
            0 -> { // 创建歌单
                if (createdPlaylists.isEmpty()) item { EmptyState("暂无创建歌单") }
                else items(createdPlaylists, key = { it.id }) { playlist ->
                    PlaylistItem(playlist) {
                        Screen.PlayList.navigate(navController) { addPath(playlist.id) }
                    }
                }
            }

            1 -> { // 收藏歌单
                if (collectedPlaylists.isEmpty()) item { EmptyState("暂无收藏歌单") }
                else items(collectedPlaylists, key = { it.id }) { playlist ->
                    PlaylistItem(playlist) {
                        onPlaylistClick(playlist.id)

                    }
                }
            }

            2 -> { // 收藏专辑
//                val albums = (albumList as? Resource.Success)?.data?.data ?: emptyList()

                if (albums.isEmpty()) item { EmptyState("暂无收藏专辑") }
                else{
//                    albums.map { al->
//                        al.toAlbumEntity().let {
//                            viewModel.insertAlbum(it.first, it.second)
//                        }
//                    }
                    items(albums, key = { it.id }) { album ->
                        AlbumItem(album) {
                            onAlbumClick(album.id.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@Composable
fun BigUserHeader(nickname: String, avatarUrl: String, onAvatarClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // 大圆角头像
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .size(80.dp)
                .clickable { onAvatarClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(
                text = "My Library",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Text(
                text = nickname.ifEmpty { "Music Lover" },
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}