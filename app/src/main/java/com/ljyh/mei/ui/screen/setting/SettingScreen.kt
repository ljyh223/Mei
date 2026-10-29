package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.glass.LocalMeiShowBackButton
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.component.layout.meiPageGutter
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.navigationBuilder
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.tablet.TabletPageContent
import com.ljyh.mei.ui.theme.MeiCategory
import com.ljyh.mei.ui.theme.MeiCategoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(scrollBehavior: TopAppBarScrollBehavior) {
    val navController = LocalNavController.current
    if (rememberDeviceInfo().isTablet) {
        TabletSettingsScreen(scrollBehavior)
        return
    }
    MeiPinnedPage(title = "设置", contentTopPadding = 12.dp) { paddingValues ->
        Column(
            Modifier.fillMaxSize().verticalScroll(LocalMeiPageScroll.current!!)
                .padding(paddingValues).padding(horizontal = meiPageGutter()),
        ) {
            MeiLargeTitle("设置", modifier = Modifier.padding(bottom = 16.dp), startPadding = 0.dp)
            SettingsCategories { it.navigate(navController) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabletSettingsScreen(scrollBehavior: TopAppBarScrollBehavior) {
    val detailNavController = rememberNavController()
    val entry by detailNavController.currentBackStackEntryAsState()
    val selectedRoute = when (entry?.destination?.route) {
        Screen.UsbExclusiveSettings.route, Screen.Equalizer.route, Screen.ReverbSettings.route -> Screen.AudioOutputSettings.route
        Screen.GlassSettings.route -> Screen.AppearanceSettings.route
        Screen.LxSources.route -> Screen.SourceAndLyricsSettings.route
        Screen.DownloadManage.route -> Screen.DownloadSettings.route
        Screen.DeveloperSettings.route -> Screen.About.route
        Screen.Log.route -> Screen.About.route
        null -> Screen.PlaySettings.route
        else -> entry?.destination?.route
    }
    Row(Modifier.fillMaxSize()) {
        MeiPinnedPage(title = "设置", modifier = Modifier.weight(0.42f), contentTopPadding = 12.dp) { paddingValues ->
            Column(
                Modifier.fillMaxSize().verticalScroll(LocalMeiPageScroll.current!!)
                    .padding(paddingValues).padding(horizontal = meiPageGutter()),
            ) {
                MeiLargeTitle("设置", modifier = Modifier.padding(bottom = 16.dp), startPadding = 0.dp)
                SettingsCategories(selectedRoute) { screen ->
                    detailNavController.navigate(screen.route) {
                        popUpTo(detailNavController.graph.id) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        VerticalDivider(Modifier.fillMaxHeight().width(0.5.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        CompositionLocalProvider(
            LocalNavController provides detailNavController,
            LocalMeiShowBackButton provides (entry?.destination?.route != selectedRoute),
        ) {
            NavHost(
                navController = detailNavController,
                startDestination = Screen.PlaySettings.route,
                modifier = Modifier.weight(0.58f).fillMaxSize(),
            ) {
                navigationBuilder(detailNavController, scrollBehavior, TabletPageContent)
            }
        }
    }
}

@Composable
private fun SettingsCategories(selectedRoute: String? = null, onSelect: (Screen) -> Unit) {
    CompositionLocalProvider(LocalSelectedSettingsRoute provides selectedRoute) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SettingsGroup("播放与界面") {
                SettingRow(Screen.PlaySettings.route, "播放体验", "音质、播放行为与歌曲过渡", Icons.Rounded.LibraryMusic, MeiCategory.Playback) {
                    onSelect(Screen.PlaySettings)
                }
                SettingsDivider()
                SettingRow(Screen.AudioOutputSettings.route, "音频输出与音效", "USB 独占、均衡器与混响", Icons.Rounded.GraphicEq, MeiCategory.Playback) {
                    onSelect(Screen.AudioOutputSettings)
                }
                SettingsDivider()
                SettingRow(Screen.LyricsSettings.route, "歌词", "显示、翻译与桌面歌词", Icons.Rounded.Lyrics, MeiCategory.Lyrics) {
                    onSelect(Screen.LyricsSettings)
                }
                SettingsDivider()
                SettingRow(Screen.AppearanceSettings.route, "外观", "主题、封面与界面", Icons.Rounded.Palette, MeiCategory.Appearance) {
                    onSelect(Screen.AppearanceSettings)
                }
            }
            SettingsGroup("账号与内容来源") {
                SettingRow(Screen.ContentSettings.route, "账号", "网易云与 QQ 音乐账号", Icons.Rounded.AccountCircle, MeiCategory.Content) {
                    onSelect(Screen.ContentSettings)
                }
                SettingsDivider()
                SettingRow(Screen.SourceAndLyricsSettings.route, "音源与平台歌词", "音源脚本、接口与歌词服务", Icons.Rounded.Language, MeiCategory.Content) {
                    onSelect(Screen.SourceAndLyricsSettings)
                }
            }
            SettingsGroup("存储与其他") {
                SettingRow(Screen.DownloadSettings.route, "下载", "音质与保存位置", Icons.Rounded.Download, MeiCategory.Offline) {
                    onSelect(Screen.DownloadSettings)
                }
                SettingsDivider()
                SettingRow(Screen.CacheSettings.route, "缓存", "占用空间与清理", Icons.Rounded.Storage, MeiCategory.Storage) {
                    onSelect(Screen.CacheSettings)
                }
                SettingsDivider()
                SettingRow(Screen.BetaSettings.route, "Beta 功能", "试用中的效果与参数", Icons.Rounded.Science, MeiCategory.Experimental) {
                    onSelect(Screen.BetaSettings)
                }
                SettingsDivider()
                SettingRow(Screen.About.route, "关于 Mei", null, Icons.Rounded.Info, MeiCategory.About) {
                    onSelect(Screen.About)
                }
            }
        }
    }
}

private val LocalSelectedSettingsRoute = androidx.compose.runtime.staticCompositionLocalOf<String?> { null }

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
    route: String,
    title: String,
    description: String?,
    icon: ImageVector,
    category: MeiCategory,
    onClick: () -> Unit,
) {
    val selected = LocalSelectedSettingsRoute.current == route
    Row(
        modifier = Modifier.fillMaxWidth().background(if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent).selectable(selected = selected, onClick = onClick)
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
