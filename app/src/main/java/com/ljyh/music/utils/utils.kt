package com.ljyh.music.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
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


fun checkFilesPermissions(activity: Activity): Boolean{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        val writePermission = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readPermission = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.READ_EXTERNAL_STORAGE
        )
        writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED
    }
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

fun makeTimeString(duration: Long?): String {
    if (duration == null || duration < 0) return ""
    var sec = duration / 1000
    val day = sec / 86400
    sec %= 86400
    val hour = sec / 3600
    sec %= 3600
    val minute = sec / 60
    sec %= 60
    return when {
        day > 0 -> "%d:%02d:%02d:%02d".format(day, hour, minute, sec)
        hour > 0 -> "%d:%02d:%02d".format(hour, minute, sec)
        else -> "%d:%02d".format(minute, sec)
    }
}


val Int.textDp: TextUnit
    @Composable get() = this.textDp(density = LocalDensity.current)

private fun Int.textDp(density: Density): TextUnit = with(density) {
    this@textDp.dp.toSp()
}

fun dp2px(dp:Float) :Float=
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().displayMetrics)
