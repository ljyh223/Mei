package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import coil3.Image
import kotlinx.coroutines.CancellationException
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage

/** Shared NetEase source fallback and palette extraction, using the same decoded image as the UI. */
@Composable
internal fun NeteaseArtistArtwork(
    state: ArtistDetailUiState,
    onPortraitChanged: (ArtistArtworkLoadState, Painter?, Color?) -> Unit,
    onColorRequested: suspend (String, Image) -> Color? = { _, _ -> null },
    preferCover: Boolean = false,
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable (Painter?, ArtistArtworkLoadState) -> Unit,
) {
    val sources = remember(state.artist?.avatar, state.artist?.cover, preferCover) {
        val artist = state.artist
        val urls = if (preferCover) listOf(artist?.cover, artist?.avatar) else listOf(artist?.avatar, artist?.cover)
        urls.mapNotNull { it?.takeIf(String::isNotBlank) }.distinct()
    }
    var index by remember(state.id, sources) { mutableIntStateOf(0) }
    val report by rememberUpdatedState(onPortraitChanged)
    val colorRequested by rememberUpdatedState(onColorRequested)
    if (sources.isEmpty()) {
        LaunchedEffect(state.id, sources) { report(ArtistArtworkLoadState.Unavailable, null, null) }
        Box(modifier) { content(null, ArtistArtworkLoadState.Unavailable) }
        return
    }
    val url = sources[index]
    key(state.id, url) {
        var palette by remember { mutableStateOf<Color?>(null) }
        var paletteAttempted by remember { mutableStateOf(false) }
        SubcomposeAsyncImage(model = url, contentDescription = null, modifier = modifier) {
            val result by painter.state.collectAsState()
            val image = (result as? AsyncImagePainter.State.Success)?.painter
            LaunchedEffect(result, url) {
                report(result.artistArtworkLoadState(), image, palette)
                if (result is AsyncImagePainter.State.Success && !paletteAttempted) {
                    val color = try { colorRequested(url, (result as AsyncImagePainter.State.Success).result.image) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { null }
                    palette = color
                    paletteAttempted = true
                    report(ArtistArtworkLoadState.Success, image, color)
                }
                if (result is AsyncImagePainter.State.Error && index < sources.lastIndex) index++
            }
            content(image, result.artistArtworkLoadState())
        }
    }
}
