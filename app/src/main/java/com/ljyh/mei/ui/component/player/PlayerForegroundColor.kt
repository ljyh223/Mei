package com.ljyh.mei.ui.component.player

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Keeps the controls readable when motion artwork supplies its own background palette. */
val LocalPlayerForegroundColor = compositionLocalOf { Color.White }

/** Shared contrast decision for sampled cover backgrounds on both player presentations. */
internal fun playerForegroundForBackground(background: Color?): Color =
    if (background?.luminance()?.let { it > .179f } == true) Color.Black else Color.White
