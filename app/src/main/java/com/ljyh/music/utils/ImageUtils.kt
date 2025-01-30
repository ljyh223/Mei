package com.ljyh.music.utils


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.ljyh.music.data.model.room.Color
import com.ljyh.music.di.ColorDao
import com.ljyh.music.di.ColorRepository
import com.materialkolor.ktx.themeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import javax.inject.Inject

fun String.smallImage(): String {

    return "$this?param=100y100"
}

fun String.middleImage(): String {
    return "$this?param=300y300"
}

fun String.largeImage(): String {

    return "$this?param=500y500"
}

class ImageColorRepository @Inject constructor(private val colorDao: ColorDao) {

    fun getCachedColor(url: String): Color? {
        return colorDao.getColor(url)
    }

    suspend fun cacheColor(imageColor: Color) {
        colorDao.insertColor(imageColor)
    }
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
    suspend fun downloadImageBytes(imageUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(imageUrl)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes() ?: return@withContext null
                if(bytes[1].toInt()==80){
                    pngToJpg(bytes)
                }else{
                    bytes
                }

            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getImageDominantColor(
        url: String,
        context: Context,
        repository: ColorRepository
    ): androidx.compose.ui.graphics.Color {
        val DEFAULT_COLOR = androidx.compose.ui.graphics.Color(0xFF000000)
        Log.d("getImageDominantColor", "url: $url")
        return try {
            withContext(Dispatchers.IO) {
                val cachedColor = repository.getColor(url)
                if (cachedColor != null  && cachedColor.color>-16000000) {
                    Log.d("getImageDominantColor", "url : $url,color: ${cachedColor.color}")
                    return@withContext androidx.compose.ui.graphics.Color(cachedColor.color)
                } else {
                    return@withContext CoilImageLoader.loadImageDrawable(context, url)?.themeColor(DEFAULT_COLOR)?:DEFAULT_COLOR
                }
            }
        } catch (e: Exception) {
            Log.e("getImageDominantColor", "Error processing color for URL: $url", e)
            DEFAULT_COLOR
        }
    }

    fun pngToJpg(pngBytes: ByteArray): ByteArray? {
        return try {
            // 将PNG字节数组解码为Bitmap
            val bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
            if (bitmap != null) {
                // 创建一个空白的JPEG格式的Bitmap
                val jpegBitmap =
                    Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)

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


