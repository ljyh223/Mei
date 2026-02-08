package com.ljyh.mei.ui.component.player.overlay

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.component.player.PlayerViewModel
import com.ljyh.mei.ui.component.player.component.sheet.AlbumArtistBottomSheet
import com.ljyh.mei.ui.component.player.component.sheet.MoreActionsSheet
import com.ljyh.mei.ui.component.player.component.sheet.PlayerActionSettingsSheet
import com.ljyh.mei.ui.component.player.component.sheet.PlaylistBottomSheet
import com.ljyh.mei.ui.component.player.component.sheet.QQMusicSelectSheet
import com.ljyh.mei.ui.component.player.component.sheet.SleepTimerSheet
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.component.playlist.AddToPlaylistSheet
import com.ljyh.mei.ui.component.playlist.CreatePlaylistSheet
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.model.MoreAction
import com.ljyh.mei.ui.screen.Screen
import timber.log.Timber

/**
 * 公共弹窗处理器 UI
 * 统一渲染所有弹窗组件，提取约150行重复代码
 */
@UnstableApi
@Composable
fun CommonOverlayHandler(
    overlayHandler: PlayerOverlayHandler,
    stateContainer: PlayerStateContainer,
    sheetState: BottomSheetState? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val playerViewModel = stateContainer.playerViewModel

    when (val overlay = overlayHandler.currentOverlayValue) {
        OverlayState.None -> {}

        OverlayState.Playlist -> {
            PlaylistBottomSheet(
                onDismiss = { overlayHandler.dismiss() }
            )
        }

        is OverlayState.AlbumArtist -> {
            AlbumArtistBottomSheet(
                coverUrl = overlay.cover,
                albumInfo = overlay.album,
                artistList = overlay.artists,
                onAlbumClick = { id ->
                    Screen.Album.navigate(navController) {
                        addPath(id.toString())
                    }
                    sheetState?.collapse(spring(stiffness = Spring.StiffnessVeryLow))
                },
                onArtistClick = { id ->
                    Screen.Artist.navigate(navController) {
                        addPath(id.toString())
                    }
                    sheetState?.collapse(spring(stiffness = Spring.StiffnessVeryLow))
                },
                onDismissRequest = { overlayHandler.dismiss() },
            )
        }

        is OverlayState.QQMusicSelection -> {
            QQMusicSelectSheet(
                searchNew = overlay.searchResult,
                viewmodel = playerViewModel,
                mediaMetadata = overlay.mediaMetadata,
                onDismiss = { overlayHandler.dismiss() }
            )
        }

        OverlayState.SleepTimer -> {
            SleepTimerSheet(
                playerConnection = stateContainer.playerConnection,
                onDismiss = { overlayHandler.dismiss() }
            )
        }

        is OverlayState.AddToPlaylist -> {
            AddToPlaylistSheet(
                playlists = stateContainer.allPlaylist.value,
                onDismiss = { overlayHandler.dismiss() },
                onSelectPlaylist = { selectedPlaylist ->
                    overlayHandler.addSongToPlaylist(selectedPlaylist, overlay.mediaId)
                },
                onCreateNewPlaylist = {
                    overlayHandler.showCreatePlaylist()
                }
            )
        }

        OverlayState.CreatePlaylist -> {
            CreatePlaylistSheet(
                onDismiss = { overlayHandler.dismiss() },
                onConfirm = { name, privacy ->
                    overlayHandler.createPlaylist(name, privacy)
                }
            )
        }

        OverlayState.BottomAction -> {
            PlayerActionSettingsSheet(onDismiss = { overlayHandler.dismiss() })
        }

        OverlayState.MoreAction -> {
            MoreActionsSheet(
                onDismissRequest = {
                    overlayHandler.dismiss()
                },
                onActionClick = { action ->
                    overlayHandler.handleMoreAction(action)
                },
                viewModel = playerViewModel
            )
        }

        is OverlayState.MusicQualitySelection -> {
            // TODO: 实现音质选择弹窗
            Toast.makeText(context, "音质选择: ${overlay.current}", Toast.LENGTH_SHORT).show()
        }

        is OverlayState.TrackActionMenu -> {
            // TODO: 实现轨道操作菜单
            Toast.makeText(context, "轨道操作: ${overlay.track.title}", Toast.LENGTH_SHORT).show()
        }
    }
}
