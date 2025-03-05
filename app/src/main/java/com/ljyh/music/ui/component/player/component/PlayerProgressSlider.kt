package com.ljyh.music.ui.component.player.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import com.smarttoolfactory.slider.ColorfulSlider
import com.smarttoolfactory.slider.MaterialSliderDefaults
import com.smarttoolfactory.slider.SliderBrushColor

@Composable
fun PlayerProgressSlider(
    position: Long,
    duration: Long,
    onPositionChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(position.toFloat()) }

    ColorfulSlider(
        modifier = modifier,
        value = position.toFloat(),
        thumbRadius = 0.dp,
        trackHeight = 2.dp,
        onValueChange = { value ->
            sliderPosition = value
        },
        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
        onValueChangeFinished = {
            onPositionChange(sliderPosition.toLong())
        },
        colors = MaterialSliderDefaults.materialColors(
            activeTrackColor = SliderBrushColor(
                color = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            thumbColor = SliderBrushColor(
                color = MaterialTheme.colorScheme.tertiaryContainer
            )
        )
    )
}