package com.ljyh.mei.ui.screen.artist

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.ui.screen.artist.component.ArtistProfileHeader

/** NetEase supplies the portrait and text; the ordinary profile layout is shared with AM. */
@Composable
internal fun TabletNeteaseArtistHeader(
    state: ArtistDetailUiState,
    actions: ArtistDetailActions,
    onInfo: () -> Unit,
    topPadding: Dp,
    artworkModifier: Modifier,
    portrait: Painter?,
    loading: Boolean,
) {
    ArtistProfileHeader(state, actions, onInfo, portrait, loading, topPadding, artworkModifier) {
        Text(state.artist?.name ?: if (state.detailLoading) "正在加载" else "歌手",
            modifier = Modifier.fillMaxWidth().skeleton(state.detailLoading).semantics { heading() },
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
