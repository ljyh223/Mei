package com.ljyh.music.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random
// 毫秒转分秒
fun formatDuration(milliseconds: Int): String {
    val minutes = (milliseconds / 1000) / 60
    val seconds = (milliseconds / 1000) % 60
    return String.format("%d:%02d", minutes, seconds)
}

fun specialReplace(s: String): String {
    val replacements = listOf(
        "<" to "＜",
        ">" to "＞",
        "\\" to "＼",
        "/" to "／",
        ":" to "：",
        "?" to "",
        "*" to "＊",
        "\"" to "＂",
        "|" to "｜",
        "..." to " "
    )

    var mutableString = s
    for ((original, replacement) in replacements) {
        mutableString = mutableString.replace(original, replacement)
    }
    return mutableString
}

fun checkAndRequestFilesPermissions(activity: Activity): Boolean {
    // 检查 Android 版本是否在 Android 11 或更高版本
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // 检查 MANAGE_EXTERNAL_STORAGE 权限
        val hasManageExternalStoragePermission = Environment.isExternalStorageManager()
        if (!hasManageExternalStoragePermission) {
            // 如果未授权，跳转到权限设置页面
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
            return false
        }
    } else {
        // 对于 Android 10（API 29）及以下版本，检查读写权限
        val writePermission = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readPermission = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val listPermissionsNeeded = mutableListOf<String>()
        if (writePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (readPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity, listPermissionsNeeded.toTypedArray(), 101 // 自定义的请求码
            )
            return false
        }
    }
    return true
}

fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}


fun <T> rearrangeArray(array: List<T>, selectedIndex: Int): List<T> {
    // 检查索引是否有效
    require(selectedIndex in array.indices) { "Selected index is out of bounds" }

    // 提取选中的元素
    val selectedElement = array[selectedIndex]

    // 创建剩余元素的列表并打乱
    val remainingElements = array.filterIndexed { index, _ -> index != selectedIndex }.toMutableList()
    remainingElements.shuffle(Random) // 使用默认随机源进行打乱

    // 将选中的元素插入到列表的开头
    return listOf(selectedElement) + remainingElements
}