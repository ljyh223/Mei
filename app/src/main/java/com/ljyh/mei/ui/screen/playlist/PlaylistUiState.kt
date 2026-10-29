package com.ljyh.mei.ui.screen.playlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.component.player.overlay.OverlayState
import com.ljyh.mei.ui.model.PlaylistHeaderStyle
import com.ljyh.mei.ui.model.UiPlaylist

/** Shared input for single-pane and two-pane presentation; it does not select a device layout. */
internal data class PlaylistUiState(
    val uiData: UiPlaylist,
    val playlistId: Long = uiData.id,
    val trackIds: List<Long> = uiData.tracks.map { it.id },
    val playlists: List<Playlist> = emptyList(),
    val overlay: OverlayState = OverlayState.None,
    val pagingItems: LazyPagingItems<MediaMetadata>? = null,
    val isLoading: Boolean,
    val loadingHeaderStyle: PlaylistHeaderStyle? = null,
    val headerActionIcon: ImageVector,
    val headerActionLabel: String,
    val playlistSearchQuery: String = "",
    val isPlaylistSearchActive: Boolean = false,
    val reorderItems: List<PlaylistOrderItem>? = null,
    val reorderSaving: Boolean = false,
)

/** Entry-owned intents, including menu operations; layouts only invoke them. */
internal data class PlaylistActions(
    val onPlayAll: () -> Unit,
    val onOverlayChange: (OverlayState) -> Unit = {},
    val menu: PlaylistMenuActions = PlaylistMenuActions(),
    val onHeaderAction: () -> Unit,
    val onDownload: () -> Unit = {},
    val onTrackClick: (MediaMetadata, Int) -> Unit,
    val onTrackDownload: ((MediaMetadata) -> Unit)? = null,
    val onBack: () -> Unit,
    val onPlaylistSearchQueryChange: ((String) -> Unit)? = null,
    val onPlaylistSearchActiveChange: (Boolean) -> Unit = {},
    val onMoveTrack: (Int, Int) -> Unit = { _, _ -> },
    val onReorderFinished: () -> Unit = {},
)

/** Layout input supplied by the shared chrome, including one list state and menu anchor handling. */
internal class PlaylistLayoutScope(
    val state: PlaylistUiState,
    val actions: PlaylistActions,
    val listState: LazyListState,
    val padding: PaddingValues,
    val detailPaneWidth: Dp = 280.dp,
    val onTrackMore: (MediaMetadata, IntRect) -> Unit,
    val header: @Composable (compact: Boolean) -> Unit,
)
