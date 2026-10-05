package com.ljyh.mei.ui.screen.setting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.ljyh.mei.data.repository.AppleMusicCoverDiagnostic
import com.ljyh.mei.data.repository.CoverPalette
import com.ljyh.mei.data.repository.DynamicCover
import com.ljyh.mei.data.repository.MotionArtworkClip
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.player.component.DynamicCoverView
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.backToMain
import kotlinx.coroutines.delay
import androidx.core.graphics.toColorInt
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: DeveloperSettingsViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current
    val currentSong = playerConnection?.mediaMetadata?.collectAsState()?.value
    val state by viewModel.state.collectAsState()
    var album by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    var seededSongId by rememberSaveable { mutableStateOf<Long?>(null) }
    var viewportBounds by remember { mutableStateOf<Rect?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var screenResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            screenResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentSong?.id) {
        if (album.isBlank() && artist.isBlank() && currentSong != null && seededSongId == null) {
            album = currentSong.album.title
            artist = currentSong.artists.firstOrNull()?.name.orEmpty()
            seededSongId = currentSong.id
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("动态封面调试") },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = null
                    )
                }
            },
            scrollBehavior = scrollBehavior
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .onGloballyPositioned { viewportBounds = it.boundsInWindow() }
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Apple Music 搜索匹配", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                "手动查询按专辑名和歌手名匹配；播放时未命中会再比较封面。仅在点击查询时请求网络。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            OutlinedTextField(
                value = album, onValueChange = { album = it }, label = { Text("专辑名") },
                singleLine = true, modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            OutlinedTextField(
                value = artist, onValueChange = { artist = it }, label = { Text("歌手名") },
                singleLine = true, modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            if (currentSong != null) {
                Button(
                    onClick = {
                        album = currentSong.album.title
                        artist = currentSong.artists.firstOrNull()?.name.orEmpty()
                        seededSongId = currentSong.id
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) { Text("填入当前歌曲") }
            }
            Button(
                onClick = { viewModel.inspect(album, artist) },
                enabled = album.isNotBlank() && artist.isNotBlank() && state !is DiagnosticState.Loading,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) { Text("查询匹配与封面") }

            when (val current = state) {
                DiagnosticState.Idle -> Unit
                DiagnosticState.Loading -> Text(
                    "正在查询…",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                is DiagnosticState.Failed -> Text(
                    "查询失败：${current.message}", color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                is DiagnosticState.Success -> DiagnosticResult(
                    current.result,
                    viewportBounds,
                    screenResumed
                )
            }
        }
    }
}

@Composable
private fun DiagnosticResult(
    result: AppleMusicCoverDiagnostic,
    viewportBounds: Rect?,
    screenResumed: Boolean
) {
    val previewIdentity = listOf(result.match?.id, result.square?.url, result.portrait?.url)
    var squareBounds by remember(previewIdentity) { mutableStateOf<Rect?>(null) }
    var portraitBounds by remember(previewIdentity) { mutableStateOf<Rect?>(null) }
    var activePreview by remember(previewIdentity) { mutableStateOf<PreviewVariant?>(null) }
    val squareFraction = visibleHeightFraction(squareBounds, viewportBounds)
    val portraitFraction = visibleHeightFraction(portraitBounds, viewportBounds)
    LaunchedEffect(previewIdentity, squareFraction, portraitFraction, screenResumed) {
        val target = if (screenResumed) {
            chooseVisiblePreview(squareFraction, portraitFraction, activePreview)
        } else null
        if (target != activePreview) {
            // Release the previous decoder before preparing the next preview.
            activePreview = null
            if (target != null) {
                delay(150.milliseconds)
                activePreview = target
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("搜索词：${result.query}", style = MaterialTheme.typography.titleSmall)
        Text("候选专辑：${result.candidates.size} 项", style = MaterialTheme.typography.titleSmall)
        result.candidates.forEachIndexed { index, candidate ->
            val selected = candidate == result.match
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (selected) BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.primary
                ) else null
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (candidate.artworkUrl != null) {
                            AsyncImage(
                                model = candidate.artworkUrl,
                                contentDescription = "${candidate.name} 封面",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("无封面", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${index + 1}. ${candidate.name}",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            candidate.artist, style = MaterialTheme.typography.bodySmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Text("ID ${candidate.id}", style = MaterialTheme.typography.labelSmall)
                        if (selected) {
                            Text(
                                "✓ 已匹配", color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
        if (result.match == null) {
            Text("没有符合专辑名完全一致、歌手名包含要求的结果。")
        } else {
            ClipResult(
                "方形动态封面", result.square, result.match.artworkUrl,
                result.match.id, PreviewVariant.SQUARE, activePreview == PreviewVariant.SQUARE,
                onBoundsChanged = { squareBounds = it })
            ClipResult(
                "竖屏动态封面", result.portrait, result.match.artworkUrl,
                result.match.id, PreviewVariant.PORTRAIT, activePreview == PreviewVariant.PORTRAIT,
                onBoundsChanged = { portraitBounds = it })
        }
    }
}

internal enum class PreviewVariant { SQUARE, PORTRAIT }

internal fun visibleHeightFraction(item: Rect?, viewport: Rect?): Float {
    if (item == null || viewport == null || item.height <= 0f) return 0f
    return ((minOf(item.bottom, viewport.bottom) - maxOf(item.top, viewport.top))
        .coerceAtLeast(0f) / item.height).coerceIn(0f, 1f)
}

internal fun chooseVisiblePreview(
    squareFraction: Float,
    portraitFraction: Float,
    current: PreviewVariant?
): PreviewVariant? {
    val currentFraction = when (current) {
        PreviewVariant.SQUARE -> squareFraction
        PreviewVariant.PORTRAIT -> portraitFraction
        null -> 0f
    }
    val otherFraction = when (current) {
        PreviewVariant.SQUARE -> portraitFraction
        PreviewVariant.PORTRAIT -> squareFraction
        null -> 0f
    }
    if (current != null && currentFraction >= 0.4f &&
        (otherFraction < 0.6f || otherFraction < currentFraction + 0.15f)
    ) return current
    return when {
        squareFraction >= 0.55f && squareFraction >= portraitFraction -> PreviewVariant.SQUARE
        portraitFraction >= 0.55f -> PreviewVariant.PORTRAIT
        else -> null
    }
}

@Composable
private fun ClipResult(
    title: String,
    clip: MotionArtworkClip?,
    albumArtworkUrl: String?,
    albumId: String,
    variant: PreviewVariant,
    active: Boolean,
    onBoundsChanged: (Rect) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (clip == null) {
                Text("无")
            } else {
                val previewImage = clip.previewUrl ?: albumArtworkUrl.orEmpty()
                var playbackError by remember(clip.url, active) { mutableStateOf<Int?>(null) }
                var retryAttempt by remember(clip.url, active) { mutableIntStateOf(0) }
                LaunchedEffect(playbackError, retryAttempt, active) {
                    if (active && playbackError != null && retryAttempt == 0) {
                        delay(700.milliseconds)
                        retryAttempt = 1
                        playbackError = null
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            onBoundsChanged(it.boundsInWindow())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val previewModifier =
                        Modifier
                            .width(if (variant == PreviewVariant.PORTRAIT) 220.dp else 240.dp)
                            .aspectRatio(if (variant == PreviewVariant.PORTRAIT) 3f / 4f else 1f)
                            .clip(RoundedCornerShape(12.dp))
                    if (active) {
                        key(clip.url, retryAttempt) {
                            DynamicCoverView(
                                imageUrl = previewImage,
                                cover = DynamicCover(
                                    songId = albumId.toLongOrNull() ?: 0L,
                                    url = clip.url,
                                    source = DynamicCover.Source.APPLE_MUSIC,
                                    cacheKey = "dynamic:apple:$albumId${if (variant == PreviewVariant.PORTRAIT) ":portrait" else ""}",
                                    palette = clip.palette,
                                    previewUrl = clip.previewUrl
                                ),
                                playing = true,
                                onPlaybackError = { playbackError = it.errorCode },
                                modifier = previewModifier
                            )
                        }
                    } else {
                        AsyncImage(
                            model = previewImage,
                            contentDescription = "$title 预览帧",
                            contentScale = ContentScale.Crop,
                            modifier = previewModifier
                        )
                    }
                }
                if (playbackError != null && retryAttempt > 0 && active) {
                    Text(
                        "视频加载失败（错误码 $playbackError），滚出后再滚回可重试",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                CopyableUrl("视频", clip.url)
                clip.previewUrl?.let { CopyableUrl("预览帧", it) }
                val palette = clip.palette
                if (palette == null) {
                    Text("颜色：无")
                } else {
                    PalettePreview(palette)
                }
            }
        }
    }
}

@Composable
private fun CopyableUrl(label: String, url: String) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label：$url", style = MaterialTheme.typography.bodySmall,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        MaterialIconButton(onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(label, url))
            Toast.makeText(context, "已复制${label}链接", Toast.LENGTH_SHORT).show()
        }) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = "复制${label}链接")
        }
    }
}

@Composable
private fun PalettePreview(palette: CoverPalette) {
    val background = palette.bgColor.asAppleColor() ?: MaterialTheme.colorScheme.surfaceVariant
    val colors = listOf(
        "背景" to palette.bgColor,
        "文字 1" to palette.textColor1,
        "文字 2" to palette.textColor2,
        "文字 3" to palette.textColor3,
        "文字 4" to palette.textColor4
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(background)
                .padding(12.dp)
        ) {
            colors.drop(1).forEachIndexed { index, (_, hex) ->
                Text(
                    text = if (index == 0) "标题文字示例" else "文字 ${index + 1} 示例",
                    color = hex.asAppleColor() ?: MaterialTheme.colorScheme.onSurface,
                    style = if (index == 0) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.bodySmall
                )
            }
        }
        colors.forEach { (label, hex) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(hex.asAppleColor() ?: Color.Transparent)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                )
                Text("$label  ${hex?.let { "#$it" } ?: "无"}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun String?.asAppleColor(): Color? = this?.takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) }
    ?.let { Color("#$it".toColorInt()) }
