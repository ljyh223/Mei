package com.ljyh.mei.ui.screen.setting.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.player.cover.rememberCoverEdgeResult
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.utils.image.CoverBackgroundConfig
import com.ljyh.mei.utils.image.CoverBackgroundPreset
import com.ljyh.mei.utils.image.CoverExtensionMode
import com.ljyh.mei.utils.preferences.rememberCoverBackgroundSettings
import com.ljyh.mei.utils.preferences.saveCoverBackgroundSettings
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
internal fun CoverBackgroundSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by rememberCoverBackgroundSettings()
    val config = settings.config
    fun save(next: CoverBackgroundConfig, preset: CoverBackgroundPreset = CoverBackgroundPreset.Custom) {
        scope.launch { context.saveCoverBackgroundSettings(next, preset) }
    }

    PreferenceSection("匹配策略") {
        ListPreference(
            title = { Text("匹配预设") },
            description = settings.preset.description,
            selectedValue = settings.preset,
            values = CoverBackgroundPreset.entries.toList(),
            valueText = { it.label },
            onValueSelected = { save(it.config ?: config, it) },
        )
    }
    CoverMeasurements(config)
    if (settings.preset != CoverBackgroundPreset.Custom) return

    PreferenceSection("底色识别") {
        ThresholdSlider(
            label = "颜色容差", description = "越大越容易把相近颜色视为同一底色；OKLab 距离",
            value = config.colorTolerance, range = 0.025f..0.15f, display = ::decimal,
            onChange = { save(config.copy(colorTolerance = it)) },
        )
        ThresholdSlider(
            label = "最低底色可信度", description = "越高越谨慎，综合考虑边缘占比、整图占比和颜色纯度",
            value = config.minimumBackgroundConfidence, range = 0f..1f, display = ::percent,
            onChange = { save(config.copy(minimumBackgroundConfidence = it)) },
        )
        ThresholdSlider(
            label = "边缘底色最低占比", description = "取样边缘带中需要有多少像素接近底色",
            value = config.minimumBandShare, range = 0.1f..1f, display = ::percent,
            onChange = { save(config.copy(minimumBandShare = it)) },
        )
        ThresholdSlider(
            label = "整图底色最低占比", description = "避免把细边框误当作大片纯色背景",
            value = config.minimumWholeShare, range = 0f..1f, display = ::percent,
            onChange = { save(config.copy(minimumWholeShare = it)) },
        )
    }
    PreferenceSection("平整度") {
        ThresholdSlider(
            label = "最大纹理差异", description = "相邻边缘像素的平均色差；越小越容易排除颗粒和纹理",
            value = config.maximumBandTexture, range = 0f..0.1f, display = ::decimal,
            onChange = { save(config.copy(maximumBandTexture = it)) },
        )
        ThresholdSlider(
            label = "最大渐变差异", description = "各边平均颜色的最大差异；越小越容易排除渐变背景",
            value = config.maximumBandGradient, range = 0f..0.2f, display = ::decimal,
            onChange = { save(config.copy(maximumBandGradient = it)) },
        )
    }
    PreferenceSection("边缘匹配") {
        ThresholdSlider(
            label = "直接扩展最低匹配率", description = "边缘完整度足够高时直接延伸底色",
            value = config.solidEdgeMatch, range = 0f..1f, display = ::percent,
            onChange = { save(config.copy(solidEdgeMatch = it)) },
        )
        ThresholdSlider(
            label = "柔和过渡最低匹配率", description = "不高于直接扩展阈值，达到条件时使用柔和接缝",
            value = config.softEdgeMatch, range = 0f..config.solidEdgeMatch, display = ::percent,
            onChange = { save(config.copy(softEdgeMatch = it)) },
        )
        ThresholdSlider(
            label = "单边最低匹配率", description = "常规柔和过渡中，每一边都需要达到的底色比例",
            value = config.minimumSingleEdgeMatch, range = 0f..config.solidEdgeMatch, display = ::percent,
            onChange = { save(config.copy(minimumSingleEdgeMatch = it)) },
        )
        ThresholdSlider(
            label = "直接扩展单边余量", description = "直接扩展时允许最弱边比总匹配阈值低多少",
            value = config.solidSideSlack, range = 0f..1f, display = ::percent,
            onChange = { save(config.copy(solidSideSlack = it)) },
        )
        ThresholdSlider(
            label = "直接扩展最大缺口", description = "单边连续非底色像素所占的最大比例",
            value = config.maximumSolidGap, range = 0f..config.maximumSoftGap, display = ::percent,
            onChange = { save(config.copy(maximumSolidGap = it)) },
        )
        ThresholdSlider(
            label = "柔和过渡最大缺口", description = "不低于直接扩展上限；越大越允许连续遮挡",
            value = config.maximumSoftGap, range = 0f..1f, display = ::percent,
            onChange = { save(config.copy(maximumSoftGap = it)) },
        )
    }
    PreferenceSection("遮挡例外") {
        SwitchPreference(
            title = { Text("允许单边被主体占据") },
            description = "一个边缘被遮挡时仍可柔和扩展，保留底色和平整度检查；不受常规缺口上限约束",
            checked = config.allowSingleOccupiedSide,
            onCheckedChange = { save(config.copy(allowSingleOccupiedSide = it)) },
        )
        if (config.allowSingleOccupiedSide) ThresholdSlider(
            label = "单边遮挡时的最强边匹配率", description = "至少有一边达到此值，才允许单边遮挡例外",
            value = config.singleEdgeStrongMatch, range = 0f..1f, display = ::percent,
            onChange = { save(config.copy(singleEdgeStrongMatch = it)) },
        )
        SwitchPreference(
            title = { Text("允许三边完整时放宽剩余边") },
            description = "三边达到直接扩展匹配率时，单独判断剩余边；不受常规缺口上限约束",
            checked = config.allowThreeStrongSides,
            onCheckedChange = { save(config.copy(allowThreeStrongSides = it)) },
        )
        if (config.allowThreeStrongSides) ThresholdSlider(
            label = "剩余一边最低匹配率", description = "三边完整例外中，最弱一边需要保留的底色比例",
            value = config.threeSideMinimumMatch, range = 0f..1f, display = ::percent,
            onChange = { save(config.copy(threeSideMinimumMatch = it)) },
        )
    }
}

