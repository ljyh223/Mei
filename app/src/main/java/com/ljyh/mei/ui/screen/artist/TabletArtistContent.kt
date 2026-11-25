package com.ljyh.mei.ui.screen.artist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.ui.component.MeiStatePanel
import com.ljyh.mei.ui.component.glass.*
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.sheet.DescriptionSheet
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.utils.preferences.rememberPreference
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun TabletArtistContent(state: ArtistDetailUiState, actions: ArtistDetailActions) {
    val artwork = state.appleArtwork?.takeIf { state.appleArtworkEnabled }
    var presentation by remember(state.id, artwork) { mutableStateOf(ArtistArtworkPresentation()) }
    var portraitPainter by remember(state.id, artwork) { mutableStateOf<Painter?>(null) }
    var neteasePortrait by remember(state.id, state.artist?.avatar, state.artist?.cover) { mutableStateOf<Painter?>(null) }
    var neteaseColor by remember(state.id, state.artist?.avatar, state.artist?.cover) { mutableStateOf<Color?>(null) }
    var neteasePhotoState by remember(state.id, state.artist?.avatar, state.artist?.cover) {
        mutableStateOf(ArtistArtworkLoadState.Unavailable)
    }
    var description by rememberSaveable(state.id) { mutableStateOf(false) }
    val debugEnabled by rememberPreference(DebugKey, defaultValue = false)
    val list = rememberLazyListState()
    val bar = rememberMeiArtworkTopBarState(list, state.id)
    val progress by animateFloatAsState(if (bar.artworkPassed) 1f else 0f, label = "tabletArtistBar")
    val haze = rememberHazeState()
    val density = LocalDensity.current.density
    val scrollPixels = remember(list, density) { {
        list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "artist_header" }?.let {
            (-it.offset).coerceAtLeast(0).toFloat()
        } ?: if (list.firstVisibleItemIndex > 0) 380f * density else 0f
    } }
    val neteaseReady = !presentation.usesApplePhoto && neteasePhotoState == ArtistArtworkLoadState.Success
    ArtistArtworkTheme(artwork, presentation.usesApplePhoto || neteaseReady, immersive = true,
        backgroundOverride = presentation.background,
        sourceColor = if (neteaseReady) neteaseColor ?: Color(0xFF252A30) else null) {
        val background = MaterialTheme.colorScheme.background
        val bodyWidth = 1280.dp
        val discography = remember(state.albums) { artistDiscography(state.albums) }
        BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().background(background)) {
            TabletNeteaseArtistBackdrop(state, visible = !presentation.usesApplePhoto,
                onPortraitChanged = { status, painter, color ->
                    neteasePhotoState = status
                    neteasePortrait = painter
                    neteaseColor = color
                }, onColorRequested = actions.onPortraitDecoded)
            if (artwork != null) TabletArtistBackdrop(artwork, scrollPixels) { status, photo, painter ->
                presentation = presentation.copy(photo = status, background = photo.image.background,
                    tabletPhotoKind = photo.kind)
                portraitPainter = painter.takeIf { !photo.hasHero }
            }
            val preferredHeroHeight = minOf(maxWidth * 0.58f, maxHeight * 0.68f).coerceIn(320.dp, 600.dp)
            val viewportHeight = maxHeight
            Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, contentWindowInsets = WindowInsets(0), topBar = {
                MeiHazeTopBar(haze, state.artist?.name.orEmpty(), progress,
                    modifier = bar.toolbarModifier, onNavigateBack = actions.onBack,
                    backgroundProgress = progress,
                    backgroundMode = if (bar.artworkPassed) MeiTopBarBackground.Auto else MeiTopBarBackground.Transparent,
                    actions = { ArtistMoreButton(state, actions) { description = true } })
            }) { padding ->
                val bottom = maxOf(padding.calculateBottomPadding(),
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding())
                // Leave a heading and one complete song row above the floating bottom controls.
                val heroHeight = minOf(preferredHeroHeight, (viewportHeight - bottom - 144.dp).coerceAtLeast(320.dp))
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().hazeSource(haze),
                    contentPadding = PaddingValues(bottom = bottom + 24.dp)) {
                    item(key = "artist_header") {
                        if (state.artist == null && !state.detailLoading) {
                            TabletArtistBody(bodyWidth) { MeiStatePanel("歌手信息暂未加载", Icons.Rounded.CloudOff,
                                actionLabel = "重试", onAction = actions.onRetryDetail) }
                        } else TabletArtistHeader(state, artwork, presentation, portraitPainter, heroHeight, padding.calculateTopPadding() + 16.dp, actions, onInfo = { description = true },
                            onWordmarkChanged = { presentation = presentation.copy(wordmark = it) }, artworkModifier = bar.artworkModifier,
                            neteasePortrait = neteasePortrait, neteaseLoading = neteasePhotoState == ArtistArtworkLoadState.Loading)
                    }
                    if (debugEnabled && state.artist != null) item(key = "artist_artwork_debug") {
                        TabletArtistBody(bodyWidth) { ArtistArtworkDebugInfo(artwork, presentation, state.appleArtworkEnabled,
                            Modifier.padding(meiPageHorizontalPadding())) }
                    }
                    item(key = "artist_overview") { TabletArtistBody(bodyWidth) {
                        TabletArtistOverview(state, discography.latest, actions)
                    } }
                    item(key = "albums") { TabletArtistBody(bodyWidth) {
                        TabletArtistReleaseShelf("专辑", discography.albums, state, actions, actions.onBrowseAlbumReleases)
                    } }
                    item(key = "singles") { TabletArtistBody(bodyWidth) {
                        TabletArtistReleaseShelf("单曲与 EP", discography.singles, state, actions, actions.onBrowseSingles)
                    } }
                    // Music videos will follow releases when their design and data source are ready.
                }
            }
        }
        if (description && state.artist != null) DescriptionSheet(state.artist.name,
            state.artist.briefDesc.ifBlank { "暂无简介" }) { description = false }
    }
}

@Composable
private fun TabletArtistBody(maxWidth: Dp, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = maxWidth).fillMaxWidth(), content = content)
    }
}
