package com.ljyh.mei.ui.screen.local

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.item.Track
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.utils.rememberPreference
import java.io.File
import com.ljyh.mei.constants.PlaylistTrackTableHeaderKey

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
    val playlistTrackTableHeader by rememberPreference(PlaylistTrackTableHeaderKey, false)

    val songs by when (filterType) {
        "artist" -> db.songDao().getLocalSongsByArtist(filterValue)
        "album" -> db.songDao().getLocalSongsByAlbum(filterValue)
        "folder" -> db.songDao().getSongsByFolder(filterValue)
        else -> db.songDao().getLocalSongs()
    }.collectAsState(initial = emptyList())

    val tracks: List<MediaMetadata> = songs.map { song -> song.toMediaMetadata() }

    val lostSongIds: Set<String> = remember(songs) {
        songs.filter { it.path != null && !File(it.path).exists() }.map { it.id }.toSet()
    }

    fun buildQueue(index: Int = 0): ListQueue {
        val items: List<Pair<String, androidx.media3.common.MediaItem?>> = tracks.map { track ->
            track.id.toString() to track.toMediaItem()
        }
        return ListQueue(
            id = "local_$filterType+$filterValue",
            title = title,
            items = items,
            startIndex = index
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    com.ljyh.mei.ui.component.IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null,
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { playerConnection.playQueue(buildQueue(0)) }) {
                        Icon(Icons.Rounded.PlayArrow, "播放全部",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (tracks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                    val isLost = lostSongIds.contains(track.id.toString())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLost) {
                            Icon(
                                Icons.Rounded.Warning,
                                contentDescription = "文件丢失",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp).padding(start = 8.dp)
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            Track(
                                track = track,
                                index = index + 1,
                                isTablet = false,
                                onClick = {
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
                    }
                }
            }
        }
    }
}
