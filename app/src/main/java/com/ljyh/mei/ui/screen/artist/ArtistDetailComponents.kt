package com.ljyh.mei.ui.screen.artist

import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.ui.component.MeiShowAllButton
import com.ljyh.mei.ui.component.card.ArtworkCard
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.ui.model.Album

// ─── Artist identity and primary action ──────────────────────────────────────

@Composable
fun ArtistHeader(
    artist: ArtistDetail.Data.Artist?,
    isLoading: Boolean = false,
    headerSurface: Color = MaterialTheme.colorScheme.background,
    canPlayHotSongs: Boolean = false,
    onPlayHotSongs: () -> Unit = {},
    onBrowseSongs: () -> Unit = {},
    artworkModifier: Modifier = Modifier,
) {
    val loading = isLoading || artist == null
    val colors = MaterialTheme.colorScheme
    val imageUrl = artist?.cover?.takeIf { it.isNotBlank() }
        ?: artist?.avatar?.takeIf { it.isNotBlank() }
    val aliases = artist?.let(::artistDisplayAliases).orEmpty()
    val identity = (artist?.identifyTag ?: artist?.identities.orEmpty())
        .filter(String::isNotBlank).distinct().take(3).joinToString(" · ")

    Box(Modifier.fillMaxWidth().background(headerSurface).clip(RoundedCornerShape(0.dp))) {
        // A blurred copy preserves the colors and light in the photograph across the fade.
        // On older Android versions blur is unavailable, so the extracted color is used.
        if (imageUrl != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize().blur(48.dp),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to headerSurface.copy(alpha = 0.35f),
                    0.55f to headerSurface.copy(alpha = 0.65f),
                    0.84f to headerSurface,
                    1f to headerSurface,
                )
            )
        )
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(460.dp).then(artworkModifier)) {
                if (loading) {
                    Spacer(Modifier.fillMaxSize().skeleton(true, RoundedCornerShape(0.dp)))
                } else if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.White,
                                        0.48f to Color.White,
                                        0.72f to Color.White.copy(alpha = 0.72f),
                                        1f to Color.Transparent,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            },
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .padding(horizontal = meiPageGutter(), vertical = 24.dp),
                ) {
                    if (identity.isNotBlank()) {
                        Text(
                            text = identity,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onBackground.copy(alpha = 0.82f),
                            modifier = Modifier.padding(bottom = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = artist?.name?.takeIf { it.isNotBlank() } ?: if (loading) "歌手名称" else "未知歌手",
                        modifier = Modifier.skeleton(loading),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp, lineHeight = 44.sp),
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (aliases.isNotEmpty()) {
                        Text(
                            text = aliases.joinToString(" · "),
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onBackground.copy(alpha = 0.78f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!loading) {
                        Text(
                            text = "${artist.musicSize.formatCount()} 首歌曲  ·  ${artist.albumSize.formatCount()} 张专辑  ·  ${artist.mvSize.formatCount()} 个 MV",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onBackground.copy(alpha = 0.72f),
                        )
                    } else {
                        Spacer(Modifier.padding(top = 8.dp).width(120.dp).height(18.dp).skeleton(true))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = meiPageGutter()).padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onPlayHotSongs,
                    enabled = !loading && canPlayHotSongs,
                    modifier = Modifier.weight(1f).height(48.dp).skeleton(loading, RoundedCornerShape(50)),
                    shape = RoundedCornerShape(50),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("播放热门")
                }
                OutlinedButton(
                    onClick = onBrowseSongs,
                    enabled = !loading,
                    modifier = Modifier.weight(1f).height(48.dp).skeleton(loading, RoundedCornerShape(50)),
                    shape = RoundedCornerShape(50),
                ) {
                    Text("全部歌曲")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ArtistAbout(
    artist: ArtistDetail.Data.Artist,
    headerSurface: Color,
    expertIdentities: List<ArtistDetail.Data.SecondaryExpertIdentiy>,
    maxDescriptionLines: Int = 1,
    expandBelowDescription: Boolean = false,
    onExpandDescription: () -> Unit,
) {
    var canExpand by remember(artist.briefDesc, maxDescriptionLines, expandBelowDescription) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(headerSurface, MaterialTheme.colorScheme.background)))
            .padding(horizontal = meiPageGutter()).padding(top = 4.dp, bottom = 12.dp),
    ) {
        Text("关于歌手", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (expertIdentities.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                expertIdentities.forEach { expert ->
                    Text(
                        text = "${expert.expertIdentiyName} · ${expert.expertIdentiyCount.formatCount()}",
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(6.dp),
                        ).padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (artist.briefDesc.isNotBlank()) {
            @Composable
            fun DescriptionText(modifier: Modifier) {
                Text(
                    text = artist.briefDesc,
                    modifier = modifier,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp,
                    maxLines = maxDescriptionLines,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { canExpand = it.hasVisualOverflow },
                )
            }
            if (expandBelowDescription) {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    DescriptionText(Modifier.fillMaxWidth())
                    if (canExpand) {
                        TextButton(
                            onClick = onExpandDescription,
                            modifier = Modifier.align(Alignment.Start),
                            contentPadding = PaddingValues(horizontal = 0.dp),
                        ) { Text("展开") }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DescriptionText(Modifier.weight(1f))
                    if (canExpand) {
                        TextButton(
                            onClick = onExpandDescription,
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) { Text("展开") }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyArtistSection(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(horizontal = meiPageGutter(), vertical = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

// ─── Album Card ───────────────────────────────────────────────────────────────

@Composable
fun AlbumCard(album: Album? = null, isLoading: Boolean = false, onClick: (Long) -> Unit) {
    ArtworkCard(
        title = album?.title.orEmpty(), coverUrl = album?.cover,
        subtitle = album?.let { "${it.size} 首" } ?: "00 首",
        modifier = Modifier.width(116.dp), isLoading = isLoading, enabled = album != null,
        cornerRadius = 8.dp, textSpacing = 6.dp,
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
) {
    SkeletonShimmerHost(enabled = true) {
        ArtistHeader(
            artist = null,
            isLoading = true,
            headerSurface = headerSurface,
            artworkModifier = artworkModifier,
        )
    }
}


// ─── Shared ───────────────────────────────────────────────────────────────────

@Composable
fun SectionTitle(title: String, onShowAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = meiPageGutter(), end = meiPageGutter(), top = 16.dp),
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

private fun Int.formatCount(): String = when {
    this >= 100_000_000 -> "${this / 100_000_000}亿"
    this >= 10_000 -> "${this / 10_000}万"
    else -> this.toString()
}
