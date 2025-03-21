package com.ljyh.music.utils.octree

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.Color
import kotlin.math.pow


class OctreeNode {
    var redSum = 0
    var greenSum = 0
    var blueSum = 0
    var pixelCount = 0
    val children = arrayOfNulls<OctreeNode>(8)
    var isLeaf = true

    fun addColor(color: Color, level: Int) {
        if (isLeaf) {
            redSum += (color.red * 255).toInt()
            greenSum += (color.green * 255).toInt()
            blueSum += (color.blue * 255).toInt()
            pixelCount++
        } else {
            val index = getOctreeIndex(color, level)
            if (children[index] == null) {
                children[index] = OctreeNode()
            }
            children[index]?.addColor(color, level + 1)
        }
    }

    private fun getOctreeIndex(color: Color, level: Int): Int {
        val shift = 7 - level
        val r = ((color.red * 255).toInt() shr shift) and 1
        val g = ((color.green * 255).toInt() shr shift) and 1
        val b = ((color.blue * 255).toInt() shr shift) and 1
        return (r shl 2) or (g shl 1) or b
    }

    fun getAverageColor(): Color {
        return Color(
            red = (redSum / pixelCount) / 255f,
            green = (greenSum / pixelCount) / 255f,
            blue = (blueSum / pixelCount) / 255f
        )
    }
}
