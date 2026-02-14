package com.ljyh.mei.ui.screen.playlist.component

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.component.playlist.AddToPlaylistSheet
import com.ljyh.mei.ui.component.playlist.TrackActionMenu
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.utils.setClipboard


@Composable
fun PlaylistActionOverlay(
    overlay: OverlayState,
    isCreator: Boolean,
    playlistId: Long, // 当前歌单 ID，用于删除歌曲
    allMePlaylist: List<Playlist>, // 用户拥有的歌单列表
    onDismiss: () -> Unit,
    onUpdateOverlay: (OverlayState) -> Unit, // 用于切换状态（如从菜单跳到收藏页）
    viewModel: PlaylistViewModel
) {
    val context = LocalContext.current

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
                    viewModel.addSongToPlaylist(
                        pid = selectedPlaylist.id,
                        trackIds = overlay.mediaId.toString()
                    )
                    Toast.makeText(context, "已添加到 ${selectedPlaylist.title}", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )
        }

        is OverlayState.TrackActionMenu -> {
            TrackActionMenu(
                targetTrack = overlay.track,
                isCreator = isCreator,
                onDismiss = onDismiss,
                onAddToPlaylist = {
                    viewModel.getAllMePlaylist()
                    onUpdateOverlay(OverlayState.AddToPlaylist(overlay.track.id))
                },
                onDelete = {
                    viewModel.deleteSongFromPlaylist(
                        playlistId.toString(),
                        overlay.track.id.toString()
                    )
                    Toast.makeText(context, "已从歌单删除", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                onCopyId = {
                    setClipboard(context, overlay.track.id.toString(), "id")
                    onDismiss()
                },
                onCopyName = {
                    setClipboard(context, overlay.track.title, "name")
                    onDismiss()
                }
            )
        }

        // 可以在这里扩展 OverlayState.CreatePlaylist 等
        else -> {}
    }
}