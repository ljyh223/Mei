package com.ljyh.mei.ui.screen.setting

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
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ljyh.mei.constants.DevModeKey
import com.ljyh.mei.data.repository.AppleMusicCoverDiagnostic
import com.ljyh.mei.data.repository.CoverPalette
import com.ljyh.mei.data.repository.DynamicCover
import com.ljyh.mei.data.repository.MotionArtworkClip
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.SwitchPreference
import com.ljyh.mei.ui.component.player.component.DynamicCoverView
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.utils.rememberPreference

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
    val (devMode, onDevModeChange) = rememberPreference(DevModeKey, defaultValue = false)
    var album by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    var seededSongId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(currentSong?.id) {
        if (album.isBlank() && artist.isBlank() && currentSong != null && seededSongId == null) {
            album = currentSong.album.title
            artist = currentSong.artists.firstOrNull()?.name.orEmpty()
            seededSongId = currentSong.id
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("开发者选项") },
            navigationIcon = {
                IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
            },
            scrollBehavior = scrollBehavior
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SwitchPreference(
                title = { Text("开发者模式") },
                description = "关闭后隐藏设置入口",
                icon = { Icon(Icons.Rounded.BugReport, contentDescription = null) },
                checked = devMode,
                onCheckedChange = onDevModeChange
            )
            Text("Apple Music 动态封面诊断", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp))
            Text("使用与播放时相同的专辑搜索和严格匹配规则。仅在点击查询时请求网络。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp))
            OutlinedTextField(
                value = album, onValueChange = { album = it }, label = { Text("专辑名") },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            OutlinedTextField(
                value = artist, onValueChange = { artist = it }, label = { Text("歌手名") },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
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
                DiagnosticState.Loading -> Text("正在查询…", modifier = Modifier.padding(horizontal = 16.dp))
                is DiagnosticState.Failed -> Text(
                    "查询失败：${current.message}", color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                is DiagnosticState.Success -> DiagnosticResult(current.result)
            }
        }
    }
}

@Composable
private fun DiagnosticResult(result: AppleMusicCoverDiagnostic) {
    var activePreview by remember(result.match?.id, result.square?.url, result.portrait?.url) {
        mutableStateOf<PreviewVariant?>(when {
            result.square != null -> PreviewVariant.SQUARE
            result.portrait != null -> PreviewVariant.PORTRAIT
            else -> null
        })
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
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
                        Text("${index + 1}. ${candidate.name}", style = MaterialTheme.typography.titleSmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(candidate.artist, style = MaterialTheme.typography.bodySmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("ID ${candidate.id}", style = MaterialTheme.typography.labelSmall)
                        if (selected) {
                            Text("✓ 已匹配", color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        if (result.match == null) {
            Text("没有符合专辑名完全一致、歌手名包含要求的结果。")
        } else {
            ClipResult("方形动态封面", result.square, result.match.artworkUrl,
                result.match.id, PreviewVariant.SQUARE, activePreview == PreviewVariant.SQUARE) {
                activePreview = if (activePreview == PreviewVariant.SQUARE) null else PreviewVariant.SQUARE
            }
            ClipResult("竖屏动态封面", result.portrait, result.match.artworkUrl,
                result.match.id, PreviewVariant.PORTRAIT, activePreview == PreviewVariant.PORTRAIT) {
                activePreview = if (activePreview == PreviewVariant.PORTRAIT) null else PreviewVariant.PORTRAIT
            }
        }
    }
}

private enum class PreviewVariant { SQUARE, PORTRAIT }

@Composable
private fun ClipResult(
    title: String,
    clip: MotionArtworkClip?,
    albumArtworkUrl: String?,
    albumId: String,
    variant: PreviewVariant,
    active: Boolean,
    onToggle: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (clip == null) {
                Text("无")
            } else {
                val previewImage = clip.previewUrl ?: albumArtworkUrl.orEmpty()
                var playbackFailed by remember(clip.url) { mutableStateOf(false) }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val previewModifier = Modifier.width(if (variant == PreviewVariant.PORTRAIT) 220.dp else 240.dp)
                        .aspectRatio(if (variant == PreviewVariant.PORTRAIT) 3f / 4f else 1f)
                        .clip(RoundedCornerShape(12.dp))
                    if (active) {
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
                            onPlaybackError = { playbackFailed = true },
                            modifier = previewModifier
                        )
                    } else {
                        AsyncImage(
                            model = previewImage,
                            contentDescription = "$title 预览帧",
                            contentScale = ContentScale.Crop,
                            modifier = previewModifier
                        )
                    }
                }
                TextButton(onClick = onToggle) {
                    Text(if (active) "暂停视频预览" else "播放视频预览")
                }
                if (playbackFailed && active) {
                    Text("视频加载失败，已显示预览帧", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("视频：${clip.url}", style = MaterialTheme.typography.bodySmall)
                Text("预览帧：${clip.previewUrl ?: "无"}", style = MaterialTheme.typography.bodySmall)
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
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(background).padding(12.dp)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
                    .background(hex.asAppleColor() ?: Color.Transparent)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)))
                Text("$label  ${hex?.let { "#$it" } ?: "无"}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun String?.asAppleColor(): Color? = this?.takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) }
    ?.let { Color(android.graphics.Color.parseColor("#$it")) }
