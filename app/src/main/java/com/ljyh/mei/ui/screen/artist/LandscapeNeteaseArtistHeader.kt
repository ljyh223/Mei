package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.shimmer.skeleton

/** Keep phone-landscape NetEase artwork in the same photo/title/action frame as Apple artwork. */
@Composable
internal fun LandscapeNeteaseArtistHeader(
    state: ArtistDetailUiState,
    actions: ArtistDetailActions,
    onInfo: () -> Unit,
    onArtworkChanged: (ArtistArtworkLoadState, Color?) -> Unit,
    modifier: Modifier = Modifier,
) {
    NeteaseArtistArtwork(state, preferCover = true, modifier = modifier,
        onPortraitChanged = { status, _, color -> onArtworkChanged(status, color) },
        onColorRequested = actions.onPortraitDecoded,
    ) { painter, status ->
        ArtistPhotoHeader(state, actions, onInfo, compact = true,
            title = {
                Box(Modifier.fillMaxWidth().heightIn(min = 40.dp), contentAlignment = Alignment.Center) {
                    Text(state.artist?.name.orEmpty(), Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            },
            photo = { frame ->
                if (painter != null) Image(painter, "歌手照片", frame,
                    contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
                else SkeletonShimmerHost(status == ArtistArtworkLoadState.Loading || state.detailLoading, frame) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (status == ArtistArtworkLoadState.Loading || state.detailLoading)
                            Spacer(Modifier.matchParentSize().skeleton(true))
                        else Icon(Icons.Rounded.Person, null, Modifier.size(48.dp))
                    }
                }
            })
    }
}
