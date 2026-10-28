package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.theme.MeiCategory
import com.ljyh.mei.ui.theme.MeiCategoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(scrollBehavior: TopAppBarScrollBehavior) {
    val navController = LocalNavController.current
    MeiPinnedPage(title = "设置", contentTopPadding = 12.dp) { paddingValues ->
        Column(
            Modifier.fillMaxSize().verticalScroll(LocalMeiPageScroll.current!!)
                .padding(paddingValues).padding(horizontal = meiPageGutter()),
        ) {
            MeiLargeTitle("设置", modifier = Modifier.padding(bottom = 16.dp), startPadding = 0.dp)
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                SettingsGroup("播放与界面") {
                    SettingRow("播放体验", "音质、播放行为与歌曲过渡", Icons.Rounded.LibraryMusic, MeiCategory.Playback) {
                        Screen.PlaySettings.navigate(navController)
                    }
                    SettingsDivider()
                    SettingRow("音频输出与音效", "USB 独占、均衡器与混响", Icons.Rounded.GraphicEq, MeiCategory.Playback) {
                        Screen.AudioOutputSettings.navigate(navController)
                    }
                    SettingsDivider()
                    SettingRow("歌词", "显示、翻译与桌面歌词", Icons.Rounded.Lyrics, MeiCategory.Lyrics) {
                        Screen.LyricsSettings.navigate(navController)
                    }
                    SettingsDivider()
                    SettingRow("外观", "主题、封面与界面", Icons.Rounded.Palette, MeiCategory.Appearance) {
                        Screen.AppearanceSettings.navigate(navController)
                    }
                }
                SettingsGroup("账号与内容来源") {
                    SettingRow("账号", "网易云与 QQ 音乐账号", Icons.Rounded.AccountCircle, MeiCategory.Content) {
                        Screen.ContentSettings.navigate(navController)
                    }
                    SettingsDivider()
                    SettingRow("音源与平台歌词", "音源脚本、接口与歌词服务", Icons.Rounded.Language, MeiCategory.Content) {
                        Screen.SourceAndLyricsSettings.navigate(navController)
                    }
                }
                SettingsGroup("存储与其他") {
                    SettingRow("下载", "音质与保存位置", Icons.Rounded.Download, MeiCategory.Offline) {
                        Screen.DownloadSettings.navigate(navController)
                    }
                    SettingsDivider()
                    SettingRow("缓存", "占用空间与清理", Icons.Rounded.Storage, MeiCategory.Storage) {
                        Screen.CacheSettings.navigate(navController)
                    }
                    SettingsDivider()
                    SettingRow("Beta 功能", "试用中的效果与参数", Icons.Rounded.Science, MeiCategory.Experimental) {
                        Screen.BetaSettings.navigate(navController)
                    }
                    SettingsDivider()
                    SettingRow("关于 Mei", null, Icons.Rounded.Info, MeiCategory.About) {
                        Screen.About.navigate(navController)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        thickness = 0.5.dp,
    )
}

@Composable
private fun SettingRow(
    title: String,
    description: String?,
    icon: ImageVector,
    category: MeiCategory,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .heightIn(min = 76.dp).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MeiCategoryIcon(icon, category)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (!description.isNullOrBlank()) Text(
                description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp),
        )
    }
}