@Composable
private fun CoverMeasurements(config: CoverBackgroundConfig) {
    val song = LocalPlayerConnection.current?.mediaMetadata?.collectAsState()?.value
    var expanded by rememberSaveable { mutableStateOf(false) }
    val result = if (expanded && !song?.coverUrl.isNullOrBlank()) {
        rememberCoverEdgeResult(song.coverUrl, config)
    } else null
    PreferenceSection("当前封面") {
        PreferenceEntry(
            title = { Text(song?.title ?: "暂无播放歌曲") },
            description = if (song == null) "播放歌曲后可对照封面实测值调整条件" else "查看这张静态封面的实际匹配数值",
            onClick = if (song != null) ({ expanded = !expanded }) else null,
            trailingContent = if (song != null) ({
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "收起分析数值" else "展开分析数值")
            }) else null,
        )
        if (expanded && song != null) {
            val analysis = result?.analysis
            when {
                song.coverUrl.isBlank() -> PreferenceEntry(title = { Text("这首歌曲没有可分析的封面") })
                result == null -> PreferenceEntry(title = { Text("正在分析封面") })
                result.failure != null -> PreferenceEntry(title = { Text(result.failure) })
                analysis != null -> {
                    val mode = when (analysis.mode) {
                        CoverExtensionMode.SOLID -> "直接扩展"
                        CoverExtensionMode.SOFT -> "柔和过渡"
                        CoverExtensionMode.NONE -> "保留常规播放器背景"
                    }
                    PreferenceEntry(
                        title = { Text(mode) },
                        description = "静态封面的判断结果；动态封面仍使用其自身背景",
                    )
                    PreferenceEntry(
                        title = { Text("识别底色") },
                        description = String.format(Locale.ROOT, "#%06X", analysis.color and 0xFFFFFF),
                        trailingContent = { Box(Modifier.size(24.dp).background(Color(analysis.color), CircleShape)) },
                    )
                    Measurement("底色可信度", percent(analysis.backgroundConfidence), "要求 ≥ ${percent(config.minimumBackgroundConfidence)}")
                    Measurement("边缘底色占比", percent(analysis.bandShare), "要求 ≥ ${percent(config.minimumBandShare)}")
                    Measurement("整图底色占比", analysis.wholeShare?.let(::percent) ?: "未计算", "要求 ≥ ${percent(config.minimumWholeShare)}；边缘底色不足时跳过整图分析")
                    Measurement("纹理差异", decimal(analysis.bandTexture), "要求 ≤ ${decimal(config.maximumBandTexture)}")
                    Measurement("渐变差异", decimal(analysis.bandGradient), "要求 ≤ ${decimal(config.maximumBandGradient)}")
                    Measurement("扩展匹配率", percent(analysis.extensionConfidence), "直接 ≥ ${percent(config.solidEdgeMatch)} · 柔和 ≥ ${percent(config.softEdgeMatch)}")
                    Measurement("最大连续缺口", percent(analysis.longestGap), "直接 ≤ ${percent(config.maximumSolidGap)} · 柔和 ≤ ${percent(config.maximumSoftGap)}；例外规则另行判断")
                    listOf("上边", "右边", "下边", "左边").zip(analysis.sideMatches).forEach { (side, value) ->
                        Measurement("${side}匹配率", percent(value), "常规柔和过渡要求 ≥ ${percent(config.minimumSingleEdgeMatch)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun Measurement(label: String, value: String, requirement: String) {
    PreferenceEntry(
        title = { Text(label) }, description = requirement,
        trailingContent = { Text(value, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
    )
}

private fun percent(value: Float) = String.format(Locale.ROOT, "%.1f%%", value * 100)
private fun decimal(value: Float) = String.format(Locale.ROOT, "%.4f", value)

@Composable
private fun ThresholdSlider(
    label: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    var draft by remember(value) { mutableFloatStateOf(value.coerceIn(range.start, range.endInclusive)) }
    var showInput by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { showInput = true }) { Text(display(draft), maxLines = 1) }
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { onChange(draft) },
            valueRange = range,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
    if (showInput) {
        var text by remember(value) { mutableStateOf(value.toString()) }
        val parsed = text.toFloatOrNull()
        AlertDialog(
            onDismissRequest = { showInput = false },
            title = { Text(label) },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, singleLine = true,
                    label = { Text("数值") },
                    supportingText = { Text("${range.start} – ${range.endInclusive}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = parsed != null && parsed.isFinite() && parsed in range,
                    onClick = { parsed?.let(onChange); showInput = false },
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showInput = false }) { Text("取消") } },
        )
    }
}
