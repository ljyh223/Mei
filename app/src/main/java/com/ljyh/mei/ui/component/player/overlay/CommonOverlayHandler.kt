package com.ljyh.mei.ui.component.player.overlay

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.DownloadPathKey
import com.ljyh.mei.constants.DownloadQuality
import com.ljyh.mei.constants.DownloadQualityKey
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.download.DownloadManager
import com.ljyh.mei.ui.component.dialog.DownloadConfirmDialog
import com.ljyh.mei.ui.component.player.sheet.MoreActionsSheet
import com.ljyh.mei.ui.component.player.sheet.LyricSourceSheet
import com.ljyh.mei.ui.component.player.sheet.MusicQualitySheet
import com.ljyh.mei.ui.component.player.sheet.PlayerActionSettingsSheet
import com.ljyh.mei.ui.component.player.sheet.PlaylistBottomSheet
import com.ljyh.mei.ui.component.player.sheet.QQMusicSelectSheet
import com.ljyh.mei.ui.component.player.sheet.SleepTimerSheet
import com.ljyh.mei.ui.component.player.sheet.SongInfoSheet
import com.ljyh.mei.ui.component.sheet.AlbumArtistBottomSheet
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.component.sheet.AddToPlaylistSheet
import com.ljyh.mei.ui.component.sheet.CreatePlaylistSheet
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.model.MoreAction
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.model.LyricSelectionState
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference
import kotlinx.coroutines.launch

/**
 * 公共弹窗处理器 UI
 * 统一渲染所有弹窗组件，提取约150行重复代码
 */
