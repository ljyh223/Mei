package com.ljyh.mei.ui.component.player.component.classic

import android.os.Build
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.PlayerHorizontalPadding
import com.ljyh.mei.constants.ProgressBarStyle
import com.ljyh.mei.constants.ProgressBarStyleKey
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.component.player.component.PlayerControls
import com.ljyh.mei.ui.component.player.component.FluidProgressSlider
import com.ljyh.mei.ui.component.player.component.LyricScreen
import com.ljyh.mei.ui.component.player.component.PlayerActionToolbar
import com.ljyh.mei.ui.component.player.component.PlayerProgressSlider
import com.ljyh.mei.ui.component.player.component.classic.component.Cover
import com.ljyh.mei.ui.component.player.component.classic.component.PlayerHeader
import com.ljyh.mei.ui.component.player.overlay.PlayerOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.utils.rememberEnumPreference
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(UnstableApi::class)
@Composable
fun ClassicPhoneLayout(
    stateContainer: PlayerStateContainer,
    overlayHandler: PlayerOverlayHandler,
) {


    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val mediaMetadata by stateContainer.mediaMetadata
    val isPlaying by stateContainer.isPlaying
    val playbackState by stateContainer.playbackState
    val sliderPosition by remember { derivedStateOf { stateContainer.sliderPosition } }
    val duration by remember { derivedStateOf { stateContainer.duration } }
    val lyricLine by remember { derivedStateOf { stateContainer.lyricLine } }


    val progressBarStyle = rememberEnumPreference(
        key = ProgressBarStyleKey,
        defaultValue = ProgressBarStyle.WAVE
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(bottom = 16.dp, top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        mediaMetadata?.let {
            PlayerHeader(
                mediaMetadata = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
                onClick = {
                    overlayHandler.showAlbumArtist(it.album, it.artists, it.coverUrl)
                },
                onMoreClick = {
                    overlayHandler.showMoreAction()
                }
            )
        }

        // Pager (Cover / Lyrics)
        val pagerState = rememberPagerState(pageCount = { 2 })
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = PlayerHorizontalPadding),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        mediaMetadata?.let {
                            Cover(
                                playerConnection = stateContainer.playerConnection,
                                mediaMetadata = it,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }

                1 -> {
                    LyricScreen(
                        lyricData = lyricLine,
                        playerConnection = stateContainer.playerConnection,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = PlayerHorizontalPadding),
                        onClick = {
                            mediaMetadata?.let {
                                if (overlayHandler.currentOverlayValue is OverlayState.None) {
                                    overlayHandler.showQQMusicSelection(
                                        searchResult = stateContainer.playerViewModel.searchResult.value,
                                        mediaMetadata = it
                                    )
                                }
                            }
                        },
                        onLongClick = { source ->
                            if (source == LyricSource.QQMusic && mediaMetadata != null) {
                                stateContainer.playerViewModel.deleteSongById(id = mediaMetadata!!.id.toString())
                                Toast.makeText(context, "已删除QQ音乐歌词", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        controlsVisible = stateContainer.controlsVisible,
                        onToggleControls = {},
                    )
                }
            }
        }


        Spacer(Modifier.height(8.dp))

        // Progress Bar
        if (progressBarStyle.value == ProgressBarStyle.LINEAR) {
            FluidProgressSlider(
                position = sliderPosition.toLong(),
                duration = duration,
                onPositionChange = { newPosition ->
                    stateContainer.playerConnection.player.seekTo(newPosition)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 8.dp)
            )
        } else {
            PlayerProgressSlider(
                position = sliderPosition.toLong(),
                duration = duration,
                isPlaying = isPlaying, // 波浪进度条需要这个参数
                onPositionChange = { newPosition ->
                    stateContainer.playerConnection.player.seekTo(newPosition)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Controls
        PlayerControls(
            playerConnection = stateContainer.playerConnection,
            canSkipPrevious = stateContainer.canSkipPrevious.value,
            canSkipNext = stateContainer.canSkipNext.value,
            isPlaying = isPlaying,
            playbackState = playbackState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding)
        )

        Spacer(Modifier.height(16.dp))

        // Action Toolbar (Queue, Like, Sleep, etc.)
        PlayerActionToolbar(
            modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
            onLyricClick = {
                scope.launch {
                    pagerState.animateScrollToPage(if (pagerState.currentPage == 1) 0 else 1,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )
                }
            },
            onPlaylistClick = {
                overlayHandler.showPlaylist()
            },
            onSleepTimerClick = {
                overlayHandler.showSleepTimer()
            },
            onAddToPlaylistClick = {
                mediaMetadata?.let {
                    overlayHandler.showAddToPlaylist(it.id)
                }
            },
            onMoreClick = {
                overlayHandler.showMoreAction()
            }
        )

        // 底部安全区，防止被手势条遮挡
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
