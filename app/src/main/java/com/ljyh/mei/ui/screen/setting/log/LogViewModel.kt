package com.ljyh.mei.ui.screen.setting.log

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

@HiltViewModel
class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val _logFiles = MutableStateFlow<List<File>>(emptyList())
    val logFiles = _logFiles.asStateFlow()

    private val _currentFileContent = MutableStateFlow<String?>(null)
    val currentFileContent = _currentFileContent.asStateFlow()

    init {
        loadLogFiles()
    }

    // 加载所有日志文件（包括 Crash 和 普通 Log）
    fun loadLogFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val logDir = File(context.getExternalFilesDir(null), "app_logs")
            val crashDir = File(context.getExternalFilesDir(null), "crash_logs")

            val files = mutableListOf<File>()
            if (logDir.exists()) files.addAll(logDir.listFiles() ?: emptyArray())
            if (crashDir.exists()) files.addAll(crashDir.listFiles() ?: emptyArray())

            // 按最后修改时间降序排列
            files.sortByDescending { it.lastModified() }

            _logFiles.value = files
        }
    }

    // 读取特定文件内容
    fun readContent(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _currentFileContent.value = file.readText()
            } catch (e: Exception) {
                _currentFileContent.value = "读取文件失败: ${e.message}"
            }
        }
    }

    // 关闭查看
    fun closeContent() {
        _currentFileContent.value = null
    }

    // 分享文件
    fun shareFile(file: File) {
        val context = getApplication<Application>()
        try {
            // 获取 FileProvider URI
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "分享日志给开发者")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 删除所有日志
    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            _logFiles.value.forEach { it.delete() }
            loadLogFiles() // 刷新列表
        }
    }
}