package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Kitesurfing
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.AlwaysExpandMiniPlayerKey
import com.ljyh.mei.constants.LiquidGlassEnabledKey
import com.ljyh.mei.constants.DefaultThemeColor
import com.ljyh.mei.constants.MeshFlowSpeedKey
import com.ljyh.mei.constants.MeshPlayingKey
import com.ljyh.mei.constants.MeshRenderScaleKey
import com.ljyh.mei.constants.MeshStaticModeKey
import com.ljyh.mei.constants.MeshSubdivisionKey
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.constants.DynamicThemeKey
import com.ljyh.mei.constants.ThemeColorKey
import com.ljyh.mei.constants.AppleDynamicCoverKey
import com.ljyh.mei.constants.DynamicCoverEnabledKey
import com.ljyh.mei.constants.NeteaseDynamicCoverKey
import com.ljyh.mei.constants.AppleMotionEnglishTitlesOnlyKey
import com.ljyh.mei.constants.OriginalCoverKey
import com.ljyh.mei.constants.HidePlayerTitleBracketsKey
import com.ljyh.mei.constants.PlaylistCoverStyle
import com.ljyh.mei.constants.PlaylistCoverStyleKey
import com.ljyh.mei.constants.PlaylistTrackTableHeaderKey
import com.ljyh.mei.constants.ProgressBarStyle
import com.ljyh.mei.constants.ProgressBarStyleKey
import com.ljyh.mei.constants.TabletAnimationStyle
import com.ljyh.mei.constants.TabletAnimationStyleKey
import com.ljyh.mei.constants.PageTransitionStyle
import com.ljyh.mei.constants.PageTransitionStyleKey
import com.ljyh.mei.constants.PageTransitionSpeed
import com.ljyh.mei.constants.PageTransitionSpeedKey
import com.ljyh.mei.ui.screen.setting.component.EnumListPreference
import com.ljyh.mei.ui.screen.setting.component.dialog.ColorPickerDialog
import com.ljyh.mei.ui.screen.setting.component.dialog.ColorPreset
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.screen.setting.component.ListPreference
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.ui.screen.setting.component.PreferenceEntry
import com.ljyh.mei.ui.screen.setting.component.SwitchPreference
import com.ljyh.mei.ui.local.LocalNavController
import androidx.compose.foundation.layout.fillMaxSize
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.screen.setting.component.ColorPreference
import com.ljyh.mei.ui.screen.setting.component.DynamicCoverPreference
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberDynamicCoverSettings
import com.ljyh.mei.utils.preferences.rememberPreference
import com.ljyh.mei.utils.preferences.dataStore
import kotlinx.coroutines.launch

