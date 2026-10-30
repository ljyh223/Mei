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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.PlaylistCardSize
import com.ljyh.mei.ui.component.shimmer.skeleton

/** Shared card text and interaction. A specialized artwork slot may own its image geometry. */
@Composable
fun ArtworkCard(
    title: String,
    coverUrl: String?,
    modifier: Modifier = Modifier.width(PlaylistCardSize),
    subtitle: String? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    cornerRadius: Dp = 16.dp,
    textSpacing: Dp = 8.dp,
    maxTitleLine: Int = 1,
    titleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodySmall,
    reserveSubtitleSpace: Boolean = false,
    placeholder: @Composable BoxScope.() -> Unit = {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    },
    coverContent: @Composable BoxScope.() -> Unit = {},
    artworkContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Column(modifier.clickable(enabled = enabled && !isLoading, onClick = onClick)) {
        if (artworkContent != null) artworkContent() else Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(cornerRadius))
                .skeleton(isLoading, RoundedCornerShape(cornerRadius))
        ) {
            if (!isLoading) {
                if (coverUrl.isNullOrBlank()) placeholder()
                else AsyncImage(
                    model = coverUrl,
                    modifier = Modifier.matchParentSize(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
                coverContent()
            }
        }
        Spacer(Modifier.height(textSpacing))
        StableArtworkText(
            text = if (isLoading) "正在加载的标题" else title,
            modifier = Modifier
                .fillMaxWidth()
                .skeleton(isLoading),
            style = titleStyle,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (reserveSubtitleSpace || !subtitle.isNullOrBlank()) StableArtworkText(
            text = subtitle.orEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .skeleton(isLoading),
            style = subtitleStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A content-independent line box prevents lazy shelves resizing when font fallback changes. */
@Composable
internal fun StableArtworkText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Start,
) {
    val fontSize = style.fontSize.takeIf { it.isSp }?.value ?: 14f
    val lineHeight = style.lineHeight.takeIf { it.isSp }?.value ?: 0f
    val height = with(LocalDensity.current) { maxOf(lineHeight, fontSize * 1.4f).sp.toDp() }
    Box(modifier.height(height), contentAlignment = Alignment.CenterStart) {
        Text(
            text = text, style = style, color = color, fontWeight = fontWeight, textAlign = textAlign,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(),
        )
    }
}
