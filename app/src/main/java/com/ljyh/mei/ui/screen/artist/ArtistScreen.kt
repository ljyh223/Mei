package com.ljyh.mei.ui.screen.artist

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.item.AlbumItem
import com.ljyh.mei.ui.component.item.Track
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.screen.Screen

@OptIn(UnstableApi::class)
@Composable
fun ArtistScreen(
    id: String,
    viewModel: ArtistViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val artistDetail by viewModel.artistDetail.collectAsState()
    val artistAlbums by viewModel.artistAlbums.collectAsState()
    val artistSongs by viewModel.artistSongs.collectAsState()

    LaunchedEffect(id) {
        viewModel.getArtistDetail(id)
        viewModel.getArtistAlbums(id)
        viewModel.getArtistSongs(id)
    }

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Section
            item {
                if (artistDetail is Resource.Success) {
                    val detail = (artistDetail as Resource.Success).data.data
                    ArtistHeader(
                        artist = detail.artist,
                        user = detail.user,
                        videoCount = detail.videoCount,
                        eventCount = detail.eventCount,
                        onBack = { navController.popBackStack() }
                    )
                } else if (artistDetail is Resource.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }else if(artistDetail is Resource.Error){
                    Text(
                        text = (artistDetail as Resource.Error).message,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Hot Songs Section
            item {
                Text(
                    text = "Hot Songs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (artistSongs is Resource.Success) {
                val songs = (artistSongs as Resource.Success).data.hotSongs
                items(songs.take(10)) { song ->
                    val mediaMetadata = song.toMediaMetadata()
                    Track(
                        track = mediaMetadata,
                        onClick = {
                            val allIds = songs.map { it.id.toString() to it.toMediaMetadata().toMediaItem() }
                            val currentIndex = songs.indexOf(song)
                             playerConnection.playQueue(
                                ListQueue(
                                    id = "artist_song_${id}",
                                    title = "Hot Songs",
                                    items = allIds,
                                    startIndex = currentIndex
                                )
                            )
                        },
                        onMoreClick =  {

                        }
                    )
                }
            } else if (artistSongs is Resource.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }else if (artistSongs is Resource.Error) {
                item {
                    Text(
                        text = (artistSongs as Resource.Error).message,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Albums Section
            item {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (artistAlbums is Resource.Success) {
                val albums = (artistAlbums as Resource.Success).data.hotAlbums
                items(albums) { hotAlbum ->
                    val uiAlbum = Album(
                        id = hotAlbum.id.toLong(),
                        title = hotAlbum.name,
                        cover = hotAlbum.picUrl,
                        artist = hotAlbum.artists.map { Album.Artist(it.id.toLong(), it.name) },
                        size = hotAlbum.size
                    )
                    
                    AlbumItem(
                        album = uiAlbum,
                        onClick = {
                             navController.navigate("${Screen.Album.route}/${it}")
                        }
                    )
                }
            } else if (artistAlbums is Resource.Loading) {
                item {
                     Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }else if (artistAlbums is Resource.Error) {
                item {
                    Text(
                        text = (artistAlbums as Resource.Error).message,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
             item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun ArtistHeader(
    artist: ArtistDetail.Data.Artist,
    user: ArtistDetail.Data.User?,
    videoCount: Int,
    eventCount: Int,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // Background Image
        AsyncImage(
            model = artist.cover,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                 AsyncImage(
                    model = artist.avatar, // Use avatar if available, else cover
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                     Text(
                        text = artist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                     Text(
                        text = "${artist.musicSize} Songs · ${artist.albumSize} Albums · ${artist.mvSize} MVs",
                        style = MaterialTheme.typography.bodyMedium,
                         color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }
           
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = artist.briefDesc,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 40.dp, start = 8.dp)
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}
