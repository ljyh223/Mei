package com.ljyh.mei.ui.component.player.classic

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil3.request.crossfade
import com.ljyh.mei.constants.CoverStyle
import com.ljyh.mei.constants.CoverStyleKey
import com.ljyh.mei.constants.OriginalCoverKey
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.repository.DynamicCover
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.ui.component.dialog.FullScreenImageViewer
import com.ljyh.mei.ui.component.player.cover.DynamicCoverView
import com.ljyh.mei.ui.component.player.cover.PlayerArtworkImage
import com.ljyh.mei.ui.motion.PlayerMotionSpec
import com.ljyh.mei.utils.image.rememberPlayerArtworkRequest
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference

@OptIn(UnstableApi::class)
@Composable
fun Cover(
    playerConnection: PlayerConnection,
    mediaMetadata: MediaMetadata,
    isPlaying: Boolean,
    dynamicCover: DynamicCover? = null,
    onDynamicCoverError: (MediaMetadata) -> Unit = {},
    modifier: Modifier,
    onDoubleClick: (Offset, Int) -> Unit = { _, _ ->}
) {
    // 获取当前的封面样式设置
    val coverStyle by rememberEnumPreference(CoverStyleKey, defaultValue = CoverStyle.Square)
    val originalCover by rememberPreference(
        OriginalCoverKey,
        defaultValue = false
    )

    val coverShape = if (coverStyle == CoverStyle.Circle) {
        CircleShape
    } else {
        RoundedCornerShape(12.dp)
    }

    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.9f,
        animationSpec = PlayerMotionSpec.CoverPlayStateSpring,
        label = "CoverScale"
    )

    var showFullImage by remember { mutableStateOf(false) }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = coverScale
                scaleY = coverScale
            }
        ) {
            val url = mediaMetadata.coverUrl

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
                    .shadow(12.dp, coverShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                onDoubleClick(offset, size.width)
                            },
                            onLongPress = {
                                showFullImage = true
                            }
                        )
                    },
                shape = coverShape,
                color = Color.Transparent
            ) {
                val activeCover = dynamicCover?.takeIf { it.songId == mediaMetadata.id && url == mediaMetadata.coverUrl }
                Box {
                    PlayerArtworkImage(
                        artworkKey = url,
                        request = rememberPlayerArtworkRequest(url, originalCover)
                            ?.newBuilder()?.crossfade(false)?.build(),
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (activeCover != null) {
                        DynamicCoverView(
                            imageUrl = url,
                            cover = activeCover,
                            originalArtwork = originalCover,
                            playerArtwork = true,
                            playing = isPlaying,
                            onPlaybackError = { onDynamicCoverError(mediaMetadata) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (showFullImage) {
            FullScreenImageViewer(
                imageUrl = mediaMetadata.coverUrl,
                onDismiss = { showFullImage = false }
            )
        }
    }
}
