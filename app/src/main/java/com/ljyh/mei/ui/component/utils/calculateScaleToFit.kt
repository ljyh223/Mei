package com.ljyh.mei.ui.component.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun calculateScaleToFit(): Float {
    val boxSize = with(LocalDensity.current) { 1.dp.toPx() }
    val imageSize = 1f // This should be replaced with actual image size calculation
    return boxSize / imageSize
}