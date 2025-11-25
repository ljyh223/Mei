package com.ljyh.mei.ui.component.player.component

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.MediaMetadata

@Composable
fun Header(
    modifier: Modifier,
    mediaMetadata: MediaMetadata,
    onNavigateToArtist: (MediaMetadata.Artist) -> Unit,
    onNavigateToAlbum: (MediaMetadata.Album) -> Unit
) {
    var showArtistSheet by remember { mutableStateOf(false) }

    if (showArtistSheet) {
        ArtistSelectionSheet(
            artists = mediaMetadata.artists,
            onDismiss = { showArtistSheet = false },
            onArtistClick = { artist ->
                showArtistSheet = false
                onNavigateToArtist(artist)
            }
        )
    }
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .statusBarsPadding()
    ) {
        Text(
            text = mediaMetadata.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = Offset(2f, 2f),
                    blurRadius = 6f
                )
            ),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            modifier = Modifier.basicMarquee()
        )
        val subTitleStyle = MaterialTheme.typography.titleMedium.copy(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.5f),
                offset = Offset(2f, 2f),
                blurRadius = 6f
            )
        )


        Row {
            if (mediaMetadata.artists.isNotEmpty()) {
                Text(
                    text = mediaMetadata.artists.joinToString(", ") { it.name },
                    style = subTitleStyle,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                        .clip(RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clickable(onClick = {
                            if (mediaMetadata.artists.size == 1) {
                                // 只有一个歌手，直接跳转 (假设你有这个回调)
                                //onNavigateToArtist(metadata.artists.first())
                            } else {
                                // 多个歌手，打开底部弹窗
                                showArtistSheet = true
                            }
                        })

                )
            }

            if (mediaMetadata.album.title.isNotEmpty()) {
                Text(
                    " - ",
                    style = subTitleStyle,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = mediaMetadata.album.title,
                    style = subTitleStyle,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                        .clip(RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clickable(onClick = {
                            // 跳转到专辑页面
                            onNavigateToAlbum(mediaMetadata.album)
                        })
                )
            }
        }
    }
}