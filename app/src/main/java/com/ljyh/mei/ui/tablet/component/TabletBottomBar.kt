package com.ljyh.mei.ui.tablet.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.screen.Index
import com.ljyh.mei.ui.tablet.tabletBarLayout

/** Four tabs, playback, search: a stable horizontal layout independent of mobile compacting. */
@Composable
internal fun TabletBottomBar(
    selectedRoute: String,
    metadata: MediaMetadata?,
    playing: Boolean,
    canSkipNext: Boolean,
    visibility: Float,
    searchActive: Boolean,
    query: TextFieldValue,
    searchPlaceholder: String,
    searchFocusRequester: FocusRequester,
    onSelect: (Index) -> Unit,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSearchToggle: () -> Unit,
    onQueryChange: (TextFieldValue) -> Unit,
    onSubmit: (String) -> Unit,
) {
    val enabled = visibility >= 0.99f
    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .graphicsLayer {
                alpha = visibility.coerceIn(0f, 1f)
                translationY = size.height * (1f - visibility.coerceIn(0f, 1f))
            }
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = meiPageGutter(), vertical = 12.dp),
    ) {
        val layout = tabletBarLayout(maxWidth.value, LocalDensity.current.fontScale)
        Row(
            Modifier
                .width(layout.navigationWidth + layout.playerWidth + 88.dp)
                .height(layout.controlHeight)
                .align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabletNavigationTabs(
                selectedRoute = selectedRoute,
                showLabels = layout.showNavigationLabels,
                enabled = enabled,
                onSelect = onSelect,
                modifier = Modifier.width(layout.navigationWidth).height(layout.controlHeight),
            )
            Spacer(Modifier.width(12.dp))
            if (searchActive) TabletSearchField(
                query = query,
                placeholder = searchPlaceholder,
                focusRequester = searchFocusRequester,
                enabled = enabled,
                onQueryChange = onQueryChange,
                onSubmit = onSubmit,
                modifier = Modifier.weight(1f).height(layout.controlHeight),
            ) else {
                TabletMiniPlayer(
                    metadata = metadata,
                    playing = playing,
                    canSkipNext = canSkipNext,
                    enabled = enabled,
                    onOpen = onOpenPlayer,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    modifier = Modifier.width(layout.playerWidth).height(layout.controlHeight),
                )
            }
            Spacer(Modifier.width(12.dp))
            MeiGlassSurface(modifier = Modifier.size(64.dp), shape = CircleShape) {
                Box(Modifier.fillMaxSize().clickable(enabled = enabled, role = Role.Button, onClick = onSearchToggle),
                    contentAlignment = Alignment.Center) {
                    Icon(if (searchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = if (searchActive) "关闭搜索" else searchPlaceholder,
                        modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
