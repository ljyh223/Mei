package com.ljyh.mei.ui.screen.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ljyh.mei.BuildConfig
import com.ljyh.mei.R
import com.ljyh.mei.constants.DevModeKey
import com.ljyh.mei.constants.Github
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.preferences.rememberPreference
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val viewModel: AboutViewModel = hiltViewModel()

    val (devMode, onDevModeChange) = rememberPreference(DevModeKey, defaultValue = false)
    var clickCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(40.dp))
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Mei Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            clickCount++
                            if (clickCount >= 7 && !devMode) {
                                onDevModeChange(true)
                                Toast.makeText(context, "开发者模式已开启", Toast.LENGTH_SHORT).show()
                            }
                        }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Mei",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))
            }

            item {
                AboutSectionTitle("开发")
                AboutSegmentedGroup {
                    AboutActionItem(
                        icon = Icons.Rounded.Source,
                        title = "GitHub 仓库",
                        onClick = { openUrl(context, Github) }
                    )
                    AboutActionItem(
                        icon = Icons.Rounded.BugReport,
                        title = "问题反馈",
                        onClick = { openUrl(context, "$Github/issues") }
                    )
                    AboutActionItem(
                        icon = Icons.Default.Terminal,
                        title = "日志收集"
                    ) {
                        Screen.Log.navigate(navController)
                    }
                }
            }

            item {
                AboutSectionTitle("开源致谢")
                AboutSegmentedGroup {
                    AboutActionItem(
                        icon = Icons.Rounded.LibraryMusic,
                        title = "amll-ttml-db",
                        subtitle = "高质量逐字歌词库",
                        onClick = { openUrl(context, "https://github.com/Steve-xmh/amll-ttml-db") }
                    )
                    AboutActionItem(
                        icon = Icons.Rounded.Lyrics,
                        title = "accompanist-lyrics-ui",
                        subtitle = "精美歌词组件渲染",
                        onClick = { openUrl(context, "https://github.com/6xingyv/accompanist-lyrics-ui") }
                    )
                    AboutActionItem(
                        icon = Icons.Rounded.LibraryMusic,
                        title = "BeatNet",
                        subtitle = "智能过渡的节拍分析模型 · CC BY 4.0",
                        onClick = { openUrl(context, "$Github/blob/main/THIRD_PARTY_NOTICES") }
                    )
                }
            }

            item {
                Spacer(Modifier.height(48.dp))
                Text(
                    text = "Mei · ${LocalDate.now().year}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "基于 Jetpack Compose 构建",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(24.dp))
            }

            if (devMode) {
                item {
                    AboutSectionTitle("开发者调试")
                    AboutSegmentedGroup {
                        ListItem(
                            modifier = Modifier.clickable {
                                viewModel.deleteAllCachedLyrics()
                                Toast.makeText(context, "已清除AI歌词缓存", Toast.LENGTH_SHORT).show()
                            },
                            leadingContent = {
                                Icon(Icons.Rounded.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            headlineContent = { Text("删除AI歌词数据", style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = { Text("清除 cached_lyric 表", style = MaterialTheme.typography.bodySmall) },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                        ListItem(
                            modifier = Modifier.clickable {
                                viewModel.deleteAllQQSongs()
                                Toast.makeText(context, "已清除QQ音乐ID映射", Toast.LENGTH_SHORT).show()
                            },
                            leadingContent = {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            headlineContent = { Text("删除QQ音乐ID数据", style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = { Text("清除 qqSong 表", style = MaterialTheme.typography.bodySmall) },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                        ListItem(
                            modifier = Modifier.clickable {
                                Screen.DeveloperSettings.navigate(navController)
                            },
                            leadingContent = {
                                Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            headlineContent = { Text("动态封面调试", style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = { Text("Apple Music 匹配、视频与色板", style = MaterialTheme.typography.bodySmall) },
                            trailingContent = {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                        ListItem(
                            modifier = Modifier.clickable { onDevModeChange(false) },
                            headlineContent = { Text("开发者模式", style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = { Text("关闭后隐藏开发者调试区域", style = MaterialTheme.typography.bodySmall) },
                            trailingContent = {
                                Switch(checked = true, onCheckedChange = onDevModeChange)
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun AboutSegmentedGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
private fun AboutSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 24.dp, bottom = 10.dp)
    )
}

@Composable
private fun AboutActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle, style = MaterialTheme.typography.bodySmall) }
        } else null,
        trailingContent = {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    )
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
