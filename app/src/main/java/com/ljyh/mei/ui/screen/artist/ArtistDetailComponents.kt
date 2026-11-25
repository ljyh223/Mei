package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.ui.component.MeiShowAllButton
import com.ljyh.mei.ui.component.card.VinylAlbumCard
import com.ljyh.mei.ui.component.artwork.VinylAlbumArtworkRatio
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.ui.model.Album

// ─── Artist identity and primary action ──────────────────────────────────────

@Composable
internal fun ArtistHeader(
    artist: ArtistDetail.Data.Artist?,
    isLoading: Boolean = false,
    headerSurface: Color = MaterialTheme.colorScheme.background,
    canPlayHotSongs: Boolean = false,
    onPlayHotSongs: () -> Unit = {},
    onBrowseSongs: () -> Unit = {},
    artworkModifier: Modifier = Modifier,
    compact: Boolean = false,
    onInfo: () -> Unit = {},
    subscription: ArtistSubscriptionState = ArtistSubscriptionState(),
    onToggleSubscription: () -> Unit = {},
) {
    val loading = isLoading || artist == null
    val colors = MaterialTheme.colorScheme
    val imageUrl = artist?.cover?.takeIf { it.isNotBlank() }
        ?: artist?.avatar?.takeIf { it.isNotBlank() }

    Column(Modifier.fillMaxWidth().background(colors.background)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val photoHeight = if (compact) 224.dp else (maxWidth * 1.16f).coerceIn(360.dp, 480.dp)
            Box(Modifier.fillMaxWidth().height(photoHeight).then(artworkModifier).background(headerSurface)) {
                if (loading) {
                    Spacer(Modifier.fillMaxSize().skeleton(true, RoundedCornerShape(0.dp)))
                } else if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "歌手照片",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                    )
                }
                // Finish at the exact page color so no tinted block ends above the catalogue.
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.48f to Color.Transparent,
                    0.72f to colors.background.copy(alpha = 0.38f),
                    0.92f to colors.background.copy(alpha = 0.90f),
                    1f to colors.background,
                )))
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(meiPageHorizontalPadding())
                .padding(top = 4.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 20.dp else 24.dp),
        ) {
            Text(
                text = artist?.name?.takeIf { it.isNotBlank() } ?: if (loading) "歌手名称" else "未知歌手",
                modifier = Modifier.fillMaxWidth().skeleton(loading),
                style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ArtistPrimaryActions(canPlay = !loading && canPlayHotSongs, subscription = subscription,
                onInfo = onInfo, onPlay = onPlayHotSongs, onToggleSubscription = onToggleSubscription, compact = compact)
        }
    }
}

@Composable
internal fun EmptyArtistSection(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(meiPageHorizontalPadding()).padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

// ─── Album Card ───────────────────────────────────────────────────────────────

@Composable
fun AlbumCard(album: Album? = null, isLoading: Boolean = false, onClick: (Long) -> Unit) {
    VinylAlbumCard(
        title = album?.title.orEmpty(), coverUrl = album?.cover,
        subtitle = album?.let { "${it.size} 首" } ?: "00 首",
        modifier = Modifier.width(116.dp * VinylAlbumArtworkRatio), isLoading = isLoading, enabled = album != null,
        textSpacing = 6.dp,
        titleStyle = MaterialTheme.typography.labelMedium, subtitleStyle = MaterialTheme.typography.labelSmall,
        onClick = { album?.id?.let(onClick) },
    )
}

@Composable
fun AlbumCardShimmer() {
    AlbumCard(isLoading = true, onClick = {})
}


// ─── Header Shimmer ───────────────────────────────────────────────────────────

@Composable
fun ArtistHeaderShimmer(
    headerSurface: Color = MaterialTheme.colorScheme.background,
    artworkModifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    SkeletonShimmerHost(enabled = true) {
        ArtistHeader(
            artist = null,
            isLoading = true,
            headerSurface = headerSurface,
            artworkModifier = artworkModifier,
            compact = compact,
        )
    }
}


// ─── Shared ───────────────────────────────────────────────────────────────────

@Composable
fun SectionTitle(title: String, onShowAll: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(meiPageHorizontalPadding()).padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f))
        if (onShowAll != null) MeiShowAllButton(onClick = onShowAll)
    }
}

@Composable
fun ErrorItem(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(20.dp),
        color = MaterialTheme.colorScheme.error
    )
}
