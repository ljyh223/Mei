package com.ljyh.mei.ui.component.player.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ljyh.mei.constants.CoverStyle
import com.ljyh.mei.constants.CoverStyleKey
import com.ljyh.mei.constants.IrregularityCoverKey
import com.ljyh.mei.constants.OriginalCoverKey
import com.ljyh.mei.constants.ThumbnailCornerRadius
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.component.player.PlayerViewModel
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.ljyh.mei.utils.rememberEnumPreference
import com.ljyh.mei.utils.rememberPreference
import com.ljyh.mei.utils.size1600
import com.ljyh.mei.utils.smallImage

@Composable
fun Cover(
    viewModel: PlayerViewModel,
    playerConnection: PlayerConnection,
    mediaMetadata: MediaMetadata,
    modifier: Modifier,
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coverStyle by rememberEnumPreference(CoverStyleKey, defaultValue = CoverStyle.Square)
    val enabledIrregularityCoverKey by rememberPreference(
        IrregularityCoverKey,
        defaultValue = false
    )

    val originalCover by rememberPreference(
        OriginalCoverKey,
        defaultValue = false
    )
    
    var showTrackBottomSheet by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .placeholderMemoryCacheKey(mediaMetadata.coverUrl.smallImage()) // 先用小图占位
                .data(
                    if (originalCover) mediaMetadata.coverUrl else mediaMetadata.coverUrl.size1600()
                )
                .crossfade(true) // 平滑过渡
                .build(),
            contentScale = ContentScale.Crop,
            contentDescription = "Loaded Image",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(
                    when (coverStyle) {
                        CoverStyle.Square -> RoundedCornerShape(ThumbnailCornerRadius * 6)
                        CoverStyle.Circle -> CircleShape
                    }
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2) {
                                playerConnection.player.seekBack()
                            } else {
                                playerConnection.player.seekForward()
                            }
                        }
                    )
                }
        )

//        viewModel.mediaMetadata?.let {
//            TrackBottomSheet(
//                showBottomSheet = showTrackBottomSheet,
//                viewModel = playlistViewModel,
//                mediaMetadata = it,
//            ) {
//                showTrackBottomSheet = false
//            }
//        }
    }
}

