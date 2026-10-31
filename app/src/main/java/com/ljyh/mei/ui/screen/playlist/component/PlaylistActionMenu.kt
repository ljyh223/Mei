package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.runtime.Composable
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.ui.component.player.overlay.OverlayState
import com.ljyh.mei.ui.component.sheet.AddToPlaylistSheet
import com.ljyh.mei.ui.component.track.TrackActionMenu
import com.ljyh.mei.ui.component.track.TrackMenuTarget
import com.ljyh.mei.ui.screen.playlist.PlaylistMenuActions

/** Presentation-only menu: data and user intents are supplied by the owner. */
@Composable
internal fun PlaylistActionMenu(
    overlay: OverlayState,
    isCreator: Boolean,
    queueSource: QueueSource,
    allMePlaylist: List<Playlist>,
    onDismiss: () -> Unit,
    onUpdateOverlay: (OverlayState) -> Unit,
    onDownloadTrack: ((MediaMetadata) -> Unit)?,
    actions: PlaylistMenuActions,
) {
    when (overlay) {
        is OverlayState.AddToPlaylist -> {
            AddToPlaylistSheet(
                playlists = allMePlaylist,
                onDismiss = onDismiss,
                onCreateNewPlaylist = {
                    // 这里可以跳转到创建歌单的 Overlay 或页面
                    onUpdateOverlay(OverlayState.CreatePlaylist)
                },
                onSelectPlaylist = { selectedPlaylist ->
                    actions.addSong(selectedPlaylist, overlay.mediaId)
                    onDismiss()
                }
            )
        }

        is OverlayState.TrackActionMenu -> {
            TrackActionMenu(
                target = TrackMenuTarget(overlay.track, overlay.anchor, queueSource),
                isCreator = isCreator,
                onDismiss = onDismiss,
                onAddToPlaylist = {
                    actions.refreshPlaylists()
                    onUpdateOverlay(OverlayState.AddToPlaylist(overlay.track.id))
                },
                onPlayNext = { actions.playNext(overlay.track, queueSource) },
                onAddToQueue = { actions.addToQueue(overlay.track, queueSource) },
                onDownloadTrack = onDownloadTrack?.let { { it(overlay.track) } },
                onDelete = {
                    actions.deleteSong(overlay.track.id)
                    onDismiss()
                },
                onCopyId = {
                    actions.copy(overlay.track.id.toString(), "id")
                    onDismiss()
                },
                onCopyName = {
                    actions.copy(overlay.track.title, "name")
                    onDismiss()
                }
            )
        }

        // 可以在这里扩展 OverlayState.CreatePlaylist 等
        else -> {}
    }
}
