package com.ljyh.mei.ui.component.player.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    playerConnection: PlayerConnection,
    mediaMetadata: MediaMetadata,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val coverStyle by rememberEnumPreference(CoverStyleKey, defaultValue = CoverStyle.Square)
    val originalCover by rememberPreference(
        OriginalCoverKey,
        defaultValue = false
    )
    AnimatedContent(
        modifier=modifier,
        targetState = mediaMetadata.coverUrl,
        transitionSpec = {
            fadeIn(tween(500)) togetherWith fadeOut(tween(500))
        },
        label = "CoverTransition"
    ){ url ->
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(12.dp))
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
                    },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ){
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .placeholderMemoryCacheKey(url.smallImage()) // 先用小图占位
                        .data(
                            if (originalCover) url else url.size1600()
                        )
                        .crossfade(false)// 平滑过渡
                        .build(),
                    contentScale = ContentScale.Crop,
                    contentDescription = "Loaded Image",
                    modifier = Modifier
                        .fillMaxWidth()

                )
            }

        }
    }
    

}

