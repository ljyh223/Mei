package com.ljyh.mei.ui.screen.local

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.playlist.FinalPerfectCollage
import com.ljyh.mei.ui.component.playlist.PlaylistBackground
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.screen.playlist.component.PlaylistTrackList
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalSongListScreen(
    filterType: String,
    filterValue: String,
    title: String,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val db = AppDatabase.getDatabase(context)

    val songs by when (filterType) {
        "artist" -> db.songDao().getLocalSongsByArtistContains(filterValue)
        "album" -> db.songDao().getLocalSongsByAlbum(filterValue)
        "folder" -> db.songDao().getSongsByFolder(filterValue)
        else -> db.songDao().getLocalSongs()
    }.collectAsState(initial = emptyList())

    val tracks: List<MediaMetadata> = songs.map { it.toMediaMetadata() }
    val coverUrl = songs.firstOrNull { it.cover.isNotEmpty() }?.cover

    val lazyListState = rememberLazyListState()
    val device = rememberDeviceInfo()
    val showTopBarTitle by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }

    fun buildQueue(index: Int = 0): ListQueue {
        val items = tracks.map { it.id.toString() to it.toMediaItem() }
        return ListQueue(
            id = "local_$filterType+$filterValue",
            title = title,
            items = items,
            startIndex = index
        )
    }

    Box(Modifier.fillMaxSize()) {
        if (coverUrl != null) {
            PlaylistBackground(coverUrl = coverUrl)
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        AnimatedVisibility(
                            visible = showTopBarTitle,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    navigationIcon = {
                        com.ljyh.mei.ui.component.IconButton(
                            onClick = navController::navigateUp,
                            onLongClick = navController::backToMain
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    )
                )
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (device.isTablet && device.isLandscape) {
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(0.4f).align(Alignment.CenterVertically)) {
                            LocalSongListHeader(
                                songs = songs,
                                title = title,
                                filterType = filterType,
                                onPlayAll = { playerConnection.playQueue(buildQueue(0)) }
                            )
                        }
                        PlaylistTrackList(
                            modifier = Modifier.weight(0.6f),
                            pagingItems = null,
                            staticTracks = tracks,
                            isTablet = true,
                            onTrackClick = { track, index ->
                                val isLost = songs.find { it.id == track.id.toString() }?.let {
                                    it.path == null || !File(it.path).exists()
                                } ?: false
                                if (!isLost) {
                                    playerConnection.onTrackClicked(
                                        trackId = track.id.toString(),
                                        buildQueue = { buildQueue(index) }
                                    )
                                }
                            },
                            onMoreClick = {}
                        )
                    }
                } else {
                    PlaylistTrackList(
                        pagingItems = null,
                        staticTracks = tracks,
                        lazyListState = lazyListState,
                        headerContent = {
                            LocalSongListHeader(
                                songs = songs,
                                title = title,
                                filterType = filterType,
                                onPlayAll = { playerConnection.playQueue(buildQueue(0)) }
                            )
                        },
                        onTrackClick = { track, index ->
                            val isLost = songs.find { it.id == track.id.toString() }?.let {
                                it.path == null || !File(it.path).exists()
                            } ?: false
                            if (!isLost) {
                                playerConnection.onTrackClicked(
                                    trackId = track.id.toString(),
                                    buildQueue = { buildQueue(index) }
                                )
                            }
                        },
                        onMoreClick = {},
                        contentPadding = PaddingValues(
                            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                                .calculateBottomPadding()
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalSongListHeader(
    songs: List<Song>,
    title: String,
    filterType: String,
    onPlayAll: () -> Unit
) {
    val cover = songs.firstOrNull { it.cover.isNotEmpty() }?.cover
    val coverList = songs.filter { it.cover.isNotEmpty() }.map { it.cover }.take(9)
    val totalDuration = songs.sumOf { it.duration }
    val durationText = formatDuration(totalDuration)

    val placeholderIcon: ImageVector = when (filterType) {
        "artist" -> Icons.Rounded.Person
        "album" -> Icons.Rounded.Album
        "folder" -> Icons.Rounded.Folder
        else -> Icons.Rounded.MusicNote
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.size(220.dp)
        ) {
            if (coverList.size >= 5) {
                FinalPerfectCollage(imageUrls = coverList, modifier = Modifier.fillMaxSize())
            } else if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        placeholderIcon,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape
            ) {
                Text(
                    text = "${songs.size} 首",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = durationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (filterType == "album") {
            val artists = songs.flatMap { splitArtistString(it.artist) }.distinct()
            if (artists.isNotEmpty()) {
                Text(
                    text = if (artists.size <= 2) artists.joinToString(" / ")
                    else "${artists.first()} 等 ${artists.size} 位艺术家",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onPlayAll,
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("播放全部")
        }

        Spacer(Modifier.height(24.dp))
    }
}

private val SEPARATORS = Regex("[、/;|&]")

private fun splitArtistString(artist: String): List<String> {
    if (artist.isBlank()) return emptyList()
    return artist.split(SEPARATORS).map { it.trim() }.filter { it.isNotEmpty() }
        .ifEmpty { listOf(artist.trim()) }
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0 分钟"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours} 小时 ${minutes} 分钟" else "${minutes} 分钟"
}
