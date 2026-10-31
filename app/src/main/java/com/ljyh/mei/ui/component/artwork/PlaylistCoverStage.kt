package com.ljyh.mei.ui.component.artwork

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.ui.component.shimmer.SkeletonShimmerHost
import com.ljyh.mei.ui.component.shimmer.skeleton
import kotlinx.coroutines.delay

/** Loading placeholders and artwork share every size, offset, rotation and clipping boundary. */
@Composable
internal fun PlaylistCoverStage(
    covers: List<String>,
    liked: Boolean,
    onCoverClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    clickLabel: String = "预览封面",
    isLoading: Boolean = false,
    placeholderCoverCount: Int = 3,
    animateEntrance: Boolean = true,
) {
    val count = (if (isLoading) maxOf(covers.size, placeholderCoverCount) else covers.size).coerceIn(0, 3)
    val animate = animateEntrance
    BoxWithConstraints(modifier.clipToBounds()) {
        val width = maxWidth
        val height = width * if (liked) 1.02f else 0.65f
        Box(Modifier.fillMaxWidth().height(height)) {
            if (count == 0) {
                CoverFrame(null, "歌曲封面", onCoverClick,
                    Modifier.size(width * 0.54f).align(Alignment.Center), clickLabel, isLoading)
            } else if (liked && count == 1) {
                LikedCoverEntrance(
                    url = covers.getOrNull(0), onCoverClick = onCoverClick, clickLabel = clickLabel,
                    rotation = -10f, dropDistance = width, isLoading = isLoading, animate = animate,
                    modifier = Modifier.size(width * 0.65f).align(Alignment.Center),
                )
            } else if (liked) {
                if (count > 2) LikedCoverEntrance(
                    url = covers.getOrNull(2), onCoverClick = onCoverClick, clickLabel = clickLabel,
                    rotation = 9f, opacity = 0.72f, dropDistance = width,
                    spreadDistance = width * -0.08f, isLoading = isLoading, animate = animate,
                    modifier = Modifier.offset(x = width * 0.45f, y = width * 0.04f).size(width * 0.48f),
                )
                if (count > 1) LikedCoverEntrance(
                    url = covers.getOrNull(1), onCoverClick = onCoverClick, clickLabel = clickLabel,
                    rotation = -15f, dropDistance = width, delayMillis = 80,
                    spreadDistance = width * 0.12f, isLoading = isLoading, animate = animate,
                    modifier = Modifier.offset(x = width * 0.07f, y = width * 0.16f).size(width * 0.55f),
                )
                LikedCoverEntrance(
                    url = covers.getOrNull(0), onCoverClick = onCoverClick, clickLabel = clickLabel,
                    rotation = 17f, dropDistance = width * 1.15f, delayMillis = 150,
                    spreadDistance = width * -0.10f, isLoading = isLoading, animate = animate,
                    modifier = Modifier.offset(x = width * 0.40f, y = width * 0.43f).size(width * 0.48f),
                )
            } else {
                if (count > 1) CoverFrame(covers.getOrNull(1), "第 2 名封面", onCoverClick,
                    Modifier.offset(x = width * 0.04f, y = width * 0.16f).size(width * 0.40f)
                        .graphicsLayer { rotationZ = -7f; alpha = 0.85f }, clickLabel, isLoading)
                if (count > 2) CoverFrame(covers.getOrNull(2), "第 3 名封面", onCoverClick,
                    Modifier.offset(x = width * 0.56f, y = width * 0.16f).size(width * 0.40f)
                        .graphicsLayer { rotationZ = 7f; alpha = 0.85f }, clickLabel, isLoading)
                CoverFrame(covers.getOrNull(0), "第 1 名封面", onCoverClick,
                    Modifier.offset(x = width * 0.23f, y = width * 0.02f).size(width * 0.54f), clickLabel, isLoading)
            }
        }
    }
}

