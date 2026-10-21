package com.ljyh.mei.ui.screen.setting

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ljyh.mei.constants.UsbExclusivePlaybackKey
import com.ljyh.mei.playback.UsbOutputState
import com.ljyh.mei.playback.usb.*
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.screen.setting.component.ListPreference
import com.ljyh.mei.ui.screen.setting.component.PreferenceEntry
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.ui.screen.setting.component.SwitchPreference
import com.ljyh.mei.utils.preferences.dataStore
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbExclusiveSettingsScreen(scrollBehavior: TopAppBarScrollBehavior) {
    val context = LocalContext.current
    val nav = LocalNavController.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val stored by remember(context) { context.dataStore.data }.collectAsState(initial = emptyPreferences())
    val settings = stored.usbPlaybackPreferences()
    val enabled = stored[UsbExclusivePlaybackKey] ?: false
    val status by UsbOutputState.status.collectAsState()
    val keepAliveStatus by UsbKeepAliveState.message.collectAsState()
    var overlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var batteryExempt by remember { mutableStateOf(false) }
    var backgroundRestricted by remember { mutableStateOf(false) }
    var showBackgroundGuide by remember { mutableStateOf(false) }
    fun refreshPermissions() {
        overlays = Settings.canDrawOverlays(context)
        batteryExempt = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        backgroundRestricted = context.getSystemService(ActivityManager::class.java).isBackgroundRestricted
    }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refreshPermissions() }
        lifecycle.addObserver(observer)
        refreshPermissions()
        onDispose { lifecycle.removeObserver(observer) }
    }
    val systemLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refreshPermissions() }
    fun openSystemSettings(intent: Intent) {
        runCatching { systemLauncher.launch(intent) }.onFailure {
            Toast.makeText(context, "无法打开系统设置，请在应用信息中手动设置", Toast.LENGTH_LONG).show()
        }
    }
    fun requestOverlayPermission() = openSystemSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))

    if (showBackgroundGuide) AlertDialog(
        onDismissRequest = { showBackgroundGuide = false },
        title = { Text("允许后台播放") },
        text = { Text("在 Mei 的电池用量设置中选择“不受限制”，并在系统提供相关选项时允许后台运行和自启动。\n\n若锁屏后 USB 播放仍会中断，可尝试悬浮保活；效果取决于手机的后台管理策略。") },
        confirmButton = {
            TextButton(onClick = {
                showBackgroundGuide = false
                openSystemSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }) { Text("应用信息") }
        },
        dismissButton = {
            TextButton(onClick = {
                showBackgroundGuide = false
                openSystemSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }) { Text("电池优化") }
        },
    )
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("USB 独占播放") },
            navigationIcon = {
                IconButton(onClick = nav::navigateUp, onLongClick = nav::backToMain) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }) { padding ->
        Column(Modifier.padding(padding)
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())) {
            PreferenceSection("音频输出") {
                SwitchPreference(
                    title = { Text("USB 独占播放") },
                    description = "通过 USB 直连解码器，独占播放时停用均衡器与歌曲过渡",
                    icon = { Icon(Icons.Rounded.Usb, null) },
                    checked = enabled,
                    onCheckedChange = { next -> scope.launch { context.dataStore.edit { it[UsbExclusivePlaybackKey] = next } } },
                )
                PreferenceEntry(title = { Text("输出状态") },
                    description = if (enabled) status.message.ifBlank { "等待连接 USB 解码器" } else "已关闭，使用普通输出")
            }
            PreferenceSection("缓冲") {
                ListPreference(
                    title = { Text("缓冲档位") },
                    selectedValue = settings.profile,
                    values = UsbBufferProfile.entries,
                    valueText = { "${it.label} · 前台 ${it.foregroundMs} ms / 后台 ${it.backgroundMs} ms" },
                    description = settings.profile.label + if (settings.customized) " · 已自定义时长" else " · 前台 ${settings.bufferMs(true)} ms / 后台 ${settings.bufferMs(false)} ms",
                    onValueSelected = { profile -> scope.launch {
                        context.dataStore.edit {
                            it[UsbBufferProfileKey] = profile.name
                            it[UsbForegroundBufferKey] = profile.foregroundMs
                            it[UsbBackgroundBufferKey] = profile.backgroundMs
                        }
                    } },
                )
                UsbBufferSlider("前台缓冲", settings.bufferMs(true), 100..1000,
                    "较低的缓冲可缩短操作后的等待时间") { value -> scope.launch {
                    context.dataStore.edit { it[UsbForegroundBufferKey] = normalizeForegroundBuffer(value) }
                } }
                UsbBufferSlider("后台缓冲", settings.bufferMs(false), 200..3000,
                    "锁屏或切到其他应用时使用；较大的缓冲更能抵御短暂调度延迟") { value -> scope.launch {
                    context.dataStore.edit { it[UsbBackgroundBufferKey] = normalizeBackgroundBuffer(value) }
                } }
            }
            PreferenceSection("格式兼容") {
                SwitchPreference(title = { Text("采样率兼容") }, checked = settings.sampleRateCompatibility,
                    description = "音源采样率不受支持时，转换到解码器支持的采样率",
                    onCheckedChange = { value -> scope.launch { context.dataStore.edit { it[UsbSampleRateCompatibilityKey] = value } } })
                SwitchPreference(title = { Text("位深兼容") }, checked = settings.bitDepthCompatibility,
                    description = "允许扩展或转换位深，以适配解码器支持的格式",
                    onCheckedChange = { value -> scope.launch { context.dataStore.edit { it[UsbBitDepthCompatibilityKey] = value } } })
                SwitchPreference(title = { Text("声道兼容") }, checked = settings.channelCompatibility,
                    description = "允许单声道与双声道转换，或将立体声送到多声道设备的前两个声道",
                    onCheckedChange = { value -> scope.launch { context.dataStore.edit { it[UsbChannelCompatibilityKey] = value } } })
                PreferenceEntry(title = { Text("兼容设置生效方式") },
                    description = "修改后重新连接当前输出；关闭兼容时，格式需与解码器匹配。USB 启动失败时会暂停并切回普通输出。")
            }
            PreferenceSection("后台运行") {
                PreferenceEntry(title = { Text("后台运行设置") },
                    icon = { Icon(Icons.Rounded.BatterySaver, null) },
                    description = when {
                        backgroundRestricted -> "系统限制后台运行，点按查看设置方法"
                        !batteryExempt -> "电池优化仍开启，锁屏播放中断时可调整"
                        else -> "已忽略电池优化；厂商后台管理需在系统中确认"
                    },
                    onClick = { showBackgroundGuide = true })
                SwitchPreference(title = { Text("悬浮保活") }, checked = settings.floatingKeepAlive,
                    icon = { Icon(Icons.Rounded.PictureInPictureAlt, null) },
                    description = "USB 在后台播放时使用不可触摸的透明悬浮窗，可能改善部分手机的锁屏断流",
                    onCheckedChange = { value ->
                        scope.launch { context.dataStore.edit { it[UsbFloatingKeepAliveKey] = value } }
                        if (value && !overlays) requestOverlayPermission()
                    })
                if (settings.floatingKeepAlive) PreferenceEntry(
                    title = { Text(if (overlays) "保活状态" else "允许悬浮窗权限") },
                    description = if (!overlays) "尚未授权，悬浮保活未运行；点按前往授权" else keepAliveStatus,
                    onClick = if (!overlays) ({ requestOverlayPermission() }) else null,
                )
            }
        }
    }
}

@Composable
private fun UsbBufferSlider(title: String, value: Int, range: IntRange, description: String, onCommit: (Int) -> Unit) {
    var draft by remember { mutableFloatStateOf(value.toFloat()) }
    LaunchedEffect(value) { draft = value.toFloat() }
    PreferenceEntry(title = { Text(title) }, description = description, content = {
        Text("${draft.roundToInt()} ms", color = MaterialTheme.colorScheme.primary)
        Slider(value = draft, onValueChange = { draft = (it.roundToInt() / 50 * 50).toFloat() },
            onValueChangeFinished = { onCommit(draft.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first) / 50 - 1,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = title })
    })
}
