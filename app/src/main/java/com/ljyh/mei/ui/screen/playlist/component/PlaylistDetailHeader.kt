package com.ljyh.mei.ui.screen.playlist.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.ljyh.mei.ui.component.dialog.FullScreenImageViewer
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.model.PlaylistHeaderStyle
import com.ljyh.mei.ui.model.UiPlaylist
import kotlinx.coroutines.delay

/** Shared dispatch for both the phone list header and the tablet detail column. */
@Composable
fun PlaylistDetailHeader(
    playlist: UiPlaylist,
    isLoading: Boolean,
    onPlayAll: () -> Unit,
    onHeaderAction: () -> Unit,
    onDownload: () -> Unit,
    actionIcon: ImageVector,
    actionLabel: String,
    compact: Boolean = false,
) {
    if (playlist.headerStyle == PlaylistHeaderStyle.Standard ||
        playlist.headerStyle == PlaylistHeaderStyle.Radar || isLoading
    ) {
        PlaylistHeader(
            title = playlist.title,
            cover = playlist.cover,
            coverList = playlist.coverList,
            creator = playlist.creatorName,
            count = playlist.count,
            playCount = playlist.playCount ?: 0L,
            subscribeCount = playlist.subscriberCount,
            isSubscribed = playlist.isSubscribed,
            isLoading = isLoading,
            headerStyle = playlist.headerStyle,
            description = playlist.description,
            updateTime = playlist.updateTime,
            onPlayAll = onPlayAll,
            onSubscribed = { onHeaderAction() },
            onDownload = onDownload,
            actionIcon = actionIcon,
            actionLabel = actionLabel,
        )
        return
    }

    val liked = playlist.headerStyle == PlaylistHeaderStyle.Liked
    var preview by remember(playlist.id) { mutableStateOf<String?>(null) }
    val covers = playlist.coverList.filter(String::isNotBlank).take(3)
        .ifEmpty { listOfNotNull(playlist.cover.takeIf(String::isNotBlank)) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = meiPageGutter()),
            contentAlignment = Alignment.Center,
        ) {
            key(playlist.id) {
                PlaylistCoverStage(
                    covers = covers,
                    liked = liked,
                    onPreview = { preview = it },
                    modifier = Modifier.widthIn(max = if (compact) 280.dp else 420.dp).fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.padding(horizontal = meiPageGutter()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (liked) {
                Icon(Icons.Rounded.Favorite, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = if (liked && playlist.isCreator) "我喜欢的音乐" else playlist.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = listOfNotNull(
                "${playlist.count} 首",
                playlist.creatorName.takeIf(String::isNotBlank),
                playlist.playCount?.let { "播放 ${compactPlaylistCount(it)} 次" },
            ).joinToString(" · "),
            modifier = Modifier.padding(horizontal = meiPageGutter()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!liked && playlist.updateTime > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                playlistUpdateLabel(playlist.updateTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = meiPageGutter())) {
            PlaylistHeaderActions(
                isSubscribed = playlist.isSubscribed,
                onPlayAll = onPlayAll,
                onSubscribed = onHeaderAction,
                onDownload = onDownload,
                actionIcon = actionIcon,
                actionLabel = actionLabel,
            )
        }
    }
    preview?.let { FullScreenImageViewer(it) { preview = null } }
}

@Composable
private fun PlaylistCoverStage(
    covers: List<String>,
    liked: Boolean,
    onPreview: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.clipToBounds()) {
        val width = maxWidth
        val height = width * if (liked) 1.02f else 0.65f
        Box(Modifier.fillMaxWidth().height(height)) {
            if (covers.isEmpty()) {
                Box(Modifier.size(width * 0.54f).align(Alignment.Center)
                    .clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MusicNote, null, Modifier.size(48.dp))
                }
            } else if (liked && covers.size == 1) {
                LikedCoverEntrance(
                    url = covers[0], onPreview = onPreview,
                    rotation = -10f, dropDistance = width,
                    modifier = Modifier.size(width * 0.65f).align(Alignment.Center),
                )
            } else if (liked) {
                covers.getOrNull(2)?.let {
                    LikedCoverEntrance(
                        url = it, onPreview = onPreview,
                        rotation = 9f, opacity = 0.72f, dropDistance = width,
                        spreadDistance = width * -0.08f,
                        modifier = Modifier.offset(x = width * 0.45f, y = width * 0.04f)
                            .size(width * 0.48f),
                    )
                }
                covers.getOrNull(1)?.let {
                    LikedCoverEntrance(
                        url = it, onPreview = onPreview,
                        rotation = -15f, dropDistance = width, delayMillis = 80,
                        spreadDistance = width * 0.12f,
                        modifier = Modifier.offset(x = width * 0.07f, y = width * 0.16f)
                            .size(width * 0.55f),
                    )
                }
                LikedCoverEntrance(
                    url = covers[0], onPreview = onPreview,
                    rotation = 17f, dropDistance = width * 1.15f, delayMillis = 150,
                    spreadDistance = width * -0.10f,
                    modifier = Modifier.offset(x = width * 0.40f, y = width * 0.43f)
                        .size(width * 0.48f),
                )
            } else {
                covers.getOrNull(1)?.let {
                    HeroCover(it, "第 2 名封面", onPreview,
                        Modifier.offset(x = width * 0.04f, y = width * 0.16f)
                            .size(width * 0.40f).graphicsLayer { rotationZ = -7f; alpha = 0.85f })
                }
                covers.getOrNull(2)?.let {
                    HeroCover(it, "第 3 名封面", onPreview,
                        Modifier.offset(x = width * 0.56f, y = width * 0.16f)
                            .size(width * 0.40f).graphicsLayer { rotationZ = 7f; alpha = 0.85f })
                }
                Box(Modifier.offset(x = width * 0.23f, y = width * 0.02f).size(width * 0.54f)) {
                    HeroCover(covers[0], "第 1 名封面", onPreview, Modifier.fillMaxSize())
                }
            }
        }
    }
}

/** Play once for each cover, after its own Coil request completes; reserve layout throughout. */
@Composable
private fun LikedCoverEntrance(
    url: String,
    onPreview: (String) -> Unit,
    rotation: Float,
    dropDistance: Dp,
    modifier: Modifier,
    spreadDistance: Dp = 0.dp,
    opacity: Float = 1f,
    delayMillis: Long = 0,
) {
    var entered by rememberSaveable(url) { mutableStateOf(false) }
    var imageReady by remember(url) { mutableStateOf(false) }
    val progress = remember(url) { Animatable(if (entered) 1f else 0f) }
    val density = LocalDensity.current
    val dropPx = with(density) { dropDistance.toPx() }
    val spreadPx = with(density) { spreadDistance.toPx() }
    LaunchedEffect(url, imageReady) {
        if (!imageReady || entered) return@LaunchedEffect
        entered = true
        val durationScale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        if (durationScale == 0f) {
            progress.snapTo(1f)
        } else {
            delay((delayMillis * durationScale).toLong())
            progress.animateTo(
                1f,
                spring(dampingRatio = 0.68f, stiffness = 65f),
            )
        }
    }
    HeroCover(
        url = url,
        label = "歌曲封面",
        onPreview = { if (progress.value > 0.5f) onPreview(it) },
        onReady = { imageReady = true },
        modifier = modifier.graphicsLayer {
            val fraction = progress.value
            val remaining = 1f - fraction
            translationY = -dropPx * remaining
            translationX = spreadPx * remaining
            rotationZ = rotation - 12f * remaining
            scaleX = 0.92f + 0.08f * fraction
            scaleY = scaleX
            alpha = (fraction * 3f).coerceIn(0f, 1f) * opacity
        },
    )
}

@Composable
private fun HeroCover(
    url: String,
    label: String,
    onPreview: (String) -> Unit,
    modifier: Modifier,
    onReady: () -> Unit = {},
) {
    AsyncImage(
        model = url,
        contentDescription = label,
        contentScale = ContentScale.Crop,
        onSuccess = { onReady() },
        onError = { onReady() },
        modifier = modifier.clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClickLabel = "预览封面") { onPreview(url) },
    )
}

internal fun compactPlaylistCount(count: Long): String = when {
    count >= 100_000_000 -> String.format(java.util.Locale.CHINA, "%.1f亿", count / 100_000_000.0)
    count >= 10_000 -> String.format(java.util.Locale.CHINA, "%.1f万", count / 10_000.0)
    else -> count.toString()
}
