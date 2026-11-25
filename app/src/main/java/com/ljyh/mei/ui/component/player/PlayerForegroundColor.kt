package com.ljyh.mei.ui.component.player

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Keeps player content readable over both cover extensions and motion artwork. */
val LocalPlayerForegroundColor = compositionLocalOf { Color.White }

/** Shared contrast decision for sampled cover backgrounds on both player presentations. */
internal fun playerForegroundForBackground(background: Color?): Color =
    if (background?.luminance()?.let { it > .179f } == true) Color.Black else Color.White


/** Additive blending cannot darken a light backdrop, even when the glyph color is black. */
internal fun playerLyricBlendMode(foreground: Color): BlendMode =
    if (foreground.luminance() < .5f) BlendMode.SrcOver else BlendMode.Plus
