package com.ljyh.mei.ui.component.player.component.mesh

import android.graphics.Bitmap
import android.graphics.Color

object AlbumTextureProcessor {

    fun process(bitmap: Bitmap): Bitmap {
        var img = Bitmap.createScaledBitmap(bitmap, 32, 32, true)

        img = applyContrast(img, 0.4f)
        img = applySaturation(img, 3.0f)
        img = applyContrast(img, 1.7f)
        img = applyBrightness(img, 0.75f)

        for (i in 0 until 4) {
            img = stackBlur(img, 2)
        }

        return img
    }

    private fun applyContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            val a = Color.alpha(pixels[i])

            val nr = clamp(((r / 255f - 0.5f) * contrast + 0.5f) * 255f)
            val ng = clamp(((g / 255f - 0.5f) * contrast + 0.5f) * 255f)
            val nb = clamp(((b / 255f - 0.5f) * contrast + 0.5f) * 255f)

            pixels[i] = Color.argb(a, nr.toInt(), ng.toInt(), nb.toInt())
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        if (bitmap !== result) bitmap.recycle()
        return result
    }

    private fun applySaturation(bitmap: Bitmap, saturation: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val hsv = FloatArray(3)
        for (i in pixels.indices) {
            Color.colorToHSV(pixels[i], hsv)
            hsv[1] = (hsv[1] * saturation).coerceIn(0f, 1f)
            if (hsv[1] < 0.15f) hsv[1] = 0.2f
            pixels[i] = Color.HSVToColor(Color.alpha(pixels[i]), hsv)
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        if (bitmap !== result) bitmap.recycle()
        return result
    }

    private fun applyBrightness(bitmap: Bitmap, brightness: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val r = clamp(Color.red(pixels[i]) * brightness)
            val g = clamp(Color.green(pixels[i]) * brightness)
            val b = clamp(Color.blue(pixels[i]) * brightness)
            val a = Color.alpha(pixels[i])
            pixels[i] = Color.argb(a, r.toInt(), g.toInt(), b.toInt())
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        if (bitmap !== result) bitmap.recycle()
        return result
    }

    private fun stackBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        val dst = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)

        val r = radius.coerceAtLeast(1)
        val divisor = r * 2 + 1

        for (y in 0 until h) {
            for (x in 0 until w) {
                var rr = 0; var gg = 0; var bb = 0; var aa = 0; var count = 0
                for (kx in -r..r) {
                    val sx = (x + kx).coerceIn(0, w - 1)
                    val pixel = src[y * w + sx]
                    rr += Color.red(pixel); gg += Color.green(pixel)
                    bb += Color.blue(pixel); aa += Color.alpha(pixel); count++
                }
                dst[y * w + x] = Color.argb(aa / count, rr / count, gg / count, bb / count)
            }
        }

        val tmp = src.copyOf()
        for (x in 0 until w) {
            for (y in 0 until h) {
                var rr = 0; var gg = 0; var bb = 0; var aa = 0; var count = 0
                for (ky in -r..r) {
                    val sy = (y + ky).coerceIn(0, h - 1)
                    val pixel = dst[sy * w + x]
                    rr += Color.red(pixel); gg += Color.green(pixel)
                    bb += Color.blue(pixel); aa += Color.alpha(pixel); count++
                }
                tmp[y * w + x] = Color.argb(aa / count, rr / count, gg / count, bb / count)
            }
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(tmp, 0, w, 0, 0, w, h)
        if (bitmap !== result) bitmap.recycle()
        return result
    }

    private fun clamp(v: Float): Float = v.coerceIn(0f, 255f)
}