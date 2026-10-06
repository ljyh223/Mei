package com.ljyh.mei.ui.screen.setting

import android.text.format.Formatter
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.ImageCacheLimitMbKey
import com.ljyh.mei.constants.MusicCacheLimitMbKey
import com.ljyh.mei.playback.source.CacheManager
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.ListPreference
import com.ljyh.mei.ui.component.PreferenceEntry
import com.ljyh.mei.ui.component.PreferenceGroupTitle
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.utils.cache.CacheCategory
import com.ljyh.mei.utils.cache.CacheUsage
import com.ljyh.mei.utils.cache.StorageCacheManager
import com.ljyh.mei.utils.preferences.rememberPreference
import com.ljyh.mei.ui.component.IconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheSettingsScreen(scrollBehavior: TopAppBarScrollBehavior) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var usage by remember { mutableStateOf<CacheUsage?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var pendingClear by remember { mutableStateOf<CacheCategory?>(null) }
    var busy by remember { mutableStateOf(false) }
    val (imageLimit, setImageLimit) = rememberPreference(ImageCacheLimitMbKey, 250)
    val (musicLimit, setMusicLimit) = rememberPreference(
        MusicCacheLimitMbKey, CacheManager.DEFAULT_MUSIC_CACHE_LIMIT_MB
    )

    LaunchedEffect(Unit) {
        runCatching { withContext(Dispatchers.IO) { StorageCacheManager.usage(context) } }
            .onSuccess { usage = it }
            .onFailure {
                loadFailed = true
                snackbar.showSnackbar("无法读取缓存占用，请点击刷新重试")
            }
    }

    pendingClear?.let { category ->
        val label = category.label
        AlertDialog(
            onDismissRequest = { pendingClear = null },
            title = { Text("清理$label？") },
            text = {
                Text(
                    when (category) {
                        CacheCategory.MUSIC -> "已缓存的在线播放音乐会被移除，正在播放的歌曲可能短暂重新缓冲。已下载的歌曲不会删除。"
                        CacheCategory.OTHER -> "动态封面和临时缓存会被移除，封面可能重新加载。已下载的歌曲和正在下载的文件不会删除。"
                        CacheCategory.IMAGE -> "图片会在需要时重新加载。已下载的歌曲和个人数据不会删除。"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingClear = null
                    busy = true
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                StorageCacheManager.clear(context, category)
                                StorageCacheManager.usage(context)
                            }
                        }
                        result.onSuccess {
                            usage = it
                            loadFailed = false
                        }
                        busy = false
                        snackbar.showSnackbar(
                            if (result.isSuccess) "${label}已清理" else "清理失败，请稍后重试"
                        )
                    }
                }) { Text("清理", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingClear = null }) { Text("取消") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("缓存设置") },
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
                actions = {
                    androidx.compose.material3.IconButton(onClick = {
                        scope.launch {
                            loadFailed = false
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    StorageCacheManager.usage(
                                        context
                                    )
                                }
                            }
                                .onSuccess { usage = it }
                                .onFailure {
                                    loadFailed = true
                                    snackbar.showSnackbar("无法读取缓存占用，请稍后重试")
                                }
                        }
                    }, enabled = !busy) {
                        Icon(Icons.Rounded.Cached, contentDescription = "刷新缓存占用")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
        ) {
            PreferenceGroupTitle("占用空间")
            PreferenceEntry(
                title = { Text("缓存总计") },
                description = usage?.let { Formatter.formatShortFileSize(context, it.totalBytes) }
                    ?: if (loadFailed) "读取失败" else "正在计算…",
                icon = { Icon(Icons.Rounded.Storage, contentDescription = null) },
            )
            CacheRow(
                "图片",
                usage?.imageBytes,
                busy,
                loadFailed,
                { pendingClear = CacheCategory.IMAGE }) {
                Icon(Icons.Rounded.Image, contentDescription = null)
            }
            CacheRow(
                "音乐",
                usage?.musicBytes,
                busy,
                loadFailed,
                { pendingClear = CacheCategory.MUSIC }) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = null)
            }
            CacheRow(
                "其他",
                usage?.otherBytes,
                busy,
                loadFailed,
                { pendingClear = CacheCategory.OTHER }) {
                Icon(Icons.Rounded.Cached, contentDescription = null)
            }
            PreferenceGroupTitle("缓存上限")
            ListPreference(
                title = { Text("图片缓存") },
                description = "${imageLimit.toCacheLimitLabel()} · 下次启动生效",
                selectedValue = imageLimit,
                values = listOf(50, 100, 250, 500, 1024, 2048),
                valueText = { it.toCacheLimitLabel() },
                onValueSelected = setImageLimit,
                icon = { Icon(Icons.Rounded.Image, contentDescription = null) },
            )
            ListPreference(
                title = { Text("音乐缓存") },
                description = "${musicLimit.toCacheLimitLabel()} · 下次启动生效",
                selectedValue = musicLimit,
                values = listOf(256, 512, 1024, 2048, 5120, 10240, 20480),
                valueText = { it.toCacheLimitLabel() },
                onValueSelected = setMusicLimit,
                icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null) },
            )
            Text(
                "音乐与动态封面共用缓存上限。其他临时缓存不设单独上限；正在下载的文件不计入，也不会被清理。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CacheRow(
    title: String,
    bytes: Long?,
    busy: Boolean,
    loadFailed: Boolean,
    onClear: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val context = LocalContext.current
    PreferenceEntry(
        title = { Text("${title}缓存") },
        description = bytes?.let { Formatter.formatShortFileSize(context, it) }
            ?: if (loadFailed) "读取失败" else "正在计算…",
        icon = icon,
        trailingContent = {
            TextButton(onClick = onClear, enabled = !busy && bytes != null && bytes > 0L) {
                Text("清理")
            }
        },
    )
}

private val CacheCategory.label: String
    get() = when (this) {
        CacheCategory.IMAGE -> "图片缓存"
        CacheCategory.MUSIC -> "音乐缓存"
        CacheCategory.OTHER -> "其他缓存"
    }

private fun Int.toCacheLimitLabel(): String =
    if (this >= 1024) "${this / 1024} GB" else "$this MB"
