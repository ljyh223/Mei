package com.ljyh.mei.utils


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import androidx.core.graphics.createBitmap

fun String.smallImage(): String {
    if (this.startsWith("/")) return this
    return "$this?param=100y100"
}

fun String.middleImage(): String {
    if (this.startsWith("/")) return this
    return "$this?param=300y300"
}

fun String.largeImage(): String {
    if (this.startsWith("/")) return this
    return "$this?param=500y500"
}

fun String.size1600():String{
    return "$this?param=1600y1600"
}

class CoilImageLoader {
    companion object {
        suspend fun loadImageDrawable(context: Context, imageUrl: String): ImageBitmap? =
            withContext(Dispatchers.IO) {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // 避免返回硬件加速的 Bitmap
                    .build()
                loader.execute(request).image?.toBitmap()?.asImageBitmap()
            }
    }
}

object ImageUtils {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun downloadImageBytes(imageUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(imageUrl)
            .build()

        try {
            val result = httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body.bytes()
                    if (bytes[1].toInt() == 80) {
                        pngToJpg(bytes)
                    } else {
                        bytes
                    }
                } else {
                    null
                }
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun pngToJpg(pngBytes: ByteArray): ByteArray? {
        return try {
            // 将PNG字节数组解码为Bitmap
            val bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
            if (bitmap != null) {
                // 创建一个空白的JPEG格式的Bitmap
                val jpegBitmap =
                    createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)

                // 将PNG内容绘制到JPEG Bitmap上
                val canvas = Canvas(jpegBitmap)
                canvas.drawColor(android.graphics.Color.WHITE) // 设置背景为白色
                val zero = 0
                canvas.drawBitmap(bitmap, zero.toFloat(), zero.toFloat(), Paint())

                // 将JPEG Bitmap压缩为字节数组
                val outputStream = ByteArrayOutputStream()
                jpegBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.toByteArray()
            } else {
                // 解码失败
                ByteArray(0)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            // 处理异常
            ByteArray(0)
        }
    }
}


