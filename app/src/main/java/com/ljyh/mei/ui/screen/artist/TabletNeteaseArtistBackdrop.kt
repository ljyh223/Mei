package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import coil3.Image
import kotlinx.coroutines.CancellationException
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import com.ljyh.mei.ui.screen.artist.component.ArtistColorBackdrop

/** One NetEase decode serves the circular portrait and the same color field as ordinary AM artwork. */
@Composable
internal fun TabletNeteaseArtistBackdrop(
    state: ArtistDetailUiState,
    visible: Boolean,
    onPortraitChanged: (ArtistArtworkLoadState, Painter?, Color?) -> Unit,
    onColorRequested: suspend (String, Image) -> Color? = { _, _ -> null },
) {
    val sources = remember(state.artist?.avatar, state.artist?.cover) {
        listOfNotNull(state.artist?.avatar?.takeIf(String::isNotBlank),
            state.artist?.cover?.takeIf(String::isNotBlank)).distinct()
    }
    var index by remember(state.id, sources) { mutableIntStateOf(0) }
    val report by rememberUpdatedState(onPortraitChanged)
    val colorRequested by rememberUpdatedState(onColorRequested)
    if (sources.isEmpty()) {
        LaunchedEffect(state.id, sources) { report(ArtistArtworkLoadState.Unavailable, null, null) }
        return
    }
    val url = sources[index]
    key(state.id, url) {
        var palette by remember { mutableStateOf<Color?>(null) }
        var paletteAttempted by remember { mutableStateOf(false) }
        SubcomposeAsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize()) {
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
            if (visible && image != null) ArtistColorBackdrop(image)
        }
    }
}