private val themeColorPresets = listOf(
    ColorPreset("蓝色", Color(0xFF0061A4)),
    ColorPreset("紫色", Color(0xFF6750A4)),
    ColorPreset("红色", Color(0xFFB3261E)),
    ColorPreset("洋红", Color(0xFFB91690)),
    ColorPreset("青绿", Color(0xFF008577)),
    ColorPreset("绿色", Color(0xFF34883F)),
    ColorPreset("琥珀", Color(0xFFB78100)),
    ColorPreset("橙色", Color(0xFFC75B00)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val (glassEnabled, onGlassEnabledChange) = rememberPreference(LiquidGlassEnabledKey, defaultValue = true)

    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(
        DynamicThemeKey,
        defaultValue = true
    )
    val (themeColor, onThemeColorChange) = rememberPreference(
        ThemeColorKey,
        defaultValue = DefaultThemeColor,
    )
    val selectedThemeColor = remember(themeColor) {
        runCatching { Color(android.graphics.Color.parseColor(themeColor)) }
            .getOrDefault(Color(0xFF0061A4))
    }
    var showThemeColorPicker by remember { mutableStateOf(false) }

    val (originalCover, onOriginalCover) = rememberPreference(
        OriginalCoverKey,
        defaultValue = false
    )
    val dynamicCoverSettings by rememberDynamicCoverSettings()
    val (appleEnglishTitlesOnly, onAppleEnglishTitlesOnlyChange) = rememberPreference(
        AppleMotionEnglishTitlesOnlyKey,
        defaultValue = false
    )

    val (alwaysExpandMiniPlayer, onAlwaysExpandMiniPlayerChange) = rememberPreference(
        AlwaysExpandMiniPlayerKey, defaultValue = false
    )
    val (hideTitleBrackets, onHideTitleBracketsChange) = rememberPreference(
        HidePlayerTitleBracketsKey, defaultValue = false
    )
    val (debug, onDebug) = rememberPreference(
        DebugKey, defaultValue = false
    )
    val (progressBarStyle, onProgressBarStyleChange) = rememberEnumPreference(
        key = ProgressBarStyleKey,
        defaultValue = ProgressBarStyle.LINEAR // 默认值设为你喜欢的
    )

    val (tabletAnimStyle, onTabletAnimStyleChange) = rememberEnumPreference(
        key = TabletAnimationStyleKey,
        defaultValue = TabletAnimationStyle.FLIP_3D
    )

    val (pageTransitionStyle, onPageTransitionStyleChange) = rememberEnumPreference(
        PageTransitionStyleKey, PageTransitionStyle.Slide,
    )
    val (pageTransitionSpeed, onPageTransitionSpeedChange) = rememberEnumPreference(
        PageTransitionSpeedKey, PageTransitionSpeed.Standard,
    )

    val (playlistStyle, onPlaylistStyleChange) = rememberEnumPreference(
        key = PlaylistCoverStyleKey,
        defaultValue = PlaylistCoverStyle.Cover
    )

    val (playlistTrackTableHeader, onPlaylistTrackTableHeaderChange) = rememberPreference(
        key = PlaylistTrackTableHeaderKey,
        defaultValue = false
    )
    val (meshFlowSpeed, onMeshFlowSpeedChange) = rememberPreference(
        MeshFlowSpeedKey, defaultValue = 0.25f
    )
    val (meshRenderScale, onMeshRenderScaleChange) = rememberPreference(
        MeshRenderScaleKey, defaultValue = 0.75f
    )
    val (meshStaticMode, onMeshStaticModeChange) = rememberPreference(
        MeshStaticModeKey, defaultValue = false
    )
    val (meshPlaying, onMeshPlayingChange) = rememberPreference(
        MeshPlayingKey, defaultValue = true
    )
    val (meshSubdivision, onMeshSubdivisionChange) = rememberPreference(
        MeshSubdivisionKey, defaultValue = 50
    )

    MeiPinnedPage(
        title = "外观",
        onNavigateBack = navController::navigateUp,
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(LocalMeiPageScroll.current!!)
                .padding(paddingValues)
        ) {
            MeiLargeTitle("外观")


            PreferenceSection("主题") {
                SwitchPreference(
                    title = { Text("液态玻璃") },
                    description = "导航、搜索与播放控件使用玻璃材质；关闭后使用实色外观",
                    icon = { Icon(Icons.Rounded.BlurOn, contentDescription = null) },
                    checked = glassEnabled,
                    onCheckedChange = onGlassEnabledChange,
                )
                if(glassEnabled){
                    PreferenceEntry(
                        title = { Text("玻璃材质") },
                        description = "透明度、折射与清晰度",
                        icon = { Icon(Icons.Rounded.Visibility, contentDescription = null) },
                        onClick = { navController.navigate(Screen.GlassSettings.route) },
                    )
                }
                SwitchPreference(
                    title = { Text("动态主题") },
                    icon = { Icon(Icons.Rounded.Palette, null) },
                    checked = dynamicTheme,
                    onCheckedChange = onDynamicThemeChange
                )
                if (!dynamicTheme) {
                    ColorPreference(
                        title = "主题颜色",
                        color = selectedThemeColor,
                        description = "#%06X".format(selectedThemeColor.toArgb() and 0xFFFFFF),
                        onClick = { showThemeColorPicker = true },
                    )
                }
            }
            PreferenceSection("页面动画") {
                EnumListPreference(
                    title = { Text("切换样式") },
                    icon = { Icon(Icons.Rounded.SwapHoriz, null) },
                    selectedValue = pageTransitionStyle,
                    onValueSelected = onPageTransitionStyleChange,
                    valueText = { it.label },
                )
                EnumListPreference(
                    title = { Text("动画速度") },
                    icon = { Icon(Icons.Rounded.Speed, null) },
                    selectedValue = pageTransitionSpeed,
                    onValueSelected = onPageTransitionSpeedChange,
                    valueText = { it.label },
                    isEnabled = pageTransitionStyle != PageTransitionStyle.None,
                )
            }
            PreferenceSection("歌单") {

                EnumListPreference(
                    title = { Text("歌单封面样式") },
                    icon = { Icon(Icons.Rounded.Image, null) },
                    selectedValue = playlistStyle,
                    onValueSelected = onPlaylistStyleChange,
                    valueText = {
                        when (it) {
                            PlaylistCoverStyle.Cover -> "封面"
                            PlaylistCoverStyle.FirstSongImage -> "第一首歌曲封面"
                            PlaylistCoverStyle.Combination -> "组合图片"
                        }
                    }
                )

                SwitchPreference(
                    title = { Text("歌单开启表头") },
                    description = "平板模式下歌单显示表头",
                    icon = { Icon(Icons.Rounded.FormatBold, null) },
                    checked = playlistTrackTableHeader,
                    onCheckedChange = onPlaylistTrackTableHeaderChange
                )
            }
            PreferenceSection("播放界面") {
                SwitchPreference(
                    title = { Text("始终展开底部播放条") },
                    description = "保持播放条与底部导航展开，不随页面滑动收起",
                    icon = { Icon(Icons.Rounded.MusicVideo, null) },
                    checked = alwaysExpandMiniPlayer,
                    onCheckedChange = onAlwaysExpandMiniPlayerChange,
                )
                SwitchPreference(
                    title = { Text("隐藏歌名括号内容") },
                    description = "在迷你播放条和完整播放器标题中隐藏中英文括号及其中内容",
                    icon = { Icon(Icons.Rounded.FormatBold, null) },
                    checked = hideTitleBrackets,
                    onCheckedChange = onHideTitleBracketsChange,
                )

                // 原图封面
                SwitchPreference(
                    title = { Text("使用原图加载封面") },
                    icon = { Icon(Icons.Rounded.Image, null) },
                    checked = originalCover,
                    onCheckedChange = onOriginalCover
                )
                DynamicCoverPreference(
                    settings = dynamicCoverSettings,
                    englishTitlesOnly = appleEnglishTitlesOnly,
                    onEnabledChange = { enabled ->
                        scope.launch {
                            context.dataStore.edit {
                                it[DynamicCoverEnabledKey] = enabled
                            }
                        }
                    },
                    onAppleMusicChange = { enabled ->
                        scope.launch {
                            context.dataStore.edit {
                                it[AppleDynamicCoverKey] = enabled
                            }
                        }
                    },
                    onNeteaseChange = { enabled ->
                        scope.launch {
                            context.dataStore.edit {
                                it[NeteaseDynamicCoverKey] = enabled
                            }
                        }
                    },
                    onEnglishTitlesOnlyChange = onAppleEnglishTitlesOnlyChange,
                )
                EnumListPreference(
                    title = { Text("进度条样式") },
                    icon = { Icon(Icons.Rounded.LinearScale, null) },
                    selectedValue = progressBarStyle,
                    onValueSelected = onProgressBarStyleChange,
                    valueText = { it.label }
                )
                EnumListPreference(
                    title = { Text("平板切换动画") },
                    icon = { Icon(Icons.Rounded.SwapHoriz, null) },
                    selectedValue = tabletAnimStyle,
                    onValueSelected = onTabletAnimStyleChange,
                    valueText = { it.label }
                )
            }
            PreferenceSection("流动背景") {
                ListPreference(
                    title = { Text("流动速度") },
                    description = "渐变动画速度，值越大流动越快",
                    icon = { Icon(Icons.Rounded.Speed, null) },
                    selectedValue = meshFlowSpeed,
                    values = listOf(0.05f, 0.10f, 0.15f, 0.25f, 0.5f),
                    onValueSelected = onMeshFlowSpeedChange,
                    valueText = {
                        when (it) {
                            0.05f -> "极慢"
                            0.10f -> "慢"
                            0.15f -> "标准"
                            0.25f -> "快"
                            0.5f -> "极快"
                            else -> "${(it * 100).toInt()}%"
                        }
                    }
                )
                ListPreference(
                    title = { Text("渲染精度") },
                    description = "渲染分辨率缩放，越低越省电但越模糊",
                    icon = { Icon(Icons.Rounded.Visibility, null) },
                    selectedValue = meshRenderScale,
                    values = listOf(0.25f, 0.5f, 0.75f, 1.0f),
                    onValueSelected = onMeshRenderScaleChange,
                    valueText = { "${(it * 100).toInt()}%" }
                )
                ListPreference(
                    title = { Text("网格细分") },
                    description = "网格细分级数，越大越平滑越耗GPU",
                    icon = { Icon(Icons.Rounded.GridOn, null) },
                    selectedValue = meshSubdivision,
                    values = (1..8).map { it * 10 },
                    onValueSelected = onMeshSubdivisionChange,
                    valueText = { "$it" }
                )
                SwitchPreference(
                    title = { Text("静态模式") },
                    description = "过渡完成后停止动画，省电",
                    icon = { Icon(Icons.Rounded.Timer, null) },
                    checked = meshStaticMode,
                    onCheckedChange = onMeshStaticModeChange
                )
                SwitchPreference(
                    title = { Text("播放动画") },
                    description = "暂停/恢复背景渲染",
                    icon = { Icon(Icons.Rounded.MusicVideo, null) },
                    checked = meshPlaying,
                    onCheckedChange = onMeshPlayingChange
                )
            }
            PreferenceSection("歌词") {
                PreferenceEntry(
                    title = { Text("歌词设置") },
                    description = "歌词外观与桌面歌词",
                    icon = { Icon(Icons.Rounded.Palette, null) },
                    onClick = { Screen.LyricsSettings.navigate(navController) }
                )
            }
            PreferenceSection("调试") {

                SwitchPreference(
                    title = { Text("Debug") },
                    description = "显示歌曲和歌单菜单中的调试复制操作",
                    icon = { Icon(Icons.Rounded.Kitesurfing, null) },
                    checked = debug,
                    onCheckedChange = onDebug
                )
            }
        }
    }

    if (showThemeColorPicker && !dynamicTheme) {
        ColorPickerDialog(
            title = "选择主题色",
            initialColor = selectedThemeColor,
            presets = themeColorPresets,
            onDismiss = { showThemeColorPicker = false },
            onApply = { color ->
                onThemeColorChange("#%08X".format(color.toArgb()))
                showThemeColorPicker = false
            },
        )
    }


}
