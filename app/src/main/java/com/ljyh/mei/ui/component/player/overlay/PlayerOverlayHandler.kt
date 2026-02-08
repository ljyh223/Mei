package com.ljyh.mei.ui.component.player.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.component.player.PlayerViewModel
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.model.MoreAction
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel

/**
 * 播放器弹窗处理器
 * 统一管理所有弹窗的显示逻辑
 */
class PlayerOverlayHandler(
    private val stateContainer: PlayerStateContainer,
    private val playlistViewModel: PlaylistViewModel,
    private val navController: NavController,
    private val context: android.content.Context
) {
    private val _currentOverlay = mutableStateOf<OverlayState>(OverlayState.None)
    val currentOverlay: State<OverlayState> = _currentOverlay

    val currentOverlayValue: OverlayState
        get() = _currentOverlay.value

    /**
     * 显示播放列表弹窗
     */
    fun showPlaylist() {
        _currentOverlay.value = OverlayState.Playlist
    }

    /**
     * 显示睡眠定时弹窗
     */
    fun showSleepTimer() {
        _currentOverlay.value = OverlayState.SleepTimer
    }

    /**
     * 显示添加到播放列表弹窗
     */
    fun showAddToPlaylist(mediaId: Long) {
        _currentOverlay.value = OverlayState.AddToPlaylist(mediaId)
    }

    /**
     * 显示创建播放列表弹窗
     */
    fun showCreatePlaylist() {
        _currentOverlay.value = OverlayState.CreatePlaylist
    }

    /**
     * 显示更多操作弹窗
     */
    fun showMoreAction() {
        _currentOverlay.value = OverlayState.MoreAction
    }

    /**
     * 显示底部设置弹窗
     */
    fun showBottomAction() {
        _currentOverlay.value = OverlayState.BottomAction
    }

    /**
     * 显示专辑艺术家弹窗
     */
    fun showAlbumArtist(album: MediaMetadata.Album, artists: List<MediaMetadata.Artist>, cover: String) {
        _currentOverlay.value = OverlayState.AlbumArtist(album, artists, cover)
    }

    /**
     * 显示QQ音乐选择弹窗
     */
    fun showQQMusicSelection(searchResult: Resource<SearchResult>, mediaMetadata: MediaMetadata) {
        _currentOverlay.value = OverlayState.QQMusicSelection(searchResult, mediaMetadata)
    }

    /**
     * 显示音质选择弹窗
     */
    fun showMusicQualitySelection(current: Int) {
        _currentOverlay.value = OverlayState.MusicQualitySelection(current)
    }

    /**
     * 显示轨道操作菜单
     */
    fun showTrackActionMenu(track: MediaMetadata) {
        _currentOverlay.value = OverlayState.TrackActionMenu(track)
    }

    /**
     * 关闭当前弹窗
     */
    fun dismiss() {
        _currentOverlay.value = OverlayState.None
    }

    /**
     * 处理更多操作点击
     */
    fun handleMoreAction(action: MoreAction) {
        val mediaMetadata = stateContainer.mediaMetadata.value
        when (action) {
            MoreAction.ADD_TO_PLAYLIST -> {
                mediaMetadata?.let {
                    showAddToPlaylist(it.id)
                }
            }
            MoreAction.SHARE -> {
                dismiss()
                android.widget.Toast.makeText(context, "暂未实现", android.widget.Toast.LENGTH_SHORT).show()
            }
            MoreAction.DOWNLOAD -> {
                dismiss()
                android.widget.Toast.makeText(context, "暂未实现", android.widget.Toast.LENGTH_SHORT).show()
            }
            MoreAction.DELETE -> {
                mediaMetadata?.let {
                    stateContainer.playerViewModel.deleteSongById(it.id.toString())
                }
            }
            MoreAction.VIEW_PLAYLIST -> {
                showPlaylist()
            }
            MoreAction.SLEEP_TIMER -> {
                showSleepTimer()
            }
            MoreAction.BOTTOM_ACTION -> {
                showBottomAction()
            }
        }
    }

    /**
     * 添加歌曲到播放列表
     */
    fun addSongToPlaylist(selectedPlaylist: Playlist, mediaId: Long) {
        playlistViewModel.addSongToPlaylist(
            pid = selectedPlaylist.id,
            trackIds = mediaId.toString()
        )
        android.widget.Toast.makeText(
            context,
            "已添加到 ${selectedPlaylist.title}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        timber.log.Timber.tag("Playlist").d("Added song to ${selectedPlaylist.title}")
        dismiss()
    }

    /**
     * 创建新播放列表
     */
    fun createPlaylist(name: String, privacy: Boolean) {
        stateContainer.playerViewModel.createPlaylist(name, privacy)
    }
}

/**
 * 记忆并创建弹窗处理器
 */
@Composable
fun rememberOverlayHandler(
    stateContainer: PlayerStateContainer,
    playlistViewModel: PlaylistViewModel,
    navController: NavController
): PlayerOverlayHandler {
    val context = LocalContext.current

    return remember(stateContainer) {
        PlayerOverlayHandler(
            stateContainer = stateContainer,
            playlistViewModel = playlistViewModel,
            navController = navController,
            context = context
        )
    }
}
