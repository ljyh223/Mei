package com.ljyh.mei.ui.screen.setting

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ljyh.mei.constants.AccompanimentLyricTextBoldKey
import com.ljyh.mei.constants.AccompanimentLyricTextSizeKey
import com.ljyh.mei.constants.DesktopLyricsControlsHideDelayKey
import com.ljyh.mei.constants.DesktopLyricsEnabledKey
import com.ljyh.mei.constants.DesktopLyricsFontSizeKey
import com.ljyh.mei.constants.DesktopLyricsLockedKey
import com.ljyh.mei.constants.DesktopLyricsTextColorKey
import com.ljyh.mei.constants.DesktopLyricsTranslationColorKey
import com.ljyh.mei.constants.DesktopLyricsTranslationFontSizeKey
import com.ljyh.mei.constants.DefaultDesktopLyricsControlsHideDelay
import com.ljyh.mei.constants.DefaultDesktopLyricsFontSize
import com.ljyh.mei.constants.DefaultDesktopLyricsTextColor
import com.ljyh.mei.constants.DefaultDesktopLyricsTranslationColor
import com.ljyh.mei.constants.DefaultDesktopLyricsTranslationFontSize
import com.ljyh.mei.constants.LyricTextSize
import com.ljyh.mei.constants.NormalLyricTextBoldKey
import com.ljyh.mei.constants.NormalLyricTextSizeKey
import com.ljyh.mei.constants.SeekOnLyricTapKey
import com.ljyh.mei.constants.ShowLyricPhoneticKey
import com.ljyh.mei.ui.screen.setting.component.EnumListPreference
import com.ljyh.mei.ui.screen.setting.component.dialog.ColorPickerDialog
import com.ljyh.mei.ui.screen.setting.component.dialog.ColorPreset
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.screen.setting.component.ListPreference
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.ui.screen.setting.component.SwitchPreference
import com.ljyh.mei.ui.local.LocalNavController
import androidx.compose.foundation.layout.fillMaxSize
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference
import androidx.compose.ui.graphics.toArgb
import com.ljyh.mei.ui.screen.setting.component.ColorPreference

