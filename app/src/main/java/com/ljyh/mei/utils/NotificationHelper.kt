package com.ljyh.mei.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ljyh.mei.R
import timber.log.Timber

fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        "download_channel", // 通道 ID
        "Download Notifications", // 通道名称
        NotificationManager.IMPORTANCE_LOW // 通道重要性
    ).apply {
        description = "Notifications for song downloads"
    }

    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}

// 检查并申请通知权限
fun checkAndRequestNotificationPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        Timber.tag("NotificationHelper").d("申请权限")
        // 申请权限
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    }
}
class NotificationHelper(private val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationId = 1
    val TAG = "NotificationHelper"

    fun showProgressNotification(current: Int, total: Int, lose: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "没有权限")
            return
        }

        val progress = (current * 100) / total
        val notification = NotificationCompat.Builder(context, "download_channel")
            .setContentTitle("Downloading songs")
            .setContentText("$current/$lose | $total songs downloaded")
            .setSmallIcon(android.R.drawable.stat_sys_download) // 确保有图标
            .setProgress(100, progress, false)
            .build()
        notificationManager.notify(notificationId, notification)
    }


    fun showCompletionNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "没有权限")
            return // 没有权限时直接返回
        }
        val notification = NotificationCompat.Builder(context, "download_channel")
            .setContentTitle("Download Complete")
            .setContentText("All songs have been downloaded.")
            .setSmallIcon(R.drawable.baseline_download_24)
            .build()
        notificationManager.notify(notificationId, notification)
    }
}
