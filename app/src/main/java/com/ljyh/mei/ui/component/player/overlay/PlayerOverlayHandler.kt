package com.ljyh.mei.ui.component.player.overlay

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.utils.share.MediaShareLink
import com.ljyh.mei.utils.share.shareMediaLink
import com.ljyh.mei.ui.model.PlayerAction
import com.ljyh.mei.ui.model.PlayerActionProfile
import com.ljyh.mei.ui.model.MoreAction
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.ui.screen.playlist.PlaylistTrackAddOutcome


/**
 * 播放器弹窗处理器
 * 统一管理所有弹窗的显示逻辑
 */
@OptIn(UnstableApi::class)
class PlayerOverlayHandler(
    private val stateContainer: PlayerStateContainer,
    private val playlistViewModel: PlaylistViewModel,
    private val navController: NavController,
    private val context: android.content.Context,
    private val useInlineQueue: Boolean,
    private val _panel: MutableState<PlayerPanel> = mutableStateOf(PlayerPanel.Lyrics),
    private val _detailsVisible: MutableState<Boolean> = mutableStateOf(false),
) {
    private val sheetNavigation = PlayerSheetNavigation()
    val currentOverlay: State<OverlayState> = sheetNavigation.current
    val canGoBack: Boolean get() = sheetNavigation.canGoBack
    val isReturning: Boolean get() = sheetNavigation.isReturning

    private fun show(next: OverlayState) = sheetNavigation.show(next)
    fun goBack() = sheetNavigation.goBack()

    val actionProfile = if (useInlineQueue) PlayerActionProfile.Tablet else PlayerActionProfile.Phone
    private val _downloadRequest = mutableStateOf<MediaMetadata?>(null)
    val downloadRequest: State<MediaMetadata?> = _downloadRequest
    fun consumeDownloadRequest() { _downloadRequest.value = null }

    val detailsVisible: State<Boolean> = _detailsVisible
    val panel: State<PlayerPanel> = _panel
    private val _phoneComments = mutableStateOf(false)
    val phoneComments: State<Boolean> = _phoneComments

    val currentOverlayValue: OverlayState
        get() = sheetNavigation.current.value

    /**
     * 显示播放列表弹窗
     */
    fun showPlaylist() {
        show(OverlayState.Playlist)
    }

    fun selectPanel(panel: PlayerPanel) { _panel.value = panel; _detailsVisible.value = true }

    fun toggleInlineQueue() {
        _panel.value = when (_panel.value) {
            PlayerPanel.Lyrics -> PlayerPanel.Queue
            PlayerPanel.Queue,
            PlayerPanel.Comments -> PlayerPanel.Lyrics
        }
    }

    fun showLyrics() {
        _panel.value = PlayerPanel.Lyrics
        _detailsVisible.value = false
        _phoneComments.value = false
    }

    fun showInlineComments() {
        if (useInlineQueue) selectPanel(PlayerPanel.Comments)
        else _phoneComments.value = true
        dismiss()
    }

    /**
     * 显示睡眠定时弹窗
     */
    fun showSleepTimer() {
        show(OverlayState.SleepTimer)
    }

    /**
     * 显示添加到播放列表弹窗
     */
    fun showAddToPlaylist(mediaId: Long) {
        val uid = stateContainer.playerViewModel.userId
        if (uid.isNotEmpty()) {
            stateContainer.playerViewModel.syncUserPlaylists(uid)
        }
        show(OverlayState.AddToPlaylist(mediaId))
    }

    /**
     * 显示创建播放列表弹窗
     */
    fun showCreatePlaylist() {
        show(OverlayState.CreatePlaylist)
    }

    /**
     * 显示更多操作弹窗
     */
    fun showMoreAction() {
        show(OverlayState.MoreAction)
    }

    /**
     * 显示底部设置弹窗
     */
    fun showBottomAction() {
        show(OverlayState.BottomAction)
    }

    /**
     * 显示专辑艺术家弹窗
     */
    fun showAlbumArtist(album: MediaMetadata.Album, artists: List<MediaMetadata.Artist>, cover: String) {
        show(OverlayState.AlbumArtist(album, artists, cover))
    }

    /**
     * 显示QQ音乐选择弹窗
     */
    fun showQQMusicSelection(mediaMetadata: MediaMetadata) {
        show(OverlayState.QQMusicSelection(mediaMetadata))
    }

    fun showLyricSourceSelection(mediaMetadata: MediaMetadata) {
        show(OverlayState.LyricSourceSelection(mediaMetadata))
    }

    /**
     * 显示音质选择弹窗
     */
    fun showMusicQualitySelection() {
        show(OverlayState.MusicQualitySelection)
    }

    /**
     * 显示轨道操作菜单
     */
    fun showTrackActionMenu(track: MediaMetadata) {
        show(OverlayState.TrackActionMenu(track))
    }

    fun showSongInfo(metadata: MediaMetadata) {
        show(OverlayState.SongInfo(metadata))
    }

    /**
     * 关闭当前弹窗
     */
    fun dismiss() = sheetNavigation.dismiss()

    /**
     * 处理更多操作点击
     */
    fun handleMoreAction(action: MoreAction) {
        if (action == MoreAction.BOTTOM_ACTION) showBottomAction()
        else action.playerAction?.let { handlePlayerAction(it, togglePanel = false) }
    }

    /** Toolbar and menu commands share a single execution path; only toolbar panels toggle off. */
    fun handlePlayerAction(action: PlayerAction, togglePanel: Boolean = true) {
        val metadata = stateContainer.mediaMetadata.value
        if (!action.isAvailable(metadata != null, metadata?.neteaseId?.let { it > 0 } == true)) return
        when (action) {
            PlayerAction.PLAY_MODE -> stateContainer.playerConnection.switchPlayMode()
            PlayerAction.LYRICS -> { showLyrics(); _detailsVisible.value = true; dismiss() }
            PlayerAction.QUEUE -> if (useInlineQueue) {
                _panel.value = nextPlayerPanel(_panel.value, PlayerPanel.Queue, togglePanel)
                _detailsVisible.value = _panel.value != PlayerPanel.Lyrics
                dismiss()
            } else showPlaylist()
            PlayerAction.COMMENTS -> if (metadata != null) {
                if (useInlineQueue) {
                    _panel.value = nextPlayerPanel(_panel.value, PlayerPanel.Comments, togglePanel)
                    _detailsVisible.value = _panel.value != PlayerPanel.Lyrics
                    dismiss()
                } else showInlineComments()
            }
            PlayerAction.SLEEP_TIMER -> showSleepTimer()
            PlayerAction.ADD_TO_PLAYLIST -> metadata?.let { showAddToPlaylist(it.id) }
            PlayerAction.DOWNLOAD -> _downloadRequest.value = metadata
            PlayerAction.SHARE -> {
                if (context.shareMediaLink(metadata?.let(MediaShareLink::song))) dismiss()
            }
            PlayerAction.SONG_INFO -> metadata?.let(::showSongInfo)
            PlayerAction.MORE -> showMoreAction()
        }
    }

    /**
     * 添加歌曲到播放列表
     */
    fun addSongToPlaylist(selectedPlaylist: Playlist, mediaId: Long) {
        playlistViewModel.addSongToPlaylist(
            pid = selectedPlaylist.id,
            trackIds = mediaId.toString(),
            previousTrackCount = selectedPlaylist.count
        ) { outcome ->
            val message = when (outcome) {
                PlaylistTrackAddOutcome.Added -> "已添加到 ${selectedPlaylist.title}"
                PlaylistTrackAddOutcome.AlreadyExists -> "歌曲已在 ${selectedPlaylist.title} 中"
                PlaylistTrackAddOutcome.Failed -> "添加到歌单失败"
            }
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            timber.log.Timber.tag("Playlist").d("Add song to ${selectedPlaylist.title}: $outcome")
        }
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
@OptIn(UnstableApi::class)
@Composable
fun rememberOverlayHandler(
    stateContainer: PlayerStateContainer,
    playlistViewModel: PlaylistViewModel,
    navController: NavController,
    useInlineQueue: Boolean,
): PlayerOverlayHandler {
    val context = LocalContext.current
    val panel = rememberPlayerPanelState()
    val detailsVisible = rememberSaveable { mutableStateOf(false) }

    return remember(stateContainer, useInlineQueue, panel, detailsVisible, navController, context) {
        PlayerOverlayHandler(
            stateContainer = stateContainer,
            playlistViewModel = playlistViewModel,
            navController = navController,
            context = context,
            useInlineQueue = useInlineQueue,
            _panel = panel,
            _detailsVisible = detailsVisible,
        )
    }
}