/** The skeleton keeps its final pose; only decoded artwork receives the entrance transform. */
@Composable
private fun LikedCoverEntrance(
    url: String?,
    onCoverClick: (String) -> Unit,
    rotation: Float,
    dropDistance: Dp,
    modifier: Modifier,
    isLoading: Boolean,
    animate: Boolean,
    spreadDistance: Dp = 0.dp,
    opacity: Float = 1f,
    delayMillis: Long = 0,
    clickLabel: String = "预览封面",
) {
    CoverFrame(url, "歌曲封面", onCoverClick, modifier, clickLabel, isLoading,
        rotation = rotation, opacity = opacity,
        entrance = if (animate) ArtworkEntrance(dropDistance, spreadDistance, delayMillis) else null)
}

private data class ArtworkEntrance(val drop: Dp, val spread: Dp, val delayMillis: Long)

internal data class CoverEntranceState(
    val progress: Animatable<Float, AnimationVector1D>,
    val settled: Boolean,
)

/** Image readiness starts the animation; metadata loading and placeholder drawing do not. */
@Composable
internal fun rememberCoverEntranceState(
    contentKey: Any?, ready: Boolean, enabled: Boolean, delayMillis: Long,
): CoverEntranceState {
    var entered by rememberSaveable(contentKey) { mutableStateOf(!enabled) }
    val progress = remember(contentKey) { Animatable(if (entered || !enabled) 1f else 0f) }
    LaunchedEffect(contentKey, ready, enabled, delayMillis) {
        if (!enabled || entered) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        if (!ready) return@LaunchedEffect
        val durationScale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        if (durationScale == 0f) progress.snapTo(1f) else {
            delay((delayMillis * durationScale).toLong())
            progress.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = 65f))
        }
        entered = true
    }
    return CoverEntranceState(progress, entered)
}

@Composable
private fun CoverFrame(
    url: String?,
    label: String,
    onCoverClick: (String) -> Unit,
    modifier: Modifier,
    clickLabel: String,
    isLoading: Boolean,
    rotation: Float = 0f,
    opacity: Float = 1f,
    entrance: ArtworkEntrance? = null,
) {
    val image = url?.takeIf(String::isNotBlank)
    var imageLoading by remember(image) { mutableStateOf(image != null) }
    var imageFailed by remember(image) { mutableStateOf(false) }
    val ready = image != null && !imageLoading && !imageFailed
    val motion = rememberCoverEntranceState(image, ready, entrance != null, entrance?.delayMillis ?: 0)
    val density = LocalDensity.current
    val dropPx = with(density) { (entrance?.drop ?: 0.dp).toPx() }
    val spreadPx = with(density) { (entrance?.spread ?: 0.dp).toPx() }
    val shape = RoundedCornerShape(10.dp)
    Box(modifier, contentAlignment = Alignment.Center) {
        val restingPose = Modifier.matchParentSize().graphicsLayer { rotationZ = rotation; alpha = opacity }
        if (!ready) Box(restingPose.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
        if ((isLoading && image == null) || imageLoading) {
            // This layer never receives the entrance translation or scale.
            SkeletonShimmerHost(enabled = true, modifier = restingPose) {
                Spacer(Modifier.matchParentSize().skeleton(true, shape))
            }
        }
        if (image != null) {
            val interaction = if (!isLoading && !imageFailed) Modifier.clickable(onClickLabel = clickLabel) {
                if (motion.progress.value > 0.5f) onCoverClick(image)
            } else Modifier
            AsyncImage(
                model = image, contentDescription = label.takeUnless { isLoading }, contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().graphicsLayer {
                    val fraction = motion.progress.value
                    val remaining = 1f - fraction
                    translationY = -dropPx * remaining
                    translationX = spreadPx * remaining
                    rotationZ = rotation - if (entrance != null) 12f * remaining else 0f
                    scaleX = if (entrance != null) 0.92f + 0.08f * fraction else 1f
                    scaleY = scaleX
                    alpha = (fraction * 3f).coerceIn(0f, 1f) * opacity
                }.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh).then(interaction),
                onLoading = { imageLoading = true; imageFailed = false },
                onSuccess = { imageLoading = false; imageFailed = false },
                onError = { imageLoading = false; imageFailed = true },
            )
        }
        if (!isLoading && (image == null || imageFailed)) {
            Box(restingPose, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MusicNote, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
