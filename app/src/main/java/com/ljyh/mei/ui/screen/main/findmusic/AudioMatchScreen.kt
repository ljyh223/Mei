package com.ljyh.mei.ui.screen.main.findmusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.ljyh.mei.audio.match.MatchedSong
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.backToMain

@Composable
fun AudioMatchScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AudioMatchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) viewModel.start() else viewModel.onPermissionDenied()
    }
    val requestRecognition = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED) {
            viewModel.start()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.cancelIfRunning()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.cancelIfRunning()
        }
    }

    val busy = state == AudioMatchUiState.Recording ||
        state == AudioMatchUiState.Fingerprinting || state == AudioMatchUiState.Matching
    val status = when (state) {
        AudioMatchUiState.Ready -> "听听周围的音乐"
        AudioMatchUiState.Recording -> "正在聆听…"
        AudioMatchUiState.Fingerprinting -> "正在生成指纹…"
        AudioMatchUiState.Matching -> "正在查找歌曲…"
        AudioMatchUiState.NoMatch -> "暂时没找到这首歌"
        is AudioMatchUiState.Results -> "找到了这些歌曲"
        is AudioMatchUiState.Failure -> "识别失败"
    }
    val detail = when (val current = state) {
        AudioMatchUiState.Ready -> "录制约 3 秒，只发送音频指纹"
        AudioMatchUiState.Recording -> "请让手机靠近正在播放的音乐"
        AudioMatchUiState.Fingerprinting, AudioMatchUiState.Matching -> "这通常只需要几秒钟"
        AudioMatchUiState.NoMatch -> "换一段更清晰的音乐再试试"
        is AudioMatchUiState.Failure -> current.message
        is AudioMatchUiState.Results -> "点选歌曲即可播放"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("听歌识曲") },
                navigationIcon = {
                    com.ljyh.mei.ui.component.IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(144.dp), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(112.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(52.dp),
                        )
                    }
                }
                if (busy) CircularProgressIndicator(Modifier.size(140.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(status, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            if (busy) {
                TextButton(onClick = viewModel::cancel) { Text("取消识别") }
            } else {
                Button(onClick = requestRecognition) {
                    Text(if (state == AudioMatchUiState.Ready) "开始识别" else "再次识别")
                }
            }
            if (state is AudioMatchUiState.Failure &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${context.packageName}".toUri()),
                    )
                }) { Text("打开权限设置") }
            }

            val results = (state as? AudioMatchUiState.Results)?.songs.orEmpty()
            if (results.isNotEmpty()) {
                Spacer(Modifier.height(36.dp))
                Text(
                    "识别结果",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    results.forEachIndexed { index, song ->
                        MatchedSongCard(song) {
                            playerConnection?.playQueue(
                                ListQueue(
                                    id = "AudioMatch-${System.currentTimeMillis()}",
                                    title = "听歌识曲",
                                    items = results.map { it.id.toString() to it.mediaItem },
                                    startIndex = index,
                                    position = song.startTimeMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                                ),
                                startInShuffleMode = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchedSongCard(song: MatchedSong, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (song.coverUrl != null) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Box(
                    modifier = Modifier.size(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                val subtitle = listOf(song.artist, song.album).filter(String::isNotBlank).joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                if (song.startTimeMs > 0) {
                    val seconds = song.startTimeMs / 1_000
                    Text(
                        "匹配到 ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')} 处",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
        }
    }
}
