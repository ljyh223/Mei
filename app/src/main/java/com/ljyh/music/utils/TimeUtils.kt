package com.ljyh.music.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
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

    fun getFormattedDate(): String {
        val now = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(now)
    }
}