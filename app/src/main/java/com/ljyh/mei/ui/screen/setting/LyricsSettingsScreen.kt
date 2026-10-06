package com.ljyh.mei.ui.screen.setting

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.ljyh.mei.ui.component.EnumListPreference
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.ListPreference
import com.ljyh.mei.ui.component.PreferenceGroupTitle
import com.ljyh.mei.ui.component.SwitchPreference
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.utils.rememberEnumPreference
import com.ljyh.mei.utils.rememberPreference
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.ui.HslColorPicker

private data class DesktopLyricColorPreset(val name: String, val hex: String)

private val desktopLyricColorPresets = listOf(
    DesktopLyricColorPreset("白色", DefaultDesktopLyricsTextColor),
    DesktopLyricColorPreset("浅灰", DefaultDesktopLyricsTranslationColor),
    DesktopLyricColorPreset("米白", "#FFFFE9C7"),
    DesktopLyricColorPreset("金黄", "#FFFFD166"),
    DesktopLyricColorPreset("薄荷", "#FFB8F2D0"),
    DesktopLyricColorPreset("浅蓝", "#FFADD8FF"),
    DesktopLyricColorPreset("淡紫", "#FFD7C6FF"),
    DesktopLyricColorPreset("深灰", "#FF30343B"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettingsScreen(scrollBehavior: TopAppBarScrollBehavior) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val (mainSize, onMainSize) = rememberEnumPreference(NormalLyricTextSizeKey, LyricTextSize.Size34)
    val (mainBold, onMainBold) = rememberPreference(NormalLyricTextBoldKey, true)
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
    var showCustomColorPicker by remember { mutableStateOf(false) }
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

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("歌词设置") },
            navigationIcon = {
                IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }) { padding ->
        Column(
            Modifier.padding(padding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState()),
        ) {
            PreferenceGroupTitle(title = "PLAYER LYRICS")
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

            PreferenceGroupTitle(title = "DESKTOP LYRICS")
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
            ColorPreference("桌面歌词颜色", mainColor, desktopOptionsEnabled) {
                colorTarget = "main"
                showCustomColorPicker = false
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
            ColorPreference("翻译歌词颜色", translationColor, desktopOptionsEnabled) {
                colorTarget = "translation"
                showCustomColorPicker = false
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

    colorTarget?.let { target ->
        val currentValue = if (target == "main") mainColor else translationColor
        if (showCustomColorPicker) {
            ColorPickerDialog(currentValue, onDismiss = { showCustomColorPicker = false }) { selected ->
                val hex = "#%08X".format(selected.toArgb())
                if (target == "main") onMainColor(hex) else onTranslationColor(hex)
                colorTarget = null
                showCustomColorPicker = false
            }
        } else {
            ColorPresetDialog(
                title = if (target == "main") "桌面歌词颜色" else "翻译歌词颜色",
                currentValue = currentValue,
                onDismiss = { colorTarget = null },
                onCustom = { showCustomColorPicker = true },
                onPresetSelected = { hex ->
                    if (target == "main") onMainColor(hex) else onTranslationColor(hex)
                    colorTarget = null
                },
            )
        }
    }
}

@Composable
private fun ColorPreference(title: String, value: String, enabled: Boolean, onClick: () -> Unit) {
    val color = remember(value) { runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(Color.White) }
    val presetName = desktopLyricColorPresets.firstOrNull { it.hex.equals(value, ignoreCase = true) }?.name
    com.ljyh.mei.ui.component.PreferenceEntry(
        title = { Text(title) },
        description = presetName ?: "自定义颜色",
        icon = {
            Box(
                Modifier.size(34.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .padding(2.dp)
                    .background(color, CircleShape),
            )
        },
        trailingContent = { Text("更改", color = MaterialTheme.colorScheme.primary) },
        onClick = onClick,
        isEnabled = enabled,
    )
}

@Composable
private fun ColorPresetDialog(
    title: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onCustom: () -> Unit,
    onPresetSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 4,
            ) {
                desktopLyricColorPresets.forEach { preset ->
                    val selected = preset.hex.equals(currentValue, ignoreCase = true)
                    val argb = android.graphics.Color.parseColor(preset.hex)
                    val color = Color(argb)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(64.dp)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onPresetSelected(preset.hex) },
                            )
                            .padding(vertical = 4.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(46.dp)
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape,
                                )
                                .padding(4.dp)
                                .background(color, CircleShape),
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = if (ColorUtils.calculateLuminance(argb) > 0.5) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Text(preset.name, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCustom) { Text("自定义颜色") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ColorPickerDialog(initial: String, onDismiss: () -> Unit, onApply: (Color) -> Unit) {
    val initialColor = remember(initial) {
        val hsl = FloatArray(3)
        val argb = runCatching { android.graphics.Color.parseColor(initial) }.getOrDefault(android.graphics.Color.WHITE)
        ColorUtils.colorToHSL(argb, hsl)
        HslColor(hue = hsl[0], saturation = hsl[1], lightness = hsl[2])
    }
    var selected by remember(initialColor) { mutableStateOf(initialColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择歌词颜色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val selectedComposeColor = Color(ColorUtils.HSLToColor(
                    floatArrayOf(selected.hue, selected.saturation, selected.lightness),
                ))
                Text("歌词颜色预览", color = selectedComposeColor, fontWeight = FontWeight.Bold)
                HslColorPicker(color = selected, onColorChange = { selected = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(Color(ColorUtils.HSLToColor(
                    floatArrayOf(selected.hue, selected.saturation, selected.lightness),
                )))
            }) { Text("应用") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
