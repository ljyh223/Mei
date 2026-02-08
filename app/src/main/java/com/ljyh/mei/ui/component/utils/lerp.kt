package com.ljyh.mei.ui.component.utils

import androidx.compose.ui.unit.Dp

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

fun lerp(start: Dp, stop: Dp, fraction: Float): Dp {
    return Dp(lerp(start.value, stop.value, fraction))
}
