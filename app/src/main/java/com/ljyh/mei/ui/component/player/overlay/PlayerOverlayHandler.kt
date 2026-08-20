package com.ljyh.mei.ui.component.player.overlay

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.component.player.PlayerViewModel
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.model.MoreAction
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.ui.screen.playlist.PlaylistTrackAddOutcome

enum class TabletPlayerPanel {
    Lyrics,
    Queue,
    Comments,
}

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
) {
    private val _currentOverlay = mutableStateOf<OverlayState>(OverlayState.None)
    val currentOverlay: State<OverlayState> = _currentOverlay

    private val _tabletPanel = mutableStateOf(TabletPlayerPanel.Lyrics)
    val tabletPanel: State<TabletPlayerPanel> = _tabletPanel

    val currentOverlayValue: OverlayState
        get() = _currentOverlay.value

    /**
     * 显示播放列表弹窗
     */
    fun showPlaylist() {
        _currentOverlay.value = OverlayState.Playlist
    }

    fun toggleInlineQueue() {
        _tabletPanel.value = when (_tabletPanel.value) {
            TabletPlayerPanel.Lyrics -> TabletPlayerPanel.Queue
            TabletPlayerPanel.Queue,
            TabletPlayerPanel.Comments -> TabletPlayerPanel.Lyrics
        }
    }

    fun showLyrics() {
        _tabletPanel.value = TabletPlayerPanel.Lyrics
    }

    fun showInlineComments(): Boolean {
        if (!useInlineQueue) return false
        _tabletPanel.value = TabletPlayerPanel.Comments
        dismiss()
        return true
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
        val uid = stateContainer.playerViewModel.userId
        if (uid.isNotEmpty()) {
            stateContainer.playerViewModel.syncUserPlaylists(uid)
        }
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
    fun showQQMusicSelection(mediaMetadata: MediaMetadata) {
        _currentOverlay.value = OverlayState.QQMusicSelection(mediaMetadata)
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

    fun showSongInfo(metadata: MediaMetadata) {
        _currentOverlay.value = OverlayState.SongInfo(metadata)
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
                mediaMetadata?.let {
                    dismiss()
                    requestNotificationPermission()
                    stateContainer.playerViewModel.downloadSong(it, context)
                }
            }
            MoreAction.DELETE -> {
                mediaMetadata?.let {
                    stateContainer.playerViewModel.deleteSongById(it.id.toString())
                }
            }
            MoreAction.VIEW_PLAYLIST -> {
                if (useInlineQueue) {
                    _tabletPanel.value = TabletPlayerPanel.Queue
                    dismiss()
                } else {
                    showPlaylist()
                }
            }
            MoreAction.SLEEP_TIMER -> {
                showSleepTimer()
            }
            MoreAction.BOTTOM_ACTION -> {
                showBottomAction()
            }
            MoreAction.SONG_INFO -> {
                mediaMetadata?.let {
                    showSongInfo(it)
                }
            }
            else -> {}
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                val activity = context as? Activity ?: return
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
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

    return remember(stateContainer, useInlineQueue) {
        PlayerOverlayHandler(
            stateContainer = stateContainer,
            playlistViewModel = playlistViewModel,
            navController = navController,
            context = context,
            useInlineQueue = useInlineQueue,
        )
    }
}
