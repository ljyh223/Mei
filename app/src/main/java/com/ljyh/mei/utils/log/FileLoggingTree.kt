package com.ljyh.mei.utils.log

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(private val context: Context) : Timber.Tree() {

    private val logDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    // 使用 IO 线程写入文件，避免卡顿 UI
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // 过滤掉 Verbose 级别的日志，避免文件过大（可根据需要调整）
        if (priority == Log.VERBOSE) return

        scope.launch {
            saveLogToFile(priority, tag, message, t)
        }
    }

    private fun saveLogToFile(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val logDir = File(context.getExternalFilesDir(null), "app_logs")
            if (!logDir.exists()) logDir.mkdirs()

            // 每天生成一个日志文件，方便管理
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val file = File(logDir, "log-$date.txt")

            // 格式化日志内容：时间 级别/TAG: 消息
            val time = logDateFormat.format(Date())
            val level = when (priority) {
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                else -> "V"
            }

            val logEntry = StringBuilder()
            logEntry.append("$time $level/$tag: $message\n")

            // 如果有异常堆栈，也写入
            if (t != null) {
                logEntry.append(Log.getStackTraceString(t))
            }

            // 简单的文件追加写入
            // 注意：如果并发非常高，这里可能需要加锁或使用 Channel，但一般 APP 这种写法足够了
            FileWriter(file, true).use { writer ->
                writer.append(logEntry.toString())
            }

        } catch (e: Exception) {
            // 写入日志失败，万万不可崩溃，只能在控制台打印一下
            Log.e("FileLoggingTree", "Error writing log to file", e)
        }
    }
}