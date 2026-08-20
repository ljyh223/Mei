package com.ljyh.mei.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

fun Modifier.liquidGlass(
    backdrop: Backdrop?,
    shape: Shape,
    surfaceColor: Color,
): Modifier = if (backdrop == null) {
    this
} else {
    drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(8.dp.toPx())
            lens(
                refractionHeight = 18.dp.toPx(),
                refractionAmount = 22.dp.toPx(),
            )
        },
        onDrawSurface = { drawRect(surfaceColor) },
    )
}
