package com.ljyh.mei.ui.tablet.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.CoverStyle
import com.ljyh.mei.constants.CoverStyleKey
import com.ljyh.mei.constants.OriginalCoverKey
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.repository.DynamicCover
import com.ljyh.mei.ui.component.dialog.FullScreenImageViewer
import com.ljyh.mei.ui.component.player.cover.DynamicCoverView
import com.ljyh.mei.ui.component.player.cover.CoverEdgeArtwork
import com.ljyh.mei.ui.component.player.cover.coverEdgeFeather
import com.ljyh.mei.ui.component.player.cover.PlayerArtworkImage
import com.ljyh.mei.ui.motion.PlayerMotionSpec
import com.ljyh.mei.utils.image.rememberPlayerArtworkRequest
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference

/** Uses the phone's shared image renderer and request policy, including its pixel-edge fix. */
@Composable
internal fun TabletPlayerArtwork(
    metadata: MediaMetadata?, playing: Boolean, dynamicCover: DynamicCover?, modifier: Modifier,
    onReady: (String) -> Unit, onDynamicError: () -> Unit,
    edgeArtwork: CoverEdgeArtwork? = null, edgeAlpha: Float = 0f,
    onPrevious: () -> Unit = {}, onNext: () -> Unit = {},
) {
    val original by rememberPreference(OriginalCoverKey, false)
    val style by rememberEnumPreference(CoverStyleKey, CoverStyle.Square)
    val shape = if (style == CoverStyle.Circle) CircleShape else RoundedCornerShape(12.dp)
    val scale by animateFloatAsState(if (playing) 1f else .9f,
        animationSpec = PlayerMotionSpec.CoverPlayStateSpring, label = "TabletCoverScale")
    var preview by remember { mutableStateOf(false) }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (metadata != null) Box(Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(12.dp, shape).clip(shape).coverEdgeFeather(edgeArtwork, edgeAlpha).pointerInput(metadata.coverUrl) {
                detectTapGestures(onLongPress = { preview = true })
            }.pointerInput(onPrevious, onNext) {
                var distance = 0f
                detectHorizontalDragGestures(
                    onDragStart = { distance = 0f },
                    onHorizontalDrag = { change, amount -> change.consume(); distance += amount },
                    onDragEnd = {
                        val threshold = 48.dp.toPx()
                        if (distance > threshold) onPrevious() else if (distance < -threshold) onNext()
                    },
                )
            }) {
            PlayerArtworkImage(metadata.coverUrl, rememberPlayerArtworkRequest(metadata.coverUrl, original),
                Modifier.fillMaxSize(), onSuccess = { onReady(metadata.coverUrl) })
            if (dynamicCover?.songId == metadata.id) DynamicCoverView(
                imageUrl = metadata.coverUrl, cover = dynamicCover, playing = playing,
                originalArtwork = original, playerArtwork = true,
                onPlaybackError = { onDynamicError() }, modifier = Modifier.fillMaxSize())
        }
    }
    if (preview && metadata != null) FullScreenImageViewer(metadata.coverUrl) { preview = false }
}
