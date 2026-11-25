package com.ljyh.mei.ui.screen.artist.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.screen.artist.artistReleaseDate

/** Borderless release content: square cover beside its title and release metadata. */
@Composable
internal fun ArtistLatestReleaseCard(
    album: Album?,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val fixedHeight = constraints.minHeight == constraints.maxHeight
        val imageSize = if (fixedHeight) maxHeight else minOf((maxWidth - 16.dp) * .46f, 144.dp)
        val compactText = fixedHeight && maxHeight < 112.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val cardModifier = if (fixedHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
        Box(cardModifier.clickable(enabled = !loading && album != null, onClick = onClick)) {
            Row(Modifier.fillMaxWidth().then(if (fixedHeight) Modifier.fillMaxHeight() else Modifier.heightIn(min = 148.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(imageSize).clip(RoundedCornerShape(8.dp))
                    .skeleton(loading)
                    .semantics { contentDescription = "最新发行封面" },
                    contentAlignment = Alignment.Center) {
                    if (!loading) {
                        if (album?.cover.isNullOrBlank()) Icon(Icons.Rounded.MusicNote,
                            contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        else AsyncImage(album.cover, contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (compactText) 4.dp else 8.dp)) {
                    Text(if (loading) "发行日期" else album?.let { artistReleaseDate(it, fullDate = true) } ?: "发行日期未知",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().skeleton(loading))
                    Text(if (loading) "正在加载的标题" else album?.title.orEmpty(),
                        style = if (compactText) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().skeleton(loading))
                    Text(if (loading) "歌曲数量" else "${album?.size ?: 0} 首歌曲",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().skeleton(loading))
                }
            }
        }
    }
}
