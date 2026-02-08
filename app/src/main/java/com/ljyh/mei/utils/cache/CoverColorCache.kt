package com.ljyh.mei.utils.cache

import android.content.Context
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Precision


fun preloadImage(context: Context, url: String) {
    if (url.isEmpty()) return

    val request = ImageRequest.Builder(context)
        .data(url)
        .precision(Precision.EXACT)
        .build()

    // 使用 enqueue 异步加载，不阻塞当前协程
    context.imageLoader.enqueue(request)
}
