package com.ljyh.mei.utils.image

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


/**
 * 保存图片到相册的工具函数 (适配 Android 10+ Scoped Storage)
 */
suspend fun saveImageToGallery(context: Context, imageUrl: String) {
    try {
        Toast.makeText(context, "正在保存...", Toast.LENGTH_SHORT).show()

        // 1. 使用 Coil Loader 获取 Bitmap
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false) // 必须禁用硬件加速才能获取 Bitmap
            .build()

        val result = context.imageLoader.execute(request)

        if (result is SuccessResult) {
            val bitmap = result.image.toBitmap()

            // 2. 准备 MediaStore 内容
            val filename = "IMG_${System.currentTimeMillis()}.jpg"
            var fos: OutputStream?

            // 适配 Android Q (10) 及以上
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MeiMusic") // 自定义文件夹名
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val contentResolver = context.contentResolver
                val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                imageUri?.let { uri ->
                    fos = contentResolver.openOutputStream(uri)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos!!)
                    fos.close()

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
            } else {
                // 旧版本 Android 处理 (通常需要 WRITE_EXTERNAL_STORAGE 权限，这里简化处理，假设已有权限或使用外部缓存)
                // 建议实际项目中针对 Android 9 及以下添加权限申请逻辑
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val imageFile = File(imagesDir, filename)
                fos = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.close()

                // 通知相册刷新
                // MediaScannerConnection.scanFile(...)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "保存失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}