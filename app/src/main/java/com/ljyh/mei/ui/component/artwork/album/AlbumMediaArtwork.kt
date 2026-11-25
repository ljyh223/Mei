package com.ljyh.mei.ui.component.artwork.album

import androidx.compose.animation.core.Animatable
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import com.ljyh.mei.ui.component.shimmer.skeleton
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

internal const val AlbumMediaArtworkRatio = 1.20f
private val PackageEasing = CubicBezierEasing(.4f, 0f, .2f, 1f)

@Composable
internal fun albumMediaBackground(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < .5f) Color.Black else Color.White

/** Detail-only model stage. Tracks and playback remain owned by the album page. */
@Composable
internal fun AlbumMediaArtwork(
    coverUrl: String?,
    publishTime: Long?,
    title: String,
    artist: String = "",
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    isLoading: Boolean = false,
    tracks: List<AlbumPackageTrack> = emptyList(),
    style: AlbumMediaStyle? = null,
    fillAvailableSpace: Boolean = false,
) {
    val format = style ?: albumMediaStyle(publishTime)
    val pages = remember(tracks, format) { albumLinerPages(tracks, format) }
    var trackPage by rememberSaveable(coverUrl, format) { mutableIntStateOf(0) }
    val page = trackPage.coerceIn(0, pages.lastIndex)
    val label = remember(title, artist, publishTime, pages, page, format) {
        AlbumPackageLabel(title, artist, albumReleaseYear(publishTime), pages[page], tracks.size, page, pages.size, albumLinerCapacity(format))
    }
    val resolver = rememberConstraintsSizeResolver()
    val context = LocalContext.current
    val request = remember(coverUrl, context, resolver) {
        ImageRequest.Builder(context).data(coverUrl).size(resolver).build()
    }
    val placeholder = rememberVectorPainter(Icons.Rounded.Album)
    val cover = rememberAsyncImagePainter(request, placeholder = placeholder, error = placeholder,
        contentScale = ContentScale.Crop)
    val imageState by cover.state.collectAsState()
    val loading = isLoading || imageState is AsyncImagePainter.State.Loading
    val shape = RoundedCornerShape(4.dp)
    var yaw by rememberSaveable(coverUrl, format) { mutableFloatStateOf(-16f) }
    var detail by rememberSaveable(coverUrl, format) { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val renderedYaw by animateFloatAsState(yaw, tween(if (dragging) 0 else 380, easing = PackageEasing), label = "album rotation")
    var pausedDetail by rememberSaveable(coverUrl, format) { mutableStateOf<Float?>(null) }
    val detailMotion = remember(coverUrl, format) { Animatable((pausedDetail ?: detail).coerceIn(0f, 1f)) }
    LaunchedEffect(detail, pausedDetail, detailMotion) {
        val paused = pausedDetail
        if (paused != null) {
            detailMotion.snapTo(paused.coerceIn(0f, 1f))
        } else {
            val target = detail.coerceIn(0f, 1f)
            val duration = (VinylPresentationDurationMillis * abs(target - detailMotion.value)).roundToInt().coerceAtLeast(1)
            detailMotion.animateTo(target,
                if (format == AlbumMediaStyle.Vinyl) tween(duration, easing = LinearEasing)
                else spring(dampingRatio = 1f, stiffness = if (format == AlbumMediaStyle.Cassette) 70f else 100f))
        }
    }
    val renderedDetail = detailMotion.value
    val pose = albumPresentation(format, renderedDetail)
    val cd = remember(label, format) { if (format == AlbumMediaStyle.CD) AlbumCdRenderer(label) else null }
    val tape = remember(label, format) { if (format == AlbumMediaStyle.Cassette) AlbumCassetteRenderer(label) else null }
    val vinyl = remember(label, format) { if (format == AlbumMediaStyle.Vinyl) AlbumVinylRenderer(label) else null }
    val action = when (format) {
        AlbumMediaStyle.CD -> if (detail > 0f) "合上盒盖" else "打开盒盖"
        AlbumMediaStyle.Cassette -> when {
            detail < .22f -> "打开包装"
            detail < .73f -> "取出磁带"
            else -> "装回包装"
        }
        else -> if (detail > 0f) "收回唱片" else "抽出唱片"
    }
    fun activate() {
        detail = albumNextOpeningTarget(format, detail)
        pausedDetail = null
    }
    val canInteract = interactive && format != AlbumMediaStyle.Plain && !loading
    val modelHaze = rememberHazeState()
    BoxWithConstraints(modifier.fillMaxWidth()
        .then(if (fillAvailableSpace) Modifier else Modifier.aspectRatio(AlbumMediaArtworkRatio)).then(resolver)
        .clip(shape).background(albumMediaBackground()).skeleton(loading, shape)) {
        if (!isLoading) Canvas(Modifier.matchParentSize().hazeSource(modelHaze).background(albumMediaBackground())
            .then(if (canInteract) Modifier.clickable(onClickLabel = action, onClick = ::activate)
                .pointerInput(coverUrl, format) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            // Pause the rendered pose without overwriting the user's open/close intent.
                            pausedDetail = detailMotion.value
                            yaw = renderedYaw
                            dragging = true
                        },
                        onDragEnd = { dragging = false }, onDragCancel = { dragging = false },
                    ) { change, delta -> change.consume(); yaw += delta / size.width.coerceAtLeast(1) * 360f }
                } else Modifier)
            .semantics {
                contentDescription = "专辑《$title》封面"
                if (canInteract) {
                    val mechanismState = when (format) {
                        AlbumMediaStyle.CD -> if (detail > 0f) "盒盖已打开" else "盒盖已合上"
                        AlbumMediaStyle.Cassette -> when {
                            detail < .22f -> "包装已合上"
                            detail < .73f -> "包装已打开，磁带在托盘内"
                            else -> "磁带已取出"
                        }
                        else -> when {
                            detail <= 0f && renderedDetail > .001f -> "唱片正在收回"
                            detail > 0f && renderedDetail < .25f -> "唱片正在抽出"
                            detail > 0f -> "唱片已抽出，封套曲目页"
                            else -> "唱片在封套内"
                        }
                    }
                    stateDescription = mechanismState + if (detail > 0f && label.tracks.isNotEmpty())
                        "；" + label.tracks.joinToString("；") { "${it.number}. ${it.title}" } else ""
                    customActions = listOf(
                        CustomAccessibilityAction("旋转") { yaw += 90f; true },
                        CustomAccessibilityAction("复位") { yaw = -16f; detail = 0f; pausedDetail = null; trackPage = 0; true },
                    )
                }
            }) {
            when (format) {
                AlbumMediaStyle.CD -> cd?.draw(this, cover, renderedYaw + pose.yawOffset, pose.pitch, pose.opening)
                AlbumMediaStyle.Cassette -> tape?.draw(this, cover, renderedYaw + pose.yawOffset, pose.pitch, pose.opening, pose.rotation)
                AlbumMediaStyle.Vinyl -> vinyl?.draw(this, cover, renderedYaw + pose.yawOffset, pose.pitch, if (interactive) pose.opening else .18f, pose.rotation)
                AlbumMediaStyle.Plain -> translate((size.width - size.minDimension) / 2f, (size.height - size.minDimension) / 2f) {
                    with(cover) { draw(androidx.compose.ui.geometry.Size(size.minDimension, size.minDimension)) }
                }
            }
        }
        if (canInteract && detail > 0f && pages.size > 1) {
            MeiGlassSurface(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                shape = RoundedCornerShape(24.dp), hazeState = modelHaze,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { trackPage = page - 1 }, enabled = page > 0) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "上一页曲目", Modifier.size(18.dp))
                    }
                    Text("${page + 1} / ${pages.size}", style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.semantics { contentDescription = "曲目内页，第 ${page + 1} 页，共 ${pages.size} 页" })
                    IconButton(onClick = { trackPage = page + 1 }, enabled = page < pages.lastIndex) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, "下一页曲目", Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
