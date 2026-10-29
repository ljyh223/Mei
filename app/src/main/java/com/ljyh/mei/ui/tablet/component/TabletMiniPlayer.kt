package com.ljyh.mei.ui.tablet.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.player.rememberPlayerDisplayTitle
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.component.glass.meiGlassContentColor

/** Tablet playback slot has its own geometry; no phone gutters or compact-navigation progress. */
@Composable
internal fun TabletMiniPlayer(
    metadata: MediaMetadata?,
    playing: Boolean,
    canSkipNext: Boolean,
    enabled: Boolean,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val title = rememberPlayerDisplayTitle(metadata?.title.orEmpty())
    MeiGlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        solidColor = colors.secondaryContainer,
    ) {
        Row(
            Modifier.fillMaxWidth().fillMaxHeight()
                .clickable(enabled = enabled && metadata != null, role = Role.Button, onClick = onOpen)
                .padding(start = 8.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (metadata?.coverUrl?.isNotBlank() == true) AsyncImage(
                    model = metadata.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp),
                ) else Icon(Icons.Rounded.MusicNote, null, Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = if (metadata == null) "选择歌曲开始播放" else title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metadata != null && LocalDensity.current.fontScale <= 1.5f) Text(
                    text = metadata.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = meiGlassContentColor(secondary = true),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onPlayPause, enabled = enabled && metadata != null, modifier = Modifier.size(48.dp)) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "暂停" else "播放", modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onNext, enabled = enabled && canSkipNext, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.SkipNext, "下一首", Modifier.size(24.dp))
            }
        }
    }
}
