package com.ljyh.mei.ui.component.player.component.classic

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.constants.ProgressBarStyle
import com.ljyh.mei.constants.ProgressBarStyleKey
import com.ljyh.mei.ui.component.player.MiniPlayer
import com.ljyh.mei.ui.component.player.component.AppleMusicFluidBackground
import com.ljyh.mei.ui.component.player.component.Debug
import com.ljyh.mei.ui.component.player.overlay.PlayerOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.component.sheet.BottomSheet
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.sheet.HorizontalSwipeDirection
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.utils.TimeUtils.formatMilliseconds
import com.ljyh.mei.utils.rememberEnumPreference
import com.ljyh.mei.utils.rememberPreference
import timber.log.Timber

@SuppressLint("ConfigurationScreenWidthHeight")
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(UnstableApi::class)
@Composable
fun ClassicPlayer(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    stateContainer: PlayerStateContainer,
    overlayHandler: PlayerOverlayHandler,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val device = rememberDeviceInfo()

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val debug by rememberPreference(DebugKey, defaultValue = false)


    // --- 从状态容器获取数据 ---
    val mediaMetadata by stateContainer.mediaMetadata
    val isPlaying by stateContainer.isPlaying
    val playbackState by stateContainer.playbackState
    val sliderPosition by remember { derivedStateOf { stateContainer.sliderPosition } }
    val duration by remember { derivedStateOf { stateContainer.duration } }
    val isDragging by remember { derivedStateOf { stateContainer.isDragging } }
    val lyricLine by remember { derivedStateOf { stateContainer.lyricLine } }
    val qqSong by stateContainer.qqSong

    // 背景颜色计算
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = remember(isSystemInDarkTheme, state.value, state.collapsedBound) {
        if (isSystemInDarkTheme && state.value > state.collapsedBound) {
            lerp(colorScheme.surfaceContainer, Color.Black, state.progress)
        } else {
            colorScheme.surfaceContainer
        }
    }




    BottomSheet(
        state = state,
        modifier = modifier,
        backgroundColor = backgroundColor,
        onDismiss = {
            stateContainer.playerConnection.player.stop()
            stateContainer.playerConnection.player.clearMediaItems()
        },
        onHorizontalSwipe = { direction ->
            when (direction) {
                HorizontalSwipeDirection.Left -> stateContainer.playerConnection.seekToNext()
                HorizontalSwipeDirection.Right -> stateContainer.playerConnection.seekToPrevious()
            }
        },
        collapsedContent = {
            MiniPlayer(
                position = sliderPosition.toLong(),
                duration = duration,
            )
        }
    ) {

        val coverUrl = mediaMetadata?.coverUrl
        AppleMusicFluidBackground(
            imageUrl = coverUrl
        )

        // Debug 信息层
        if (debug && mediaMetadata != null) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                Debug(
                    title = mediaMetadata!!.title,
                    artist = mediaMetadata!!.artists.firstOrNull()?.name ?: "",
                    album = mediaMetadata!!.album.title,
                    duration = formatMilliseconds(duration).toString(),
                    id = mediaMetadata!!.id.toString(),
                    qid = qqSong?.qid ?: "null",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                )
            }
        }




        val layoutMode = when {
            device.isTablet && device.isLandscape -> PlayerLayoutMode.Tablet
            !device.isTablet && device.isLandscape -> PlayerLayoutMode.ImmersiveLandscape
            else -> PlayerLayoutMode.PhonePortrait
        }

        Timber.tag("PlayerLayoutMode").d(layoutMode.name)


        when (layoutMode) {
            PlayerLayoutMode.PhonePortrait -> ClassicPhoneLayout(stateContainer, overlayHandler)
            PlayerLayoutMode.Tablet -> ClassicTabletLayout(stateContainer, overlayHandler)
            PlayerLayoutMode.ImmersiveLandscape -> ClassicImmersiveLayout(stateContainer, overlayHandler)
        }


    }
}
