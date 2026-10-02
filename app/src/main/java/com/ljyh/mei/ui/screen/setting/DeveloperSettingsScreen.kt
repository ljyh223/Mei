package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.constants.DevModeKey
import com.ljyh.mei.data.repository.AppleMusicCoverDiagnostic
import com.ljyh.mei.data.repository.MotionArtworkClip
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.SwitchPreference
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
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("搜索词：${result.query}", style = MaterialTheme.typography.titleSmall)
        Text("候选专辑：${result.candidates.size} 项", style = MaterialTheme.typography.titleSmall)
        result.candidates.forEachIndexed { index, candidate ->
            val selected = candidate == result.match
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${index + 1}. ${candidate.name}${if (selected) " · 已匹配" else ""}",
                        style = MaterialTheme.typography.titleSmall)
                    Text("歌手：${candidate.artist}", style = MaterialTheme.typography.bodySmall)
                    Text("Apple 专辑 ID：${candidate.id}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (result.match == null) {
            Text("没有符合专辑名完全一致、歌手名包含要求的结果。")
        } else {
            ClipResult("方形动态封面", result.square)
            ClipResult("竖屏动态封面", result.portrait)
        }
    }
}

@Composable
private fun ClipResult(title: String, clip: MotionArtworkClip?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (clip == null) {
                Text("无")
            } else {
                Text("视频：${clip.url}", style = MaterialTheme.typography.bodySmall)
                Text("预览帧：${clip.previewUrl ?: "无"}", style = MaterialTheme.typography.bodySmall)
                val palette = clip.palette
                if (palette == null) {
                    Text("颜色：无")
                } else {
                    Text("背景 #${palette.bgColor.orEmpty()}")
                    Text("文字 1 #${palette.textColor1.orEmpty()}")
                    Text("文字 2 #${palette.textColor2.orEmpty()}")
                    Text("文字 3 #${palette.textColor3.orEmpty()}")
                    Text("文字 4 #${palette.textColor4.orEmpty()}")
                }
            }
        }
    }
}
