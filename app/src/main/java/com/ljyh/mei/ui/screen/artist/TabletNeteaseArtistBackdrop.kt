package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import com.ljyh.mei.ui.screen.artist.component.ArtistColorBackdrop

/** One NetEase decode serves the circular portrait and the same color field as ordinary AM artwork. */
@Composable
internal fun TabletNeteaseArtistBackdrop(
    state: ArtistDetailUiState,
    visible: Boolean,
    onPortraitChanged: (ArtistArtworkLoadState, Painter?) -> Unit,
) {
    val sources = remember(state.artist?.avatar, state.artist?.cover) {
        listOfNotNull(state.artist?.avatar?.takeIf(String::isNotBlank),
            state.artist?.cover?.takeIf(String::isNotBlank)).distinct()
    }
    var index by remember(state.id, sources) { mutableIntStateOf(0) }
    val report by rememberUpdatedState(onPortraitChanged)
    if (sources.isEmpty()) {
        LaunchedEffect(state.id, sources) { report(ArtistArtworkLoadState.Unavailable, null) }
        return
    }
    val url = sources[index]
    key(state.id, url) {
        SubcomposeAsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize()) {
            val result by painter.state.collectAsState()
            val image = (result as? AsyncImagePainter.State.Success)?.painter
            LaunchedEffect(result, url) {
                report(result.artistArtworkLoadState(), image)
                if (result is AsyncImagePainter.State.Error && index < sources.lastIndex) index++
            }
            if (visible && image != null) ArtistColorBackdrop(image)
        }
    }
}
