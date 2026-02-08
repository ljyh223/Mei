package com.ljyh.mei.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture



/**
 * 一个使用 Coil 3 实现的、健壮的 Media3 BitmapLoader.
 *
 * @param context Context 对象.
 * @param imageLoader 建议传入在您应用中全局单例的 ImageLoader 实例，以实现高效的资源共享（缓存、线程池等）。
 */
@UnstableApi
class CoilBitmapLoader(
    private val context: Context,
    private val imageLoader: ImageLoader
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean {
        // Coil 支持所有主流图片格式，这个判断是合理的。
        return mimeType.startsWith("image/")
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return loadData(data)
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        return loadData(uri)
    }

    private fun loadData(data: Any): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()

        val request = ImageRequest.Builder(context)
            .data(data)
            // 禁用硬件位图。Media3 的某些组件（如视频处理的 Overlay）需要软件位图，
            // 设置为 false 可以提高兼容性，避免潜在的运行时异常。
            .allowHardware(false)
            .listener(
                onSuccess = { _, result ->
                    // 使用 androidx.core.graphics.drawable.toBitmap() 是一个安全的方式
                    // 来将任何 Drawable 转换为 Bitmap，即使是 VectorDrawable。
                    val bitmap = result.image.toBitmap()
                    future.set(bitmap)
                },
                onError = { _, result ->
                    // 关键：当加载失败时，将异常设置到 Future 中。
                    future.setException(result.throwable)
                }
            )
            .build()

        val disposable = imageLoader.enqueue(request)

        // 关键：添加一个监听器，当 Future 被外部取消时，我们也取消 Coil 的请求。
        future.addListener({
            if (future.isCancelled) {
                disposable.dispose()
            }
        }, MoreExecutors.directExecutor()) // 使用 Guava 的 directExecutor 高效执行。

        return future
    }
}