package com.ljyh.mei.ui.screen.playlist

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.ui.component.player.overlay.OverlayState
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.playlist.component.PlaylistActionMenu
import com.ljyh.mei.utils.system.setClipboard

/** Menu intents form part of the presentation boundary, with no ViewModel in their signatures. */
data class PlaylistMenuActions(
    val refreshPlaylists: () -> Unit = {},
    val addSong: (Playlist, Long) -> Unit = { _, _ -> },
    val deleteSong: (Long) -> Unit = {},
    val playNext: (MediaMetadata, QueueSource) -> Unit = { _, _ -> },
    val addToQueue: (MediaMetadata, QueueSource) -> Unit = { _, _ -> },
    val copy: (String, String) -> Unit = { _, _ -> },
)

/** Bind services at the screen entry, keeping the content and menu independently replaceable. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun rememberPlaylistMenuActions(viewModel: PlaylistViewModel, playlistId: Long): PlaylistMenuActions {
    val context = LocalContext.current
    val player = LocalPlayerConnection.current
    return remember(viewModel, playlistId, context, player) {
        fun message(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        PlaylistMenuActions(
            refreshPlaylists = viewModel::getAllMePlaylist,
            addSong = { playlist, trackId ->
                viewModel.addSongToPlaylist(playlist.id, trackId.toString(), playlist.count) { outcome ->
                    message(when (outcome) {
                        PlaylistTrackAddOutcome.Added -> "已添加到 ${playlist.title}"
                        PlaylistTrackAddOutcome.AlreadyExists -> "歌曲已在 ${playlist.title} 中"
                        PlaylistTrackAddOutcome.Failed -> "添加到歌单失败"
                    })
                }
            },
            deleteSong = { trackId ->
                viewModel.deleteSongFromPlaylist(playlistId.toString(), trackId.toString()) { deleted ->
                    if (deleted) viewModel.markTrackRemoved(trackId)
                    message(if (deleted) "已从歌单删除" else "从歌单删除失败")
                }
            },
            playNext = { track, source ->
                if (player == null) message("播放器未就绪") else {
                    player.playNext(track.toMediaItem(), source)
                    message("已添加到下一首")
                }
            },
            addToQueue = { track, source ->
                if (player == null) message("播放器未就绪") else {
                    player.addToQueue(track.toMediaItem(), source)
                    message("已添加到队尾")
                }
            },
            copy = { text, label -> setClipboard(context, text, label) },
        )
    }
}

/** Compatibility binding for existing album callers; new playlist content receives menu intents. */
@Composable
fun PlaylistActionOverlay(
    overlay: OverlayState,
    isCreator: Boolean,
    playlistId: Long,
    queueSource: QueueSource,
    allMePlaylist: List<Playlist>,
    onDismiss: () -> Unit,
    onUpdateOverlay: (OverlayState) -> Unit,
    onDownloadTrack: ((MediaMetadata) -> Unit)? = null,
    viewModel: PlaylistViewModel,
) {
    PlaylistActionMenu(overlay, isCreator, queueSource, allMePlaylist, onDismiss,
        onUpdateOverlay, onDownloadTrack, rememberPlaylistMenuActions(viewModel, playlistId))
}
