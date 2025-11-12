package com.ljyh.mei.ui.component.player.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import com.ljyh.mei.utils.TimeUtils.makeTimeString
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProgressSlider(
    position: Long,
    duration: Long,
    trackId: String,
    onPositionChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDurationValid = remember(duration) { duration > 0 }

    val valueRange = remember(duration) { 0f..(duration.takeIf { it > 0 } ?: 1).toFloat() }

    var isUserDragging by remember(trackId) { mutableStateOf(false) }
    var userDraggedPosition by remember(trackId) { mutableStateOf<Float?>(null) }

    val sliderPosition = if (isUserDragging) {
        userDraggedPosition ?: position.toFloat()
    } else {
        position.toFloat()
    }.coerceIn(valueRange)

    val progressFraction = if (isDurationValid)
        (sliderPosition / duration.toFloat()).coerceIn(0f, 1f)
    else 0f

    // 👇 动画控制：进度条放大
    val animatedTrackHeight by animateDpAsState(
        targetValue = if (isUserDragging) 4.dp else 2.dp,
        label = "TrackHeightAnim"
    )

    Column(modifier = modifier) {
        Slider(
            value = sliderPosition,
            onValueChange = {
                if (isDurationValid) {
                    isUserDragging = true
                    userDraggedPosition = it
                }
            },
            onValueChangeFinished = {
                if (isDurationValid) {
                    userDraggedPosition?.let { onPositionChange(it.roundToLong()) }
                    isUserDragging = false
                    userDraggedPosition = null
                }
            },
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            enabled = isDurationValid,
            thumb = {},
            track = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        //动态高度
                        .height(animatedTrackHeight)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    if (isDurationValid) {
                        Box(
                            Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        )

        Spacer(Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = makeTimeString(sliderPosition.toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(
                        if (isUserDragging)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        else
                            Color.Transparent
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Text(
                text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(
                        if (isUserDragging)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        else
                            Color.Transparent
                    )

                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}