private val desktopLyricColorPresets = listOf(
    ColorPreset("白色", Color(0xFFFFFFFF)),
    ColorPreset("浅灰", Color(0xFFCECED3)),
    ColorPreset("米白", Color(0xFFFFE9C7)),
    ColorPreset("金黄", Color(0xFFFFD166)),
    ColorPreset("薄荷", Color(0xFFB8F2D0)),
    ColorPreset("浅蓝", Color(0xFFADD8FF)),
    ColorPreset("淡紫", Color(0xFFD7C6FF)),
    ColorPreset("深灰", Color(0xFF30343B)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettingsScreen(scrollBehavior: TopAppBarScrollBehavior) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val (mainSize, onMainSize) = rememberEnumPreference(NormalLyricTextSizeKey, LyricTextSize.Size34)
    val (mainBold, onMainBold) = rememberPreference(NormalLyricTextBoldKey, true)
    val (showPhonetic, onShowPhonetic) = rememberPreference(ShowLyricPhoneticKey, true)
    val (seekOnLyricTap, onSeekOnLyricTap) = rememberPreference(SeekOnLyricTapKey, true)
    val (translationSize, onTranslationSize) = rememberEnumPreference(
        AccompanimentLyricTextSizeKey, LyricTextSize.Size20,
    )
    val (translationBold, onTranslationBold) = rememberPreference(AccompanimentLyricTextBoldKey, true)
    val (enabled, onEnabled) = rememberPreference(DesktopLyricsEnabledKey, false)
    val (locked, onLocked) = rememberPreference(DesktopLyricsLockedKey, false)
    val (mainColor, onMainColor) = rememberPreference(DesktopLyricsTextColorKey, DefaultDesktopLyricsTextColor)
    val (desktopTextSize, onDesktopTextSize) = rememberPreference(
        DesktopLyricsFontSizeKey, DefaultDesktopLyricsFontSize,
    )
    val (translationColor, onTranslationColor) = rememberPreference(
        DesktopLyricsTranslationColorKey, DefaultDesktopLyricsTranslationColor,
    )
    val (desktopTranslationSize, onDesktopTranslationSize) = rememberPreference(
        DesktopLyricsTranslationFontSizeKey, DefaultDesktopLyricsTranslationFontSize,
    )
    val (hideDelay, onHideDelay) = rememberPreference(
        DesktopLyricsControlsHideDelayKey, DefaultDesktopLyricsControlsHideDelay,
    )
    var permissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var waitingForPermission by remember { mutableStateOf(false) }
    var colorTarget by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        permissionGranted = Settings.canDrawOverlays(context)
        if (waitingForPermission && permissionGranted) onEnabled(true)
        waitingForPermission = false
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = Settings.canDrawOverlays(context)
                if (waitingForPermission && permissionGranted) {
                    onEnabled(true)
                    waitingForPermission = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val desktopOptionsEnabled = enabled && permissionGranted

    MeiPinnedPage(
        title = "歌词",
        onNavigateBack = navController::navigateUp,
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(LocalMeiPageScroll.current!!)
                .padding(paddingValues)
        ) {
            MeiLargeTitle("歌词")

            PreferenceSection("主歌词") {
                SwitchPreference(
                    title = { Text("主歌词字体加粗") },
                    icon = { Icon(Icons.Rounded.FormatBold, null) },
                    checked = mainBold,
                    onCheckedChange = onMainBold,
                )
                EnumListPreference(
                    title = { Text("主歌词字体大小") },
                    icon = { Icon(Icons.Rounded.FormatSize, null) },
                    selectedValue = mainSize,
                    onValueSelected = onMainSize,
                    valueText = { it.text.toString() },
                )
                SwitchPreference(
                    title = { Text("显示音译") },
                    description = "歌词提供发音时显示",
                    icon = { Icon(Icons.Rounded.Lyrics, null) },
                    checked = showPhonetic,
                    onCheckedChange = onShowPhonetic,
                )
                SwitchPreference(
                    title = { Text("点击歌词跳转进度") },
                    description = "关闭后点击歌词只显示播放控制",
                    icon = { Icon(Icons.Rounded.Lyrics, null) },
                    checked = seekOnLyricTap,
                    onCheckedChange = onSeekOnLyricTap,
                )
            }
            PreferenceSection("翻译歌词") {
                SwitchPreference(
                    title = { Text("翻译歌词字体加粗") },
                    icon = { Icon(Icons.Rounded.FormatBold, null) },
                    checked = translationBold,
                    onCheckedChange = onTranslationBold,
                )
                EnumListPreference(
                    title = { Text("翻译歌词字体大小") },
                    icon = { Icon(Icons.Rounded.FormatSize, null) },
                    selectedValue = translationSize,
                    onValueSelected = onTranslationSize,
                    valueText = { it.text.toString() },
                )
            }
            PreferenceSection("桌面歌词") {
                SwitchPreference(
                    title = { Text("桌面歌词") },
                    description = if (permissionGranted) "可拖动、锁定并自定义样式" else "开启需授予悬浮窗权限",
                    icon = { Icon(Icons.Rounded.Lyrics, null) },
                    checked = enabled && permissionGranted,
                    onCheckedChange = { checked ->
                        if (!checked) onEnabled(false)
                        else if (Settings.canDrawOverlays(context)) {
                            permissionGranted = true
                            onEnabled(true)
                        } else {
                            waitingForPermission = true
                            permissionLauncher.launch(Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri(),
                            ))
                        }
                    },
                )
                SwitchPreference(
                    title = { Text("启动时锁定") },
                    description = "锁定后只显示歌词并穿透触摸；在此处可解除锁定",
                    icon = { Icon(Icons.Rounded.Lyrics, null) },
                    checked = locked,
                    onCheckedChange = onLocked,
                    isEnabled = desktopOptionsEnabled,
                )
                DesktopLyricColorPreference("桌面歌词颜色", mainColor, desktopOptionsEnabled) {
                    colorTarget = "main"
                }
                ListPreference(
                    title = { Text("桌面歌词字体大小") },
                    icon = { Icon(Icons.Rounded.FormatSize, null) },
                    selectedValue = desktopTextSize,
                    values = listOf(16, 18, 20, 22, 24, 28, 32),
                    valueText = { "$it sp" },
                    onValueSelected = onDesktopTextSize,
                    isEnabled = desktopOptionsEnabled,
                )
                DesktopLyricColorPreference("翻译歌词颜色", translationColor, desktopOptionsEnabled) {
                    colorTarget = "translation"
                }
                ListPreference(
                    title = { Text("翻译歌词字体大小") },
                    icon = { Icon(Icons.Rounded.FormatSize, null) },
                    selectedValue = desktopTranslationSize,
                    values = listOf(12, 13, 14, 16, 18, 20),
                    valueText = { "$it sp" },
                    onValueSelected = onDesktopTranslationSize,
                    isEnabled = desktopOptionsEnabled,
                )
                ListPreference(
                    title = { Text("控制栏自动收起") },
                    description = "收起后点击歌词可重新显示；锁定时只显示歌词",
                    icon = { Icon(Icons.Rounded.Timer, null) },
                    selectedValue = hideDelay,
                    values = listOf(3, 5, 8, 12, 0),
                    valueText = { if (it == 0) "始终显示" else "$it 秒" },
                    onValueSelected = onHideDelay,
                    isEnabled = desktopOptionsEnabled,
                )
            }
        }
    }

    colorTarget?.let { target ->
        val currentValue = if (target == "main") mainColor else translationColor
        val initial = remember(currentValue) {
            runCatching { Color(android.graphics.Color.parseColor(currentValue)) }
                .getOrDefault(Color.White)
        }
        ColorPickerDialog(
            title = if (target == "main") "桌面歌词颜色" else "翻译歌词颜色",
            initialColor = initial,
            presets = desktopLyricColorPresets,
            onDismiss = { colorTarget = null },
            onApply = { selected ->
                val hex = "#%08X".format(selected.toArgb())
                if (target == "main") onMainColor(hex) else onTranslationColor(hex)
                colorTarget = null
            },
        )
    }
}

@Composable
private fun DesktopLyricColorPreference(title: String, value: String, enabled: Boolean, onClick: () -> Unit) {
    val color = remember(value) { runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(Color.White) }
    val presetName = desktopLyricColorPresets.firstOrNull { it.color.toArgb() == color.toArgb() }?.name
    ColorPreference(
        title = title,
        color = color,
        description = presetName ?: "自定义颜色",
        enabled = enabled,
        onClick = onClick,
    )
}
