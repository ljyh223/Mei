package com.ljyh.mei.ui.component.utils

import android.content.Context
import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGaussianBlurFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GPUImageBlurTransformation(val context: Context, private val blurRadius: Float) : Transformation() {

    override val cacheKey: String = "gpuImageBlur-$blurRadius"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return withContext(Dispatchers.Default) {
            val gpuImage = GPUImage(context)
            gpuImage.setImage(input)
            gpuImage.setFilter(GPUImageGaussianBlurFilter(blurRadius))
            gpuImage.setFilter(GPUImageGaussianBlurFilter(blurRadius))
            gpuImage.bitmapWithFilterApplied
        }
    }
}