package com.ljyh.mei.ui.screen.playlist.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.artwork.CenteredArtworkPane
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.glass.meiTopBarContentClearance
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.playlist.PlaylistLayoutScope
import com.ljyh.mei.ui.screen.playlist.component.PlaylistTracks

/** Two independent scrolling regions. This content has no device checks or business state. */
@Composable
internal fun TwoPanePlaylistLayout(
    content: PlaylistLayoutScope,
    compact: Boolean = false,
) {
    val state = content.state
    val direction = LocalLayoutDirection.current
    val topInset = content.padding.calculateTopPadding() + meiTopBarContentClearance() +
        if (compact) 4.dp else 0.dp
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    Row(
        Modifier.fillMaxSize().padding(
            start = content.padding.calculateStartPadding(direction),
            end = content.padding.calculateEndPadding(direction),
        ),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        CenteredArtworkPane(
            padding = PaddingValues(top = topInset, bottom = bottomInset),
            modifier = Modifier.width(content.detailPaneWidth).fillMaxHeight(),
        ) { bodyHeight ->
            val artworkBudget = if (compact) {
                (bodyHeight - 8.dp).coerceAtLeast(96.dp)
            } else {
                (bodyHeight - 220.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)).coerceAtLeast(144.dp)
            }
            content.header(artworkBudget)
        }
        PlaylistTracks(
            content = content,
            modifier = Modifier.weight(1f),
            tableRows = !compact,
            contentPadding = PaddingValues(top = topInset, bottom = bottomInset),
            scrollingControls = {
                if (!state.isLoading && state.isPlaylistSearchActive) {
                    TextField(
                        value = state.playlistSearchQuery,
                        onValueChange = { content.actions.onPlaylistSearchQueryChange?.invoke(it) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = meiPageGutter(), vertical = 12.dp),
                        placeholder = { Text("搜索歌名、歌手或专辑") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                }
            },
        )
    }
}
