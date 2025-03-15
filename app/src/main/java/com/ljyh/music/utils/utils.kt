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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
// 毫秒转分秒
fun formatDuration(milliseconds: Int): String {
    val minutes = (milliseconds / 1000) / 60
    val seconds = (milliseconds / 1000) % 60
    return String.format("%d:%02d", minutes, seconds)
}

// 毫秒转秒
fun formatMilliseconds(milliseconds: Long): Int {
    val seconds = milliseconds / 1000
    return seconds.toInt()
}
// 秒转时分秒, 如果
fun formatSeconds(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60

    if(hours>1) return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

// 获取时间戳
fun getCurrentTimestamp(): String {
    return System.currentTimeMillis().toString()
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
fun extractContent(input: String): String {
    val startTag = "<!--"
    val endTag = "-->"

    val startIndex = input.indexOf(startTag) // 找到起始标记的位置
    val endIndex = input.indexOf(endTag, startIndex + startTag.length) // 找到结束标记的位置

    if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
        // 提取起始标记后和结束标记前的内容
        return input.substring(startIndex + startTag.length, endIndex).trim()
    }
    return "" // 如果未找到标记，返回 null
}

fun checkFilesPermissions(activity: Activity): Boolean {
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


// 扩展方法：Dp 转 Px
fun Dp.toPx(context: android.content.Context): Float {
    return this.value * context.resources.displayMetrics.density
}



fun getFormattedDate(): String {
    val now = Date()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return dateFormat.format(now)
}


fun jsonToFormUrlEncoded(jsonString: String): String {
    // 使用 Gson 解析 JSON 为 Map
    val gson = Gson()
    val map= gson.fromJson(jsonString, Map::class.java)

    // 构建 URL 编码字符串
    return map.entries.joinToString("&") { (key, value) ->
        val encodedKey = URLEncoder.encode(key.toString(), "UTF-8")
        val encodedValue = URLEncoder.encode(value.toString(), "UTF-8")
        "$encodedKey=$encodedValue"
    }
}

fun getRandomString(len:Int=16):String{
    val sb = StringBuilder(len)
    for (i in 0 until len) {
        sb.append(('a'..'z').random())
    }
    return sb.toString()
}

fun getWNMCID(): String {
    val characters = "abcdefghijklmnopqrstuvwxyz"
    var randomString = ""
    for (i in 0 until 6) {
        randomString += characters.random()
    }
    return "$randomString.${System.currentTimeMillis()}.01.0"
}

fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")