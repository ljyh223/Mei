package com.ljyh.mei.ui.screen.playlist.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.playlist.PlaylistLayoutScope
import com.ljyh.mei.ui.screen.playlist.component.PlaylistTracks

/** Two independent scrolling regions. This content has no device checks or business state. */
@Composable
internal fun TwoPanePlaylistLayout(content: PlaylistLayoutScope) {
    val state = content.state
    val bottomClearance = (LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() -
        content.padding.calculateBottomPadding()).coerceAtLeast(0.dp)
    Row(Modifier.fillMaxSize().padding(content.padding),
        horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(
            Modifier.width(content.detailPaneWidth).align(Alignment.CenterVertically)
                .verticalScroll(rememberScrollState()).padding(bottom = bottomClearance),
        ) {
            content.header(true)
        }
        Column(Modifier.weight(1f)) {
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
            PlaylistTracks(content = content, modifier = Modifier.weight(1f), tableRows = true)
        }
    }
}
