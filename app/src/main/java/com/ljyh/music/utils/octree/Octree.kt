package com.ljyh.music.utils.octree

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap

fun getImageDominantColors(imageBitmap: ImageBitmap, colorCount: Int = 4): List<Color> {
    val quantizer = OctreeQuantizer()
    val bitmap = imageBitmap.asAndroidBitmap()
    val pixelMap = imageBitmap.toPixelMap()

    for (x in 0 until bitmap.width) {
        for (y in 0 until bitmap.height) {
            val color = pixelMap[x, y]
            quantizer.addColor(color)
        }
    }
    return quantizer.getDominantColors(colorCount)
}
