package com.ljyh.mei.utils.log

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.iterator
import kotlin.system.exitProcess

object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val TAG = "CrashHandler"
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private lateinit var mContext: Context

    // 日志文件文件夹名称
    private const val LOG_DIR_NAME = "crash_logs"

    fun init(context: Context) {
        mContext = context.applicationContext
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * 当 UncaughtException 发生时会转入该函数来处理
     */
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (!handleException(ex) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler?.uncaughtException(thread, ex)
        } else {
            // 等待一会，让Toast显示出来（如果做了UI提示的话），或者保证文件写入完成
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                Log.e(TAG, "error : ", e)
            }
            // 退出程序
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }

    /**
     * 自定义错误处理，收集错误信息，发送错误报告等操作均在此完成.
     * @return true: 如果处理了该异常信息; otherwise false.
     */
    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) return false

        // 1. (可选) 使用 Toast 来显示异常信息，但在 Compose 或非 UI 线程中直接弹 Toast 可能崩溃
        // 这里建议只做日志保存，重启后提示用户

        // 2. 收集设备参数信息
        val infos = collectDeviceInfo(mContext)

        // 3. 保存日志文件
        saveCrashInfo2File(ex, infos)

        return true
    }

    /**
     * 收集设备参数信息
     */
    private fun collectDeviceInfo(context: Context): Map<String, String> {
        val infos = HashMap<String, String>()
        try {
            val pm = context.packageManager
            val pi = pm.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            if (pi != null) {
                val versionName = pi.versionName ?: "null"
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pi.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    pi.versionCode.toString()
                }
                infos["versionName"] = versionName
                infos["versionCode"] = versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "an error occured when collect package info", e)
        }

        infos["MODEL"] = Build.MODEL
        infos["DEVICE"] = Build.DEVICE
        infos["MANUFACTURER"] = Build.MANUFACTURER
        infos["ANDROID_VERSION"] = Build.VERSION.RELEASE
        infos["SDK_INT"] = Build.VERSION.SDK_INT.toString()

        return infos
    }

    /**
     * 保存错误信息到文件中
     */
    private fun saveCrashInfo2File(ex: Throwable, infos: Map<String, String>): String? {
        val sb = StringBuffer()
        // 拼接设备信息
        for ((key, value) in infos) {
            sb.append("$key=$value\n")
        }

        // 拼接堆栈信息
        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        ex.printStackTrace(printWriter)
        var cause = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()
        val result = writer.toString()
        sb.append(result)

        try {
            val timestamp = System.currentTimeMillis()
            val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
            val time = formatter.format(Date())
            val fileName = "crash-$time-$timestamp.log"

            // 存储路径：/Android/data/<package-name>/files/crash_logs/
            // 这种路径不需要申请运行时存储权限，且卸载应用后会自动删除，符合规范
            val logDir = File(mContext.getExternalFilesDir(null), LOG_DIR_NAME)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val file = File(logDir, fileName)
            val fos = FileOutputStream(file)
            fos.write(sb.toString().toByteArray())
            fos.close()

            Log.e(TAG, "Crash log saved to: ${file.absolutePath}")
            return fileName
        } catch (e: Exception) {
            Log.e(TAG, "an error occured while writing file...", e)
        }
        return null
    }
}