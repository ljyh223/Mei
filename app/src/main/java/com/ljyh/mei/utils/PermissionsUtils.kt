package com.ljyh.mei.utils

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

object PermissionsUtils {
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
}