package com.ljyh.mei.ui.screen.setting

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ljyh.mei.constants.DefaultThemeColor
import com.ljyh.mei.constants.DynamicThemeKey
import com.ljyh.mei.constants.LiquidGlassEnabledKey
import com.ljyh.mei.constants.ThemeColorKey
import com.ljyh.mei.constants.ThemeMode
import com.ljyh.mei.constants.ThemeModeKey
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.setting.component.ColorPreference
import com.ljyh.mei.ui.screen.setting.component.EnumListPreference
import com.ljyh.mei.ui.screen.setting.component.PreferenceEntry
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.ui.screen.setting.component.SwitchPreference
import com.ljyh.mei.ui.screen.setting.component.dialog.ColorPickerDialog
import com.ljyh.mei.ui.screen.setting.component.dialog.ColorPreset
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference

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
fun AppearanceSettingsScreen(scrollBehavior: TopAppBarScrollBehavior) {
    val navController = LocalNavController.current
    val (glassEnabled, onGlassEnabledChange) = rememberPreference(LiquidGlassEnabledKey, defaultValue = true)

    val (themeMode, onThemeModeChange) = rememberEnumPreference(ThemeModeKey, ThemeMode.System)
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
    AppearanceSettingsPage("外观") {
        PreferenceSection("主题与材质") {
            EnumListPreference(
                title = { Text("主题模式") },
                icon = { Icon(Icons.Rounded.Contrast, null) },
                selectedValue = themeMode,
                valueText = { it.label },
                onValueSelected = onThemeModeChange,
            )
            SwitchPreference(
                title = { Text("液态玻璃") },
                description = "导航、搜索与播放控件使用玻璃材质；关闭后使用实色外观",
                icon = { Icon(Icons.Rounded.BlurOn, contentDescription = null) },
                checked = glassEnabled,
                onCheckedChange = onGlassEnabledChange,
            )
            if (glassEnabled) {
                PreferenceEntry(
                    title = { Text("玻璃材质") },
                    description = "透明度、折射与清晰度",
                    icon = { Icon(Icons.Rounded.Visibility, contentDescription = null) },
                    trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
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
        PreferenceSection("更多外观设置") {
            PreferenceEntry(
                title = { Text("封面与图片") },
                description = "歌单封面、专辑展示与歌手人像",
                icon = { Icon(Icons.Rounded.Image, null) },
                trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                onClick = { Screen.ArtworkSettings.navigate(navController) },
            )
            PreferenceEntry(
                title = { Text("播放界面") },
                description = "播放条、列表样式与流动背景",
                icon = { Icon(Icons.Rounded.MusicVideo, null) },
                trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                onClick = { Screen.PlayerAppearanceSettings.navigate(navController) },
            )
            PreferenceEntry(
                title = { Text("动画与动效") },
                description = "页面切换与播放器动画",
                icon = { Icon(Icons.Rounded.SwapHoriz, null) },
                trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                onClick = { Screen.AnimationSettings.navigate(navController) },
            )
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
