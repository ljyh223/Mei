package com.ljyh.mei.ui.screen.setting

import com.ljyh.mei.ui.component.glass.MeiTopAppBar
import android.os.Build
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.LiquidGlassEnabledKey
import com.ljyh.mei.ui.component.glass.GlassAppearance
import com.ljyh.mei.ui.component.glass.LocalMeiGlassAppearance
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.glass.LocalMeiHazeState
import com.ljyh.mei.ui.component.glass.MeiGlassRole
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.local.LocalNavController
import androidx.compose.foundation.layout.asPaddingValues
import com.ljyh.mei.ui.component.glass.MeiPinnedLazyPage
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.setting.component.ListPreference
import com.ljyh.mei.ui.screen.setting.component.PreferenceEntry
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.ui.screen.setting.component.SwitchPreference
import com.ljyh.mei.utils.preferences.rememberGlassAppearance
import com.ljyh.mei.utils.preferences.rememberPreference
import com.ljyh.mei.utils.preferences.saveGlassAppearance
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSettingsScreen() {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved by rememberGlassAppearance()
    var draft by remember(saved) { mutableStateOf(saved) }
    val (enabled, onEnabledChange) = rememberPreference(LiquidGlassEnabledKey, true)
    val live = LocalMeiGlassEnabled.current
    val save = {
        val value = draft
        scope.launch { context.saveGlassAppearance(value) }
        Unit
    }

    MeiPinnedLazyPage(
        title = "玻璃材质",
        bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
        horizontalContentPadding = 20.dp,
        largeTitleHorizontalPadding = 0.dp,
        onNavigateBack = navController::navigateUp,
    ) {
        item(key = "page-content") {
            Column {
            CompositionLocalProvider(LocalMeiGlassAppearance provides draft) {
                GlassMaterialPreview()
            }
            if (!live) Text(
                text = when {
                    !enabled -> "流体玻璃已关闭，当前显示静态样式"
                    Build.VERSION.SDK_INT < 33 -> "此设备显示静态样式，实时玻璃需要 Android 13 或更新版本"
                    else -> "当前省电或设备内存策略已简化玻璃效果"
                },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PreferenceSection("外观") {
                SwitchPreference(
                    title = { Text("流体玻璃") }, checked = enabled, onCheckedChange = onEnabledChange,
                )
                ListPreference(
                    title = { Text("材质预设") },
                    selectedValue = draft,
                    values = listOf(GlassAppearance.Clear, GlassAppearance.Balanced, GlassAppearance.Readable),
                    valueText = {
                        when (it) {
                            GlassAppearance.Clear -> "通透"
                            GlassAppearance.Balanced -> "均衡"
                            GlassAppearance.Readable -> "清晰"
                            else -> "自定义"
                        }
                    },
                    onValueSelected = { draft = it; save() },
                )
                SwitchPreference(
                    title = { Text("增强清晰度") },
                    description = "提高文字与图标对比度，并加强底色遮蔽",
                    checked = draft.strongContent,
                    onCheckedChange = { draft = draft.copy(strongContent = it); save() },
                )
            }
            PreferenceSection("材质细节") {
                GlassAdjustment("底色浓度", "越高越能遮住背景，文字更容易辨认", draft.opacity, 0f..1f,
                    { draft = draft.copy(opacity = it) }, save)
                GlassAdjustment("背景模糊", "越高越能柔化背后的图片和文字", draft.blur, 0f..40f,
                    { draft = draft.copy(blur = it) }, save, percent = false)
                GlassAdjustment("折射强度", "调整玻璃内背景的弯曲程度", draft.refraction, 0f..1f,
                    { draft = draft.copy(refraction = it) }, save)
                GlassAdjustment("高光强度", "调整玻璃表面的光泽", draft.highlight, 0f..1f,
                    { draft = draft.copy(highlight = it) }, save)
                GlassAdjustment("背景色彩", "越低越接近中性，越高越保留背景颜色", draft.saturation, 0f..1f,
                    { draft = draft.copy(saturation = it) }, save)
                PreferenceEntry(
                    title = { Text("恢复默认") }, description = "恢复均衡材质与增强清晰度",
                    onClick = { draft = GlassAppearance.Balanced; save() },
                )
            }
            Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GlassAdjustment(
    title: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
    percent: Boolean = true,
) {
    PreferenceEntry(title = { Text(title) }, description = description, content = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Slider(
                value = value,
                onValueChange = onChange,
                onValueChangeFinished = onFinished,
                valueRange = range,
                modifier = Modifier.weight(1f).semantics { contentDescription = title },
            )
            Text(
                if (percent) "${(value * 100).roundToInt()}%" else value.roundToInt().toString(),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    })
}

@Composable
private fun GlassMaterialPreview() {
    val source = rememberHazeState()
    val live = LocalMeiGlassEnabled.current
    val appearance = LocalMeiGlassAppearance.current
    val ink = if (appearance.strongContent) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("材质预览", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth().height(144.dp).clip(MaterialTheme.shapes.large)) {
            Box(
                Modifier.fillMaxSize().then(if (live) Modifier.hazeSource(source) else Modifier)
                    .background(Brush.linearGradient(listOf(Color(0xFF204978), Color(0xFFCF8569), Color(0xFFEDD6A3)))),
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround) {
                    Box(Modifier.size(72.dp).background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp)))
                    Box(Modifier.align(Alignment.Bottom).size(88.dp).background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp)))
                }
            }
            CompositionLocalProvider(LocalMeiHazeState provides source) {
                MeiGlassSurface(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp), role = MeiGlassRole.Navigation,
                ) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        listOf(Icons.Rounded.Recommend to "推荐", Icons.Rounded.MusicVideo to "发现",
                            Icons.Rounded.LibraryMusic to "音乐库").forEach { (icon, label) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
                                Text(label, color = ink, style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (appearance.strongContent) FontWeight.SemiBold else FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
