package com.ljyh.mei.ui.screen.playlist.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.component.glass.meiTopBarContentClearance
import com.ljyh.mei.ui.screen.playlist.PlaylistLayoutScope
import com.ljyh.mei.ui.screen.playlist.component.PlaylistTracks

/** A single scrolling column; the playlist header is the first item in the song list. */
@Composable
internal fun SinglePanePlaylistLayout(content: PlaylistLayoutScope) {
    val direction = LocalLayoutDirection.current
    PlaylistTracks(
        content = content,
        headerContent = { content.header(false, null) },
        contentPadding = PaddingValues(
            start = content.padding.calculateStartPadding(direction),
            end = content.padding.calculateEndPadding(direction),
            top = content.padding.calculateTopPadding() + meiTopBarContentClearance(),
            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
        ),
    )
}
