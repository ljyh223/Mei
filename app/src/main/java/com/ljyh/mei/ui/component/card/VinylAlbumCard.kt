package com.ljyh.mei.ui.component.card

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtwork
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio

/** Album-specific artwork with the same text and interaction layout as other collection cards. */
@Composable
internal fun VinylAlbumCard(
    title: String,
    subtitle: String,
    coverUrl: String?,
    modifier: Modifier = Modifier.width(136.dp * VinylAlbumArtworkRatio),
    isLoading: Boolean = false,
    enabled: Boolean = true,
    textSpacing: Dp = 8.dp,
    titleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodySmall,
    onClick: () -> Unit,
) {
    ArtworkCard(
        title = title, subtitle = subtitle, coverUrl = coverUrl, modifier = modifier,
        reserveSubtitleSpace = true, isLoading = isLoading, enabled = enabled,
        textSpacing = textSpacing, titleStyle = titleStyle, subtitleStyle = subtitleStyle,
        artworkContent = { VinylAlbumArtwork(coverUrl, isLoading = isLoading) },
        onClick = onClick,
    )
}
