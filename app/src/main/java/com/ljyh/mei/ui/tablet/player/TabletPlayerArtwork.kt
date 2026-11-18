package com.ljyh.mei.ui.tablet.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.request.crossfade
import com.ljyh.mei.constants.CoverStyle
import com.ljyh.mei.constants.CoverStyleKey
import com.ljyh.mei.constants.OriginalCoverKey
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.repository.DynamicCover
import com.ljyh.mei.ui.component.dialog.FullScreenImageViewer
import com.ljyh.mei.ui.component.player.cover.DynamicCoverView
import com.ljyh.mei.ui.component.player.cover.CoverEdgeArtwork
import com.ljyh.mei.ui.component.player.cover.coverEdgeFeather
import com.ljyh.mei.ui.component.player.cover.fillArtworkEdges
import com.ljyh.mei.ui.component.player.cover.PlayerArtworkImage
import com.ljyh.mei.ui.motion.PlayerMotionSpec
import com.ljyh.mei.utils.image.rememberPlayerArtworkRequest
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference
import kotlin.math.roundToInt

private data class RetainedDynamicArtwork(val cover: DynamicCover, val imageUrl: String)

/** Uses the phone's shared image renderer and request policy, including its pixel-edge fix. */
@Composable
internal fun TabletPlayerArtwork(
    metadata: MediaMetadata?, playing: Boolean, dynamicCover: DynamicCover?, modifier: Modifier,
    onDynamicError: () -> Unit,
    edgeArtwork: CoverEdgeArtwork? = null, edgeAlpha: Float = 0f,
    onPrevious: () -> Unit = {}, onNext: () -> Unit = {},
    playbackScale: Float? = null,
    interactive: Boolean = true,
    keepCoverVisible: Boolean = false,
    poster: Painter? = null,
    onPainterReady: (String, Painter) -> Unit = { _, _ -> },
    landscapeExpansion: Float = 0f,
    retainDynamicOnTrackChange: Boolean = false,
) {
    val original by rememberPreference(OriginalCoverKey, false)
    val style by rememberEnumPreference(CoverStyleKey, CoverStyle.Square)
    val frame = (1f - landscapeExpansion) * (1f - edgeAlpha.coerceIn(0f, 1f))
    val artworkShape = when {
        style == CoverStyle.Circle -> RoundedCornerShape((50 * frame).roundToInt())
        else -> RoundedCornerShape(12.dp * frame)
    }
    val scale = playbackScale ?: animateFloatAsState(if (playing) 1f else .9f,
        animationSpec = PlayerMotionSpec.CoverPlayStateSpring, label = "TabletCoverScale").value
    val currentMotion = dynamicCover?.takeIf { it.songId == metadata?.id }
    var retainedMotion by remember { mutableStateOf<RetainedDynamicArtwork?>(null) }
    val shownMotion = currentMotion?.let { RetainedDynamicArtwork(it, metadata?.coverUrl.orEmpty()) }
        ?: retainedMotion.takeIf { retainDynamicOnTrackChange }
    SideEffect {
        if (!retainDynamicOnTrackChange || currentMotion?.source == DynamicCover.Source.NETEASE) {
            retainedMotion = null
        } else if (currentMotion != null) {
            retainedMotion = shownMotion
        }
    }
    var preview by remember { mutableStateOf(false) }
    val feather = if (landscapeExpansion > 0f) Modifier.landscapeArtworkFade { landscapeExpansion } else Modifier
    Box(modifier, contentAlignment = Alignment.Center) {
        if (metadata != null) Box(Modifier.fillMaxSize().graphicsLayer {
            scaleX = scale
            scaleY = scale
            shape = artworkShape
            clip = true
            shadowElevation = (12.dp * frame).toPx()
        }.then(feather).coverEdgeFeather(edgeArtwork, edgeAlpha).pointerInput(metadata.coverUrl, interactive) {
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
            val request = rememberPlayerArtworkRequest(metadata.coverUrl, original)
            val artworkRequest = remember(request, keepCoverVisible) {
                if (keepCoverVisible) request?.newBuilder()?.crossfade(false)?.build() else request
            }
            var artworkLoaded by remember(artworkRequest) { mutableStateOf(false) }
            var artworkResolved by remember(artworkRequest) { mutableStateOf(artworkRequest == null) }
            var motionExitStarted by remember(shownMotion?.cover?.songId, shownMotion?.cover?.cacheKey, shownMotion?.cover?.url) {
                mutableStateOf(false)
            }
            val retiringMotion = shownMotion != null && currentMotion == null
            SideEffect { if (retiringMotion && artworkResolved) motionExitStarted = true }
            val motionOpacity by animateFloatAsState(
                if (retiringMotion && motionExitStarted) 0f else 1f,
                animationSpec = PlayerMotionSpec.tween(PlayerMotionSpec.CoverSwapDurationMillis),
                label = "LandscapeMotionToStaticArtwork",
                finishedListener = { alpha ->
                    if (alpha == 0f && retainedMotion == shownMotion) retainedMotion = null
                },
            )
            if (keepCoverVisible && !artworkLoaded) {
                if (poster != null) {
                    Image(
                        painter = poster,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().fillArtworkEdges(),
                        contentScale = ContentScale.Crop,
                    )
                } else Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PlayerArtworkImage(
                metadata.coverUrl, artworkRequest, Modifier.fillMaxSize(),
                onSuccess = { artworkLoaded = true; artworkResolved = true },
                onError = { artworkLoaded = false; artworkResolved = true },
                onPainterReady = { onPainterReady(metadata.coverUrl, it) },
            )
            if (shownMotion != null) DynamicCoverView(
                imageUrl = shownMotion.imageUrl, cover = shownMotion.cover,
                playing = playing && currentMotion != null,
                originalArtwork = original, playerArtwork = true,
                fadeFromStaticArtwork = retainDynamicOnTrackChange,
                onPlaybackError = {
                    if (currentMotion?.songId == metadata.id) onDynamicError()
                    else retainedMotion = null
                },
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = motionOpacity },
            )
        }
    }
    if (preview && metadata != null) FullScreenImageViewer(metadata.coverUrl) { preview = false }
}

/** Feather the video and poster together into the live mesh drawn behind the player. */
internal fun Modifier.landscapeArtworkFade(opacity: () -> Float = { 1f }) =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithCache {
        val endOnRight = layoutDirection == LayoutDirection.Ltr
        val seam = Brush.horizontalGradient(
            0.62f to Color.Transparent,
            0.76f to Color.Black.copy(alpha = 0.35f),
            0.89f to Color.Black.copy(alpha = 0.87f),
            0.975f to Color.Black,
            startX = if (endOnRight) 0f else size.width,
            endX = if (endOnRight) size.width else 0f,
        )
        val top = minOf(36.dp.toPx(), size.height / 4f)
        val bottom = minOf(28.dp.toPx(), size.height / 4f)
        val statusScrim = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent), endY = top)
        val gestureScrim = Brush.verticalGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
            startY = size.height - bottom, endY = size.height,
        )
        val lowerEdge = Brush.verticalGradient(
            0.78f to Color.Transparent,
            0.9f to Color.Black.copy(alpha = 0.28f),
            1f to Color.Black.copy(alpha = 0.72f),
            endY = size.height,
        )
        onDrawWithContent {
            drawContent()
            val alpha = opacity()
            if (alpha > 0f && size.width > 0f && size.height > 0f) {
                drawRect(statusScrim, size = Size(size.width, top), alpha = alpha)
                drawRect(gestureScrim, topLeft = Offset(0f, size.height - bottom),
                    size = Size(size.width, bottom), alpha = alpha)
                drawRect(lowerEdge, alpha = alpha, blendMode = BlendMode.DstOut)
                drawRect(seam, alpha = alpha, blendMode = BlendMode.DstOut)
            }
        }
    }
