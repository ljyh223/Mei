package com.ljyh.music.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture


class CoilBitmapLoader(private val context: Context) : BitmapLoader {

    private val imageLoader = ImageLoader.Builder(context)
        .build()

    // 判断是否支持某个 MIME 类型
    override fun supportsMimeType(mimeType: String): Boolean {
        // 这里假设支持常见的图片类型，你可以根据需要调整
        return mimeType.startsWith("image/")
    }

    // 从字节数组中解码 Bitmap
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()

        // 使用 Coil 来解码字节数据
        val request = ImageRequest.Builder(context)
            .data(data)
            .target { result ->
                future.set(result.toBitmap())
            }
            .build()

        imageLoader.enqueue(request)
        return future
    }

    // 从 URI 加载 Bitmap
    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()

        // 使用 Coil 加载 URI
        val request = ImageRequest.Builder(context)
            .data(uri)
            .target { result ->
                future.set(result.toBitmap())
            }
            .build()

        imageLoader.enqueue(request)
        return future
    }
}
