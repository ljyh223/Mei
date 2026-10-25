package com.ljyh.mei.ui.screen.setting

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import com.ljyh.mei.ui.component.glass.meiGlassAccentColor
import com.ljyh.mei.ui.component.glass.meiGlassContentColor
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.local.LocalNavController
import androidx.compose.foundation.layout.asPaddingValues
import com.ljyh.mei.ui.component.glass.MeiPinnedLazyPage
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
import com.ljyh.mei.ui.component.layout.meiPageGutter

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
        onNavigateBack = navController::navigateUp,
    ) {
        item(key = "page-content") {
            Column {
                CompositionLocalProvider(LocalMeiGlassAppearance provides draft) {
                    GlassMaterialPreview()
                }
                if (!live) Text(
                    text = when {
                        !enabled -> "液态玻璃已关闭，当前显示静态样式"
                        Build.VERSION.SDK_INT < 33 -> "此设备显示静态样式，实时玻璃需要 Android 13 或更新版本"
                        else -> "当前省电或设备内存策略已简化玻璃效果"
                    },
                    modifier = Modifier.padding(horizontal = meiPageGutter(), vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PreferenceSection("外观") {
                    SwitchPreference(
                        title = { Text("液态玻璃") }, checked = enabled, onCheckedChange = onEnabledChange,
                    )
                    ListPreference(
                        title = { Text("材质预设") },
                        selectedValue = draft,
                        values = listOf(GlassAppearance.Clear, GlassAppearance.Balanced, GlassAppearance.Readable)
                            .map { draft.withMaterial(it) },
                        valueText = {
                            when (draft.withMaterial(it)) {
                                draft.withMaterial(GlassAppearance.Clear) -> "通透"
                                draft.withMaterial(GlassAppearance.Balanced) -> "均衡"
                                draft.withMaterial(GlassAppearance.Readable) -> "柔和"
                                else -> "自定义"
                            }
                        },
                        onValueSelected = { draft = it; save() },
                        isEnabled = !draft.reduceTransparency,
                    )
                    SwitchPreference(
                        title = { Text("降低透明度") },
                        description = "顶栏与悬浮控件使用实色底色，关闭液态玻璃后仍生效",
                        checked = draft.reduceTransparency,
                        onCheckedChange = { draft = draft.copy(reduceTransparency = it); save() },
                    )
                    SwitchPreference(
                        title = { Text("增强清晰度") },
                        description = "加深图标和文字，并加重标签字重",
                        checked = draft.strongContent,
                        onCheckedChange = { draft = draft.copy(strongContent = it); save() },
                    )
                }
                PreferenceSection("材质细节") {
                    GlassAdjustment("底色浓度", "越高越能遮住背景，文字更容易辨认", draft.opacity, 0f..1f,
                        { draft = draft.copy(opacity = it) }, save, enabled = !draft.reduceTransparency)
                    GlassAdjustment("背景模糊", "越高越能柔化背后的图片和文字", draft.blur, 0f..40f,
                        { draft = draft.copy(blur = it) }, save, percent = false, enabled = !draft.reduceTransparency)
                    GlassAdjustment("折射强度", "调整玻璃内背景的弯曲程度", draft.refraction, 0f..1f,
                        { draft = draft.copy(refraction = it) }, save, enabled = !draft.reduceTransparency)
                    GlassAdjustment("高光强度", "调整玻璃表面的光泽", draft.highlight, 0f..1f,
                        { draft = draft.copy(highlight = it) }, save, enabled = !draft.reduceTransparency)
                    GlassAdjustment("背景色彩", "越低越接近中性，越高越保留背景颜色", draft.saturation, 0f..1f,
                        { draft = draft.copy(saturation = it) }, save, enabled = !draft.reduceTransparency)
                    PreferenceEntry(
                        title = { Text("恢复默认") }, description = "恢复通透的均衡材质与默认清晰度",
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
    enabled: Boolean = true,
) {
    PreferenceEntry(title = { Text(title) }, description = description, isEnabled = enabled, content = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Slider(
                value = value,
                onValueChange = onChange,
                onValueChangeFinished = onFinished,
                valueRange = range,
                enabled = enabled,
                modifier = Modifier.weight(1f).semantics { contentDescription = title },
            )
            Text(
                if (percent) "${(value * 100).roundToInt()}%" else "%.2f dp".format(java.util.Locale.ROOT, value),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    })
}

@Composable
private fun GlassMaterialPreview() {
    val source = rememberHazeState()
    val appearance = LocalMeiGlassAppearance.current
    val live = appearance.usesOptics(LocalMeiGlassEnabled.current)
    val ink = meiGlassContentColor()
    val secondaryInk = meiGlassContentColor(secondary = true)
    val accent = meiGlassAccentColor()
    Column(Modifier.padding(horizontal = meiPageGutter(), vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("材质预览", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth().height(188.dp).clip(MaterialTheme.shapes.large)) {
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
                Row(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MeiGlassSurface(Modifier.size(40.dp), shape = RoundedCornerShape(50)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null,
                            tint = ink, modifier = Modifier.align(Alignment.Center).size(22.dp))
                    }
                    MeiGlassSurface(Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(50)) {
                        Row(Modifier.align(Alignment.CenterStart).padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = ink,
                                modifier = Modifier.size(22.dp))
                            Text("搜索音乐", color = secondaryInk, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                MeiGlassSurface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        listOf(Icons.Rounded.Recommend to "推荐", Icons.Rounded.MusicVideo to "发现",
                            Icons.Rounded.LibraryMusic to "音乐库").forEachIndexed { index, (icon, label) ->
                            val labelInk = if (index == 0 || appearance.strongContent) ink else secondaryInk
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(icon, contentDescription = null, tint = if (index == 0) accent else labelInk,
                                    modifier = Modifier.size(24.dp))
                                Text(label, color = labelInk, style = MaterialTheme.typography.labelMedium,
                                    fontWeight = when {
                                        index == 0 -> FontWeight.Bold
                                        appearance.strongContent -> FontWeight.SemiBold
                                        else -> FontWeight.Medium
                                    })
                            }
                        }
                    }
                }
            }
        }
    }
}