@RequiresApi(Build.VERSION_CODES.R)
@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
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
    val (downloadPath) = rememberPreference(DownloadPathKey, DownloadManager.getDefaultDownloadPath())
    val (downloadQuality) = rememberEnumPreference(DownloadQualityKey, DownloadQuality.EXHIGH)
    var pendingDownload by remember { mutableStateOf<MediaMetadata?>(null) }
    val navigateFromPlayer: (() -> Unit) -> Unit = { navigate ->
        overlayHandler.dismiss()
        sheetState?.collapseThen(navigate) ?: navigate()
    }

    val requestedDownload by overlayHandler.downloadRequest
    val overlay = overlayHandler.currentOverlayValue
    val isSheet = overlay != OverlayState.None && overlay !is OverlayState.TrackActionMenu
    val moreEditing = remember { mutableStateOf(false) }
    LaunchedEffect(overlay) {
        if (overlay != OverlayState.MoreAction) moreEditing.value = false
    }

    // Direct toolbar downloads and menu downloads both reach the same confirmation dialog.
    LaunchedEffect(requestedDownload, isSheet) {
        if (!isSheet && requestedDownload != null) {
            pendingDownload = requestedDownload
            overlayHandler.consumeDownloadRequest()
        }
    }

    if (isSheet) {
        val dismissBlocked = rememberUpdatedState(overlay == OverlayState.MoreAction && moreEditing.value)
        val modalState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { next -> next != SheetValue.Hidden || !dismissBlocked.value },
        )
        val scope = rememberCoroutineScope()
        LaunchedEffect(requestedDownload) {
            if (requestedDownload != null) {
                modalState.hide()
                overlayHandler.dismiss()
            }
        }
        val closeSheet: () -> Unit = {
            scope.launch {
                modalState.hide()
                overlayHandler.dismiss()
            }
        }
        val returning = overlayHandler.isReturning
        val canGoBack = overlayHandler.canGoBack

        ModalBottomSheet(
            onDismissRequest = {
                if (overlay == OverlayState.MoreAction && moreEditing.value) moreEditing.value = false
                else closeSheet()
            },
            sheetState = modalState,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            dragHandle = {
                Box(Modifier.fillMaxWidth().height(48.dp)) {
                    BottomSheetDefaults.DragHandle(Modifier.align(Alignment.Center))
                    if (canGoBack) {
                        IconButton(
                            onClick = overlayHandler::goBack,
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(48.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回上一弹层")
                        }
                    }
                }
            },
        ) {
            BackHandler(enabled = canGoBack && !moreEditing.value) { overlayHandler.goBack() }
            AnimatedContent(
                targetState = overlay,
                modifier = Modifier.fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.88f),
                transitionSpec = {
                    val direction = if (returning) -1 else 1
                    (fadeIn(tween(180)) + slideInHorizontally(tween(220)) { direction * it / 10 }) togetherWith
                        (fadeOut(tween(140)) + slideOutHorizontally(tween(220)) { -direction * it / 10 })
                },
                label = "播放器弹层切换",
            ) { visible ->
                when (visible) {
                    OverlayState.Playlist -> PlaylistBottomSheet(onDismiss = closeSheet, embedded = true)

                    is OverlayState.AlbumArtist -> AlbumArtistBottomSheet(
                        coverUrl = visible.cover,
                        albumInfo = visible.album,
                        artistList = visible.artists,
                        onAlbumClick = { id ->
                            navigateFromPlayer { Screen.Album.navigate(navController) { addPath(id.toString()) } }
                        },
                        onArtistClick = { id ->
                            navigateFromPlayer { Screen.Artist.navigate(navController) { addPath(id.toString()) } }
                        },
                        onDismissRequest = closeSheet,
                        embedded = true,
                    )

                    is OverlayState.SongInfo -> {
                        val qqSongId by produceState<String?>(null, visible.metadata.id) {
                            value = playerViewModel.getQQSongId(visible.metadata.id)
                        }
                        val activeDynamicCover by playerViewModel.dynamicCover.collectAsState()
                        SongInfoSheet(
                            metadata = visible.metadata,
                            qqSongId = qqSongId,
                            dynamicCover = activeDynamicCover?.takeIf { it.songId == visible.metadata.id },
                            onDismissRequest = closeSheet,
                            embedded = true,
                        )
                    }

                    is OverlayState.QQMusicSelection -> QQMusicSelectSheet(
                        viewmodel = playerViewModel,
                        mediaMetadata = visible.mediaMetadata,
                        onDismiss = closeSheet,
                        embedded = true,
                    )

                    is OverlayState.LyricSourceSelection -> {
                        val selection by playerViewModel.lyricSelection.collectAsState()
                        LyricSourceSheet(
                            state = selection.takeIf { it.songId == visible.mediaMetadata.id.toString() }
                                ?: LyricSelectionState(songId = visible.mediaMetadata.id.toString()),
                            onSelect = { variant ->
                                playerViewModel.selectLyricVariant(visible.mediaMetadata.id.toString(), variant)
                                closeSheet()
                            },
                            onFindQqLyrics = {
                                playerViewModel.searchQQSong(visible.mediaMetadata.title)
                                overlayHandler.showQQMusicSelection(visible.mediaMetadata)
                            },
                        )
                    }

                    OverlayState.SleepTimer -> SleepTimerSheet(
                        playerConnection = stateContainer.playerConnection,
                        onDismiss = closeSheet,
                        embedded = true,
                    )

                    OverlayState.MusicQualitySelection -> MusicQualitySheet(onDismiss = closeSheet)

                    is OverlayState.AddToPlaylist -> AddToPlaylistSheet(
                        playlists = stateContainer.myPlaylist.value,
                        onDismiss = closeSheet,
                        onSelectPlaylist = { selectedPlaylist ->
                            overlayHandler.addSongToPlaylist(selectedPlaylist, visible.mediaId)
                        },
                        onCreateNewPlaylist = overlayHandler::showCreatePlaylist,
                        embedded = true,
                    )

                    OverlayState.CreatePlaylist -> CreatePlaylistSheet(
                        onDismiss = closeSheet,
                        onConfirm = overlayHandler::createPlaylist,
                        embedded = true,
                    )

                    OverlayState.BottomAction -> PlayerActionSettingsSheet(
                        onDismiss = closeSheet,
                        embedded = true,
                        profile = overlayHandler.actionProfile,
                    )

                    OverlayState.MoreAction -> MoreActionsSheet(
                        onDismissRequest = closeSheet,
                        onActionClick = overlayHandler::handleMoreAction,
                        embedded = true,
                        editingState = moreEditing,
                        profile = overlayHandler.actionProfile,
                    )

                    else -> Unit
                }
            }
        }
    } else {
        when (overlay) {
            is OverlayState.TrackActionMenu -> LaunchedEffect(overlay) {
                Toast.makeText(context, "轨道操作: ${overlay.track.title}", Toast.LENGTH_SHORT).show()
                overlayHandler.dismiss()
            }
            else -> Unit
        }
    }

    pendingDownload?.let { song ->
        DownloadConfirmDialog(
            currentQuality = downloadQuality,
            downloadPath = downloadPath,
            onDismiss = { pendingDownload = null },
            onConfirm = { saveSeparateLyrics ->
                pendingDownload = null
                playerViewModel.downloadSong(song, context, saveSeparateLyrics)
            },
            onGoToSettings = {
                pendingDownload = null
                navigateFromPlayer { Screen.DownloadSettings.navigate(navController) }
            },
            onGoToDownloadManage = {
                pendingDownload = null
                navigateFromPlayer { Screen.DownloadManage.navigate(navController) }
            },
        )
    }
}
