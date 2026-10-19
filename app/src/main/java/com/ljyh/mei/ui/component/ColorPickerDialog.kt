package com.ljyh.mei.ui.component

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils

data class ColorPreset(val name: String, val color: Color)

/** Shared preset and HSV picker. The caller owns persistence and the theme update. */
@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    presets: List<ColorPreset>,
    onDismiss: () -> Unit,
    onApply: (Color) -> Unit,
) {
    var selectedColor by remember(initialColor) { mutableStateOf(initialColor) }
    val hsv = remember(selectedColor) {
        FloatArray(3).also { AndroidColor.colorToHSV(selectedColor.toArgb(), it) }
    }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val selectedArgb = selectedColor.toArgb()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 460.dp)
                .heightIn(max = screenHeight * 0.82f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(20.dp))
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 4,
                    ) {
                        presets.forEach { preset ->
                            val checked = preset.color.toArgb() == selectedArgb
                            Box(
                                modifier = Modifier.size(56.dp)
                                    .selectable(checked, role = Role.RadioButton) {
                                        selectedColor = preset.color
                                    }
                                    .semantics { contentDescription = preset.name }
                                    .padding(4.dp)
                                    .border(
                                        if (checked) 2.dp else 1.dp,
                                        if (checked) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape,
                                    )
                                    .padding(3.dp)
                                    .background(preset.color, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (checked) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = if (ColorUtils.calculateLuminance(selectedArgb) > 0.5) {
                                            Color.Black
                                        } else Color.White,
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).background(selectedColor, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "#%06X".format(selectedArgb and 0xFFFFFF),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    HsvSlider("色相", hsv[0], 0f..360f, listOf(
                        Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                        Color.Blue, Color.Magenta, Color.Red,
                    )) { hue ->
                        selectedColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, hsv[1], hsv[2])))
                    }
                    HsvSlider("饱和度", hsv[1], 0f..1f, listOf(
                        Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], 0f, hsv[2]))),
                        Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], 1f, hsv[2]))),
                    )) { saturation ->
                        selectedColor = Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], saturation, hsv[2])))
                    }
                    HsvSlider("明度", hsv[2], 0f..1f, listOf(
                        Color.Black,
                        Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], hsv[1], 1f))),
                    )) { value ->
                        selectedColor = Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], hsv[1], value)))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onApply(selectedColor) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("应用颜色") }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("取消") }
            }
        }
    }
}

@Composable
fun ColorPreference(
    title: String,
    color: Color,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    PreferenceEntry(
        title = { Text(title) },
        description = description,
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
private fun HsvSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    gradient: List<Color>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxWidth().height(12.dp)
                    .background(Brush.horizontalGradient(gradient), CircleShape),
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.onSurface,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
        }
    }
}
