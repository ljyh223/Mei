package com.ljyh.mei.ui.component.card

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.PlaylistCardSize
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.utils.image.sizedNeteaseImage

/** One artwork/text layout for playlists, remote albums and local albums. Only the image is clipped. */
@Composable
fun ArtworkCard(
    title: String,
    coverUrl: String?,
    modifier: Modifier = Modifier.width(PlaylistCardSize),
    subtitle: String? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    resizeImage: Boolean = true,
    cornerRadius: Dp = 16.dp,
    textSpacing: Dp = 8.dp,
    titleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodySmall,
    titleMaxLines: Int = 2,
    placeholder: @Composable BoxScope.() -> Unit = {
        Icon(Icons.Rounded.MusicNote, contentDescription = null,
            modifier = Modifier.align(Alignment.Center).size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    },
    coverContent: @Composable BoxScope.() -> Unit = {},
    onClick: () -> Unit,
) {
    Column(modifier.clickable(enabled = enabled && !isLoading, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(cornerRadius))
            .skeleton(isLoading, RoundedCornerShape(cornerRadius))) {
            if (!isLoading) {
                if (coverUrl.isNullOrBlank()) placeholder()
                else AsyncImage(
                    model = if (resizeImage) coverUrl.sizedNeteaseImage(500) else coverUrl,
                    modifier = Modifier.matchParentSize(), contentDescription = null, contentScale = ContentScale.Crop,
                )
                coverContent()
            }
        }
        Spacer(Modifier.height(textSpacing))
        Text(
            text = if (isLoading) "正在加载的标题\n标题" else title,
            modifier = Modifier.fillMaxWidth().skeleton(isLoading),
            style = titleStyle, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface,
            maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) Text(
            text = subtitle, modifier = Modifier.fillMaxWidth().padding(top = 2.dp).skeleton(isLoading),
            style = subtitleStyle, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
