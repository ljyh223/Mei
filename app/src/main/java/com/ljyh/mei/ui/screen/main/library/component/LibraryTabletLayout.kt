package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.screen.Screen


@Composable
fun LibraryTabletLayout(
    userNickname: String,
    userAvatarUrl: String,
    userPhoto: String,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    onAvatarClick: () -> Unit,
    onPlaylistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    createdPlaylists: List<Playlist>,
    collectedPlaylists: List<Playlist>,
    albums: List<Album>,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp) // 全局留白，让界面“浮”起来
    ) {
        // --- 1. 左侧沉浸式导航区 (25%) ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 24.dp)
        ) {
            // 用户信息：不再是简单的 Row，而是大气的纵向组合
            LibraryHeroHeader(userNickname, userAvatarUrl, onAvatarClick)

            Spacer(modifier = Modifier.height(48.dp))

            // 垂直导航菜单：代替原来的 Tab
            val menuItems = listOf("我创建的", "我收藏的", "专辑")
            menuItems.forEachIndexed { index, title ->
                VerticalNavItem(
                    title = title,
                    isSelected = selectedTabIndex == index,
                    onClick = { onTabSelect(index) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 将 Bento 的功能按钮化，放在底部作为快捷工具栏
            QuickActionGrid()
        }

        // --- 2. 右侧浮动舞台区 (75%) ---
        Surface(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), // 磨砂玻璃质感
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.blur(0.dp)) { // 内部内容保持清晰
                LibraryContentStage(
                    selectedTabIndex = selectedTabIndex,
                    createdPlaylists = createdPlaylists,
                    collectedPlaylists = collectedPlaylists,
                    albums = albums,
                    heroImage = userPhoto,
                    onPlaylistClick = onPlaylistClick,
                    onAlbumClick = onAlbumClick
                )
            }
        }
    }
}


@Composable
fun VerticalNavItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 选中指示器
        AnimatedVisibility(visible = isSelected) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }

        Text(
            text = title,
            modifier = Modifier.padding(start = if (isSelected) 16.dp else 20.dp),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                fontSize = 22.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(
                alpha = 0.5f
            )
        )
    }
}

@Composable
fun LibraryContentStage(
    selectedTabIndex: Int,
    createdPlaylists: List<Playlist>,
    collectedPlaylists: List<Playlist>,
    albums: List<Album>,
    heroImage: String,
    onPlaylistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4), // 固定 4 列，方便控制 Span
        contentPadding = PaddingValues(
            top = 32.dp,
            start = 24.dp,
            end = 24.dp,
            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                .calculateBottomPadding() + 80.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // 1. Hero Card: 占据 4 列，跨度最大，视觉冲击力强
        item(span = { GridItemSpan(4) }) {
            LibraryHeroCard(heroImage)
        }

        // 2. 列表内容
        val displayList = when (selectedTabIndex) {
            0 -> createdPlaylists.map { PlaylistCard(it.id, it.title, it.cover, it.authorName) }
            1 -> collectedPlaylists.map { PlaylistCard(it.id, it.title, it.cover, it.authorName) }
            else -> albums.map { PlaylistCard(it.id.toString(), it.title, it.cover, "${it.artist.size}首") }
        }

        items(displayList) { item ->
            AlbumArtCard(
                item = item,
                onClick = { id ->
                    if (selectedTabIndex == 2) {
                        onAlbumClick(id)
                    } else {
                        onPlaylistClick(id)
                    }
                })
        }
    }
}

data class PlaylistCard(
    val id: String,
    val title: String,
    val cover: String,
    val subtitle: String
)

@Composable
fun AlbumArtCard(item: PlaylistCard, onClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .aspectRatio(1f)
                .clickable(onClick = { onClick(item.id) })
                .graphicsLayer { // 增加轻微的阴影偏移
                    shadowElevation = 8f
                    shape = RoundedCornerShape(20.dp)
                    clip = true
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            AsyncImage(
                model = item.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun LibraryHeroHeader(nickname: String, avatarUrl: String, onAvatarClick: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // 大尺寸、特殊形状的头像
        Card(
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .size(100.dp)
                .clickable(onClick = onAvatarClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 杂志样式的文字排版
        Text(
            text = "HELLO,",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 4.sp
        )
        Text(
            text = nickname.ifEmpty { "Music Lover" },
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                lineHeight = 44.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun QuickActionGrid() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val actions = listOf(
            Icons.Rounded.Folder to "Local",
            Icons.Rounded.Cloud to "Cloud",
            Icons.Rounded.Download to "Down"
        )

        actions.forEach { (icon, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    onClick = { /* TODO */ },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun LibraryHeroCard(cover: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp), // 宽屏比例
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 沉浸式大图
            AsyncImage(
                model = cover,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 艺术化的渐变，从左下角向右上角扩散
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent
                            ),
                            start = androidx.compose.ui.geometry.Offset(
                                0f,
                                Float.POSITIVE_INFINITY
                            ),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                    )
            )

            // 卡片上的文字信息
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(32.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "RECENTLY PLAYED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "继续播放你最爱的歌单",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            // 浮动的播放按钮
            LargeFloatingPlayButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )
        }
    }
}

@Composable
fun LargeFloatingPlayButton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(64.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}