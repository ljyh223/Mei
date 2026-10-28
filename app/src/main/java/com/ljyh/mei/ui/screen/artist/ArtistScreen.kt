package com.ljyh.mei.ui.screen.artist

import com.ljyh.mei.ui.component.card.ArtworkCard
import com.ljyh.mei.playback.queue.QueueSource

import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.data.model.domain.toMediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.track.Track
import com.ljyh.mei.ui.component.sheet.DescriptionSheet
import com.ljyh.mei.ui.component.track.TrackPlaceholder
import com.ljyh.mei.ui.component.track.BasicTrackActionMenu
import com.ljyh.mei.ui.component.track.TrackMenuTarget
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.ui.local.LocalNavController
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.ljyh.mei.ui.component.glass.MeiHazeTopBar
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.screen.Screen
import java.util.UUID
import kotlinx.coroutines.CancellationException
import com.ljyh.mei.ui.component.layout.meiPageGutter


@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    id: String,
    viewModel: ArtistViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current
    var menuTrack by remember { mutableStateOf<TrackMenuTarget?>(null) }

    val artistDetail by viewModel.artistDetail.collectAsState()
    val artistAlbums by viewModel.artistAlbums.collectAsState()
    val artistSongs by viewModel.artistSongs.collectAsState()
    var showDescriptionSheet by remember(id) { mutableStateOf(false) }
    val artist = (artistDetail as? Resource.Success)?.data?.data?.artist
    val artistImageUrl = artist?.cover?.takeIf { it.isNotBlank() }
        ?: artist?.avatar?.takeIf { it.isNotBlank() }
    var artistColor by remember(artistImageUrl) {
        mutableStateOf(artistImageUrl?.let(viewModel::cachedArtistColor))
    }
    LaunchedEffect(artistImageUrl) {
        if (artistImageUrl != null) {
            try {
                artistColor = viewModel.artistColor(artistImageUrl)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the theme background if color extraction fails.
            }
        }
    }

    val pageBackground = MaterialTheme.colorScheme.background
    val targetHeaderSurface = artistColor?.let {
        lerp(pageBackground, it, if (pageBackground.luminance() < 0.5f) 0.38f else 0.24f)
    } ?: pageBackground
    val headerSurface by animateColorAsState(targetHeaderSurface, label = "artistHeaderColor")

    val scrollState = rememberLazyListState()
    // Show the compact title after the artist image has mostly scrolled away.
    val heroHeightPx = with(LocalDensity.current) { 460.dp.toPx() }
    val showTopBarTitle by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 ||
                    scrollState.firstVisibleItemScrollOffset > heroHeightPx * 0.6f
        }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val pageHaze = rememberHazeState()
    val barProgress by animateFloatAsState(if (showTopBarTitle) 1f else 0f, label = "artistBar")

    LaunchedEffect(id) {
        viewModel.getArtistDetail(id)
        viewModel.getArtistAlbums(id)
        viewModel.getArtistSongs(id)
    }

    Scaffold(
        topBar = {

            MeiHazeTopBar(
                state = pageHaze,
                title = (artistDetail as? Resource.Success)?.data?.data?.artist?.name ?: "",
                progress = barProgress,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(pageHaze)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            // Hero 顶到状态栏下方，TopBar 悬浮其上
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding() + LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
            )
        ) {
            // --- 1. Hero + Info Header ---
            item {
                when (val detail = artistDetail) {
                    is Resource.Success -> ArtistHeader(
                        artist = detail.data.data.artist,
                        headerSurface = headerSurface,
                        canPlayHotSongs = playerConnection != null &&
                            ((artistSongs as? Resource.Success)?.data?.hotSongs?.isNotEmpty() == true),
                        onBrowseSongs = { Screen.ArtistSongs.navigate(navController) { addPath(id) } },
                        onPlayHotSongs = {
                            val songs = (artistSongs as? Resource.Success)?.data?.hotSongs.orEmpty()
                            val first = songs.firstOrNull()
                            if (first != null) {
                                val source = QueueSource(QueueSource.Type.ARTIST, id, detail.data.data.artist.name)
                                playerConnection?.onTrackClicked(
                                    trackId = first.id.toString(),
                                    fallbackSource = source,
                                    buildQueue = {
                                        ListQueue(
                                            UUID.randomUUID().toString(),
                                            "热门单曲",
                                            songs.map { it.id.toString() to it.toMediaMetadata().toMediaItem() },
                                            0,
                                            source = source,
                                        )
                                    },
                                )
                            }
                        },
                    )
                    is Resource.Loading -> ArtistHeaderShimmer(headerSurface)
                    is Resource.Error -> ErrorItem(detail.message)
                }
            }

            val detail = artistDetail as? Resource.Success
            val artist = detail?.data?.data?.artist
            if (artist != null && (artist.briefDesc.isNotBlank() ||
                    detail.data.data.secondaryExpertIdentiy.any { it.expertIdentiyCount > 0 })) {
                item {
                    ArtistAbout(
                        artist = artist,
                        headerSurface = headerSurface,
                        expertIdentities = detail.data.data.secondaryExpertIdentiy.filter { it.expertIdentiyCount > 0 },
                        onExpandDescription = { showDescriptionSheet = true },
                    )
                }
            } else if (artist != null) {
                item {
                    Spacer(
                        Modifier.fillMaxWidth().height(48.dp).background(
                            Brush.verticalGradient(listOf(headerSurface, pageBackground))
                        )
                    )
                }
            }

            // --- 2. Hot Songs ---
            item { SectionTitle("热门单曲", onShowAll = {
                Screen.ArtistSongs.navigate(navController) { addPath(id) }
            }) }

            when (val songsResource = artistSongs) {
                is Resource.Success -> {
                    val songs = songsResource.data.hotSongs
                    if (songs.isEmpty()) item { EmptyArtistSection("暂无热门单曲") }
                    items(songs.take(8), key = { it.id }) { song ->
                        Track(
                            track = song.toMediaMetadata(),
                            onClick = {
                                val allIds = songs.map {
                                    it.id.toString() to it.toMediaMetadata().toMediaItem()
                                }
                                playerConnection?.onTrackClicked(
                                    trackId = song.id.toString(),
                                    fallbackSource = QueueSource(QueueSource.Type.ARTIST, id,
                                        (artistDetail as? Resource.Success)?.data?.data?.artist?.name),
                                    buildQueue = {
                                        ListQueue(
                                            UUID.randomUUID().toString(),
                                            "Hot Songs",
                                            allIds,
                                            songs.indexOf(song),
                                            source = QueueSource(QueueSource.Type.ARTIST, id,
                                                (artistDetail as? Resource.Success)?.data?.data?.artist?.name),
                                        )
                                    }
                                )
                            },
                            onMoreClick = { anchor -> menuTrack = TrackMenuTarget(song.toMediaMetadata(), anchor,
                                QueueSource(QueueSource.Type.ARTIST, id,
                                    (artistDetail as? Resource.Success)?.data?.data?.artist?.name)) }
                        )
                    }
                }
                is Resource.Loading -> item {
                    SkeletonShimmerHost(enabled = true) {
                        Column {
                            repeat(5) { index -> TrackPlaceholder(index = index) }
                        }
                    }
                }
                is Resource.Error -> item { ErrorItem(songsResource.message) }
            }

            // --- 3. Albums ---
            item { SectionTitle("专辑", onShowAll = {
                Screen.ArtistAlbums.navigate(navController) { addPath(id) }
            }) }

            when (val albumsResource = artistAlbums) {
                is Resource.Success -> {
                    val albums = albumsResource.data.hotAlbums.take(10)
                    if (albums.isEmpty()) item { EmptyArtistSection("暂无专辑") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = meiPageGutter()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(albums, key = { it.id }) { hotAlbum ->
                                AlbumCard(
                                    album = Album(
                                        id = hotAlbum.id,
                                        title = hotAlbum.name,
                                        cover = hotAlbum.picUrl,
                                        artist = hotAlbum.artists.map {
                                            Album.Artist(it.id.toLong(), it.name)
                                        },
                                        size = hotAlbum.size
                                    ),
                                    onClick = {
                                        navController.navigate("${Screen.Album.route}/$it")
                                    }
                                )
                            }
                        }
                    }
                }
                is Resource.Loading -> item {
                    SkeletonShimmerHost(enabled = true) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = meiPageGutter()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(4) { AlbumCardShimmer() }
                        }
                    }
                }
                is Resource.Error -> item { ErrorItem(albumsResource.message) }
            }

        }
    }
    BasicTrackActionMenu(menuTrack) { menuTrack = null }
    if (showDescriptionSheet && artist != null) {
        DescriptionSheet(artist.name, artist.briefDesc) { showDescriptionSheet = false }
    }
}


