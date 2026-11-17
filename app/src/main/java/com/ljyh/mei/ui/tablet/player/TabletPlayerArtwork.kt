package com.ljyh.mei.ui.tablet.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
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
import kotlin.math.roundToInt

/** Uses the phone's shared image renderer and request policy, including its pixel-edge fix. */
@Composable
internal fun TabletPlayerArtwork(
    metadata: MediaMetadata?, playing: Boolean, dynamicCover: DynamicCover?, modifier: Modifier,
    onDynamicError: () -> Unit,
    edgeArtwork: CoverEdgeArtwork? = null, edgeAlpha: Float = 0f,
    onPrevious: () -> Unit = {}, onNext: () -> Unit = {},
    playbackScale: Float? = null,
    interactive: Boolean = true,
    onPainterReady: (String, Painter) -> Unit = { _, _ -> },
) {
    val original by rememberPreference(OriginalCoverKey, false)
    val style by rememberEnumPreference(CoverStyleKey, CoverStyle.Square)
    val frame = 1f - edgeAlpha.coerceIn(0f, 1f)
    val artworkShape = if (style == CoverStyle.Circle) RoundedCornerShape((50 * frame).roundToInt())
    else RoundedCornerShape(12.dp * frame)
    val scale = playbackScale ?: animateFloatAsState(if (playing) 1f else .9f,
        animationSpec = PlayerMotionSpec.CoverPlayStateSpring, label = "TabletCoverScale").value
    var preview by remember { mutableStateOf(false) }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (metadata != null) Box(Modifier.fillMaxSize().graphicsLayer {
            scaleX = scale
            scaleY = scale
            shape = artworkShape
            clip = true
            shadowElevation = (12.dp * frame).toPx()
        }.coverEdgeFeather(edgeArtwork, edgeAlpha).pointerInput(metadata.coverUrl, interactive) {
                if (interactive) detectTapGestures(onLongPress = { preview = true })
            }.pointerInput(onPrevious, onNext, interactive) {
                var distance = 0f
                if (interactive) detectHorizontalDragGestures(
                    onDragStart = { distance = 0f },
                    onHorizontalDrag = { change, amount -> change.consume(); distance += amount },
                    onDragEnd = {
                        val threshold = 48.dp.toPx()
                        if (distance > threshold) onPrevious() else if (distance < -threshold) onNext()
                    },
                )
            }) {
            PlayerArtworkImage(metadata.coverUrl, rememberPlayerArtworkRequest(metadata.coverUrl, original),
                Modifier.fillMaxSize(),
                onPainterReady = { onPainterReady(metadata.coverUrl, it) })
            if (dynamicCover?.songId == metadata.id) DynamicCoverView(
                imageUrl = metadata.coverUrl, cover = dynamicCover, playing = playing,
                originalArtwork = original, playerArtwork = true,
                onPlaybackError = { onDynamicError() }, modifier = Modifier.fillMaxSize())
        }
    }
    if (preview && metadata != null) FullScreenImageViewer(metadata.coverUrl) { preview = false }
}
