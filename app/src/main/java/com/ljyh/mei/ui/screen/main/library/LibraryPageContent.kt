package com.ljyh.mei.ui.screen.main.library

import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import com.ljyh.mei.ui.screen.main.library.component.LibraryBrowseLayout

internal data class LibraryPageState(
    val state: LibraryUiState,
    val likedArtwork: String?,
    val likedSongTitle: String?,
    val signedIn: Boolean,
    val scrollBehavior: TopAppBarScrollBehavior,
    val preparingPlayback: Boolean,
    val canPlay: Boolean,
    val likedArtworks: List<String> = listOfNotNull(likedArtwork),
    val likedArtworkLoading: Boolean = false,
)
internal data class LibraryPageActions(
    val onPlayLiked: (String) -> Unit,
    val onOpenPlaylist: (String) -> Unit,
    val onOpenAlbum: (Long) -> Unit,
    val onOpenCollection: (LibraryCollection) -> Unit,
    val onLogin: () -> Unit,
    val onRetry: () -> Unit,
)

@Composable
internal fun LibraryPageContent(state: LibraryPageState, actions: LibraryPageActions) {
    LibraryBrowseLayout(
        state.state, state.likedArtwork, state.likedSongTitle, state.signedIn,
        state.scrollBehavior, state.preparingPlayback, state.canPlay,
        actions.onPlayLiked, actions.onOpenPlaylist, actions.onOpenAlbum,
        actions.onOpenCollection, actions.onLogin, actions.onRetry,
    )
}
