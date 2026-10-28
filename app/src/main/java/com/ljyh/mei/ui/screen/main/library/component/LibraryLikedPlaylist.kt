package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.room.Playlist

/** Artwork comes from the first track, never from the heart-branded playlist cover. */
@Composable
internal fun LibraryLikedPlaylist(
    playlist: Playlist?,
    artwork: String?,
    preparingPlayback: Boolean,
    canPlay: Boolean,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    songTitle: String? = null,
    playAtBottom: Boolean = false,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .clickable(
                    enabled = playlist != null, role = Role.Button,
                    onClickLabel = "打开我喜欢的音乐", onClick = onOpen
                )
        ) {
            Icon(
                Icons.Rounded.MusicNote, null, Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!artwork.isNullOrBlank()) AsyncImage(
                model = artwork, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.08f),
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.78f),
                            )
                        )
                    )
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = 16.dp,
                        bottom = 16.dp,
                        top = 16.dp,
                        end = if (playAtBottom) 76.dp else 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "我喜欢的音乐",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = if (playAtBottom) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (playlist != null) Text(
                    text = buildString {
                        append("${playlist.count} 首")
                        if (playAtBottom && !songTitle.isNullOrBlank()) append(" · 最近喜欢《$songTitle》")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        FilledIconButton(
            onClick = onPlay,
            enabled = playlist != null && canPlay && !preparingPlayback && playlist.count > 0,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .align(if (playAtBottom) Alignment.BottomEnd else Alignment.TopEnd)
                .padding(12.dp)
                .size(48.dp)
                .semantics {
                    contentDescription =
                        if (preparingPlayback) "正在准备播放我喜欢的音乐" else "播放我喜欢的音乐"
                },
        ) {
            if (preparingPlayback) CircularProgressIndicator(
                Modifier.size(22.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else Icon(Icons.Rounded.PlayArrow, null, Modifier.size(28.dp))
        }
    }
}
