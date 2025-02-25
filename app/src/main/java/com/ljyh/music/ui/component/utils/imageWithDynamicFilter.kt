package com.ljyh.music.ui.component.utils

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorMatrix




fun imageWithDynamicFilter(isDarkTheme: Boolean): ColorFilter {
    val (scale, offset, saturation) = if (isDarkTheme) {
        Triple(1.3f, 80f, 1.5f) // 深色模式：适度提亮，中等偏移，轻微降饱和
    } else {
        Triple(1.5f, 100f, 1.7f)  // 浅色模式：较强提亮，大幅偏移，高饱和
    }

    val cm = ColorMatrix().apply {
        setToScale(scale, scale, scale, 1f) // 统一缩放RGB通道

        set(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, offset,
            0f, 1f, 0f, 0f, offset,
            0f, 0f, 1f, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )))
        setToSaturation(saturation)
    }

    return ColorFilter.colorMatrix(cm)
}