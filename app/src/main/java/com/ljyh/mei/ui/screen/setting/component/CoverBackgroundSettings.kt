package com.ljyh.mei.ui.screen.setting.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.CoverBackgroundConfidenceKey
import com.ljyh.mei.constants.CoverBackgroundToleranceKey
import com.ljyh.mei.constants.CoverSingleEdgeMatchKey
import com.ljyh.mei.constants.CoverSoftEdgeMatchKey
import com.ljyh.mei.constants.CoverSolidEdgeMatchKey
import com.ljyh.mei.utils.image.CoverBackgroundConfig
import com.ljyh.mei.utils.preferences.rememberPreference
import java.util.Locale

@Composable
internal fun CoverBackgroundSettings() {
    val defaults = remember { CoverBackgroundConfig() }
    val (tolerance, setTolerance) = rememberPreference(
        CoverBackgroundToleranceKey, defaults.colorTolerance,
    )
    val (background, setBackground) = rememberPreference(
        CoverBackgroundConfidenceKey, defaults.minimumBackgroundConfidence,
    )
    val (solid, setSolid) = rememberPreference(CoverSolidEdgeMatchKey, defaults.solidEdgeMatch)
    val (soft, setSoft) = rememberPreference(CoverSoftEdgeMatchKey, defaults.softEdgeMatch)
    val (single, setSingle) = rememberPreference(
        CoverSingleEdgeMatchKey, defaults.minimumSingleEdgeMatch,
    )

    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "识别参数",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    setTolerance(defaults.colorTolerance)
                    setBackground(defaults.minimumBackgroundConfidence)
                    setSolid(defaults.solidEdgeMatch)
                    setSoft(defaults.softEdgeMatch)
                    setSingle(defaults.minimumSingleEdgeMatch)
                }) { Text("重置") }
            }
            Text(
                "底色识别与无缝扩展分别判断；低于扩展阈值时保留常规播放器背景。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ThresholdSlider(
                label = "颜色容差（OKLab）", value = tolerance, range = 0.03f..0.13f,
                display = { String.format(Locale.ROOT, "%.3f", it) }, onChange = setTolerance,
            )
            ThresholdSlider(
                label = "最低底色可信度", value = background, range = 0.45f..0.85f,
                display = ::percent, onChange = setBackground,
            )
            ThresholdSlider(
                label = "直接扩展阈值", value = solid, range = 0.86f..0.99f,
                display = ::percent, onChange = setSolid,
            )
            ThresholdSlider(
                label = "柔和过渡阈值", value = soft, range = 0.50f..0.85f,
                display = ::percent, onChange = setSoft,
            )
            ThresholdSlider(
                label = "单边最低匹配率", value = single, range = 0.30f..0.80f,
                display = ::percent, onChange = setSingle,
            )
        }
    }
}

private fun percent(value: Float) = "${(value * 100).toInt()}%"

@Composable
private fun ThresholdSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    var draft by remember(value) { mutableFloatStateOf(value.coerceIn(range.start, range.endInclusive)) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(display(draft), style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { onChange(draft) },
            valueRange = range,
        )
    }
}
