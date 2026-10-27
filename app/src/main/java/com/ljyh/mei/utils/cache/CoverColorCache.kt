package com.ljyh.mei.utils.cache

import android.content.Context
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.size.Precision
import com.ljyh.mei.utils.image.ArtworkSize


fun preloadImage(context: Context, url: String): Disposable? {
    if (url.isEmpty()) return null

    val request = ImageRequest.Builder(context)
        .data(url)
        .size(ArtworkSize.Small.pixels)
        .precision(Precision.EXACT)
        .build()

    // 使用 enqueue 异步加载，不阻塞当前协程
    return context.imageLoader.enqueue(request)
}