// ─── Artist identity and primary action ──────────────────────────────────────

@Composable
fun ArtistHeader(
    artist: ArtistDetail.Data.Artist?,
    isLoading: Boolean = false,
    headerSurface: Color = MaterialTheme.colorScheme.background,
    canPlayHotSongs: Boolean = false,
    onPlayHotSongs: () -> Unit = {},
    onBrowseSongs: () -> Unit = {},
) {
    val loading = isLoading || artist == null
    val colors = MaterialTheme.colorScheme
    val imageUrl = artist?.cover?.takeIf { it.isNotBlank() }
        ?: artist?.avatar?.takeIf { it.isNotBlank() }
    val aliases = artist?.let {
        (it.transNames + it.alias.mapNotNull { value ->
            (value as? kotlinx.serialization.json.JsonPrimitive)
                ?.takeIf { primitive -> primitive.isString }?.content
        }).filter(String::isNotBlank).distinct()
    }.orEmpty()
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
            Box(Modifier.fillMaxWidth().height(460.dp)) {
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
private fun ArtistAbout(
    artist: ArtistDetail.Data.Artist,
    headerSurface: Color,
    expertIdentities: List<ArtistDetail.Data.SecondaryExpertIdentiy>,
    onExpandDescription: () -> Unit,
) {
    var canExpand by remember(artist.briefDesc) { mutableStateOf(false) }
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = artist.briefDesc,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { canExpand = it.hasVisualOverflow },
                )
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

@Composable
private fun EmptyArtistSection(message: String) {
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
fun ArtistHeaderShimmer(headerSurface: Color = MaterialTheme.colorScheme.background) {
    SkeletonShimmerHost(enabled = true) {
        ArtistHeader(
            artist = null,
            isLoading = true,
            headerSurface = headerSurface,
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
        if (onShowAll != null) TextButton(onClick = onShowAll) { Text("查看全部") }
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
