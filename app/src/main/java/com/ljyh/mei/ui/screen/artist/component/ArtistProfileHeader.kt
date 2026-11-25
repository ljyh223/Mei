package com.ljyh.mei.ui.screen.artist.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.layout.meiPageHorizontalPadding
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.ui.screen.artist.ArtistDetailActions
import com.ljyh.mei.ui.screen.artist.ArtistDetailUiState
import com.ljyh.mei.ui.screen.artist.ArtistPrimaryActions

/** Source-independent portrait layout; the supplied painter is also used by the backdrop. */
@Composable
internal fun ArtistProfileHeader(
    state: ArtistDetailUiState,
    actions: ArtistDetailActions,
    onInfo: () -> Unit,
    portrait: Painter?,
    loading: Boolean,
    topPadding: Dp,
    modifier: Modifier,
    title: @Composable () -> Unit,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 416.dp).fillMaxWidth()
            .padding(meiPageHorizontalPadding()).padding(top = topPadding, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(192.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), CircleShape)) {
                if (portrait != null) Image(portrait, "歌手照片", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else SkeletonShimmerHost(loading, Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (loading) Spacer(Modifier.matchParentSize().skeleton(true, CircleShape))
                        else Icon(Icons.Rounded.Person, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Box(Modifier.fillMaxWidth().heightIn(min = 40.dp), contentAlignment = Alignment.Center) {
                title()
            }
            Spacer(Modifier.height(16.dp))
            if (state.artist != null) ArtistPrimaryActions(state.canPlay && state.songs.isNotEmpty(),
                state.subscription, onInfo, actions.onPlayHotSongs, actions.onToggleSubscription)
        }
    }
}
