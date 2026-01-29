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
import com.ljyh.mei.ui.component.player.component.sheet.ArtistSelectionSheet

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

    val shadowStyle = Shadow(
        color = Color.Black.copy(alpha = 0.5f),
        offset = Offset(2f, 2f),
        blurRadius = 8f
    )

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.statusBarsPadding().fillMaxWidth()
    ) {
        // --- 标题部分 ---
        Text(
            text = mediaMetadata.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                shadow = shadowStyle,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            color = Color.White,
            overflow = TextOverflow.Ellipsis,
        )

        // --- 副标题部分 (歌手 & 专辑) ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .basicMarquee(iterations = Int.MAX_VALUE)
        ) {
            val subTitleStyle = MaterialTheme.typography.titleMedium.copy(
                shadow = shadowStyle,
                color = Color.White.copy(alpha = 0.7f)
            )

            if (mediaMetadata.artists.isNotEmpty()) {
                Text(
                    text = mediaMetadata.artists.joinToString(", ") { it.name },
                    style = subTitleStyle,
                    maxLines = 1, // 必须设为1
                    softWrap = false, // 禁止换行，确保 Row 宽度可以溢出
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            if (mediaMetadata.artists.size == 1) {
                                onNavigateToArtist(mediaMetadata.artists[0])
                            } else {
                                showArtistSheet = true
                            }
                        }
                )
            }

            // 2. 分隔符 (只有两者都有值时才显示)
            if (mediaMetadata.artists.isNotEmpty() && mediaMetadata.album.title.isNotEmpty()) {
                Text(
                    text = " - ",
                    style = subTitleStyle,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
            // 3. 专辑部分
            if (mediaMetadata.album.title.isNotEmpty()) {
                Text(
                    text = mediaMetadata.album.title,
                    style = subTitleStyle,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onNavigateToAlbum(mediaMetadata.album) }
                )
            }
        }
    }
}