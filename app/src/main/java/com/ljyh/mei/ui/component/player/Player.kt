package com.ljyh.mei.ui.component.player

import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Size
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.constants.PlayerHorizontalPadding
import com.ljyh.mei.constants.PlayerStyle
import com.ljyh.mei.constants.PlayerStyleKey
import com.ljyh.mei.constants.ThumbnailCornerRadius
import com.ljyh.mei.constants.UseQQMusicLyricKey
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.player.component.AppleMusicFluidBackground
import com.ljyh.mei.ui.component.player.component.LyricScreen
import com.ljyh.mei.ui.component.player.component.PlayerControlsSection
import com.ljyh.mei.ui.component.player.component.Title
import com.ljyh.mei.ui.component.player.component.applemusic.AppleMusicPlayer
import com.ljyh.mei.ui.component.player.component.classic.ClassicPlayer
import com.ljyh.mei.ui.component.player.component.sheet.AlbumArtistBottomSheet
import com.ljyh.mei.ui.component.player.component.sheet.MoreActionsSheet
import com.ljyh.mei.ui.component.player.component.sheet.PlayerActionSettingsSheet
import com.ljyh.mei.ui.component.player.component.sheet.PlaylistBottomSheet
import com.ljyh.mei.ui.component.player.component.sheet.QQMusicSelectSheet
import com.ljyh.mei.ui.component.player.component.sheet.SleepTimerSheet
import com.ljyh.mei.ui.component.playlist.AddToPlaylistSheet
import com.ljyh.mei.ui.component.playlist.CreatePlaylistSheet
import com.ljyh.mei.ui.component.sheet.BottomSheet
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.sheet.HorizontalSwipeDirection
import com.ljyh.mei.ui.component.utils.lerp
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.ui.model.MoreAction
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.utils.UnitUtils.toPx
import com.ljyh.mei.utils.encrypt.QRCUtils
import com.ljyh.mei.utils.lyric.createDefaultLyricData
import com.ljyh.mei.utils.lyric.mergeLyrics
import com.ljyh.mei.utils.rememberEnumPreference
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.min

@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
) {

    val playerStyle by rememberEnumPreference(PlayerStyleKey, defaultValue = PlayerStyle.AppleMusic)
    when(playerStyle){
        PlayerStyle.AppleMusic -> {
            AppleMusicPlayer(
                playerViewModel = playerViewModel,
                playlistViewModel = playlistViewModel,
                modifier = modifier,
                state = state
            )

        }
        PlayerStyle.Classic -> {
            ClassicPlayer(
                playerViewModel = playerViewModel,
                playlistViewModel = playlistViewModel,
                modifier = modifier,
                state = state
            )
        }
    }


}