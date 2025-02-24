package com.ljyh.music.ui.component.utils

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorMatrix

@Composable
fun imageWithDynamicFilter(): ColorFilter {
    val isDarkTheme = isSystemInDarkTheme()


    val cm = ColorMatrix(
        floatArrayOf(
            if (isDarkTheme) 0.5f else 1.5f, 0f, 0f, 0f, 0f, // 红色通道
            0f, if (isDarkTheme) 0.5f else 1.5f, 0f, 0f, 0f, // 绿色通道
            0f, 0f, if (isDarkTheme) 0.5f else 1.5f, 0f, 0f, // 蓝色通道
            0f, 0f, 0f, 1f, 0f // 透明度
        )
    )
    cm.setToSaturation(1.5f)
    val colorMatrix = ColorFilter.colorMatrix(cm)

    return colorMatrix
}