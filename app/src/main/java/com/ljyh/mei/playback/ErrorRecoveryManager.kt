package com.ljyh.mei.playback

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.ljyh.mei.data.network.api.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 错误处理和恢复管理器
 * 负责处理播放过程中的各种错误并提供恢复机制
 */
class ErrorRecoveryManager(
    private val context: Context,
    private val player: ExoPlayer,
    private val apiService: ApiService,
    private val scope: CoroutineScope
) {

    private val TAG = "ErrorRecoveryManager"

    // 错误重试配置
    private var maxRetryCount = 3
    private var retryDelay = 2000L // 2秒重试延迟
    private var currentRetryCount = 0

    // 错误监听器
    private var errorListeners = mutableListOf<ErrorListener>()

    /**
     * 处理媒体加载错误
     */
    suspend fun handleMediaLoadError(mediaId: String, retryAction: suspend () -> Unit): Boolean {
        Log.w(TAG, "Media load error for mediaId: $mediaId")
        
        if (currentRetryCount < maxRetryCount) {
            currentRetryCount++
            notifyError("加载失败，正在重试 ($currentRetryCount/$maxRetryCount)")
            
            delay(retryDelay)
            retryAction()
            return true
        } else {
            currentRetryCount = 0
            notifyError("加载失败，请检查网络连接")
            return false
        }
    }

    /**
     * 处理网络错误
     */
    suspend fun handleNetworkError(operation: String, retryAction: suspend () -> Unit): Boolean {
        Log.w(TAG, "Network error during: $operation")
        
        if (currentRetryCount < maxRetryCount) {
            currentRetryCount++
            notifyError("网络连接失败，正在重试 ($currentRetryCount/$maxRetryCount)")
            
            delay(retryDelay)
            retryAction()
            return true
        } else {
            currentRetryCount = 0
            notifyError("网络连接失败，请检查网络设置")
            return false
        }
    }

    /**
     * 处理API错误
     */
    suspend fun handleApiError(operation: String, error: Throwable, retryAction: suspend () -> Unit): Boolean {
        Log.e(TAG, "API error during $operation", error)
        
        if (currentRetryCount < maxRetryCount) {
            currentRetryCount++
            notifyError("服务暂时不可用，正在重试 ($currentRetryCount/$maxRetryCount)")
            
            delay(retryDelay)
            retryAction()
            return true
        } else {
            currentRetryCount = 0
            notifyError("服务暂时不可用，请稍后重试")
            return false
        }
    }

    /**
     * 处理播放错误
     */
    fun handlePlaybackError(error: Throwable) {
        Log.e(TAG, "Playback error", error)
        notifyError("播放失败: ${error.message ?: "未知错误"}")
    }

    /**
     * 重置重试计数
     */
    fun resetRetryCount() {
        currentRetryCount = 0
    }

    /**
     * 设置重试配置
     */
    fun setRetryConfig(maxRetries: Int, delayMs: Long) {
        maxRetryCount = maxRetries
        retryDelay = delayMs
    }

    /**
     * 添加错误监听器
     */
    fun addErrorListener(listener: ErrorListener) {
        errorListeners.add(listener)
    }

    /**
     * 移除错误监听器
     */
    fun removeErrorListener(listener: ErrorListener) {
        errorListeners.remove(listener)
    }

    /**
     * 通知错误
     */
    private fun notifyError(message: String) {
        errorListeners.forEach { it.onError(message) }
    }

    /**
     * 自动恢复播放
     */
    fun autoRecoverPlayback() {
        scope.launch {
            if (player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                // 尝试重新准备播放器
                try {
                    player.prepare()
                    delay(1000)
                    if (player.playbackState == androidx.media3.common.Player.STATE_READY) {
                        player.playWhenReady = true
                        notifyError("播放已恢复")
                    }
                } catch (e: Exception) {
                    handlePlaybackError(e)
                }
            }
        }
    }

    /**
     * 检查网络连接
     */
    suspend fun checkNetworkConnectivity(): Boolean {
        return try {
            // 这里可以添加实际的网络检查逻辑
            true // 暂时返回true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 错误监听器接口
     */
    interface ErrorListener {
        fun onError(message: String)
        fun onRecoverySuccess()
        fun onRecoveryFailed()
    }

    /**
     * 错误类型
     */
    enum class ErrorType {
        NETWORK_ERROR,
        API_ERROR,
        MEDIA_LOAD_ERROR,
        PLAYBACK_ERROR,
        UNKNOWN_ERROR
    }

    /**
     * 错误信息
     */
    data class ErrorInfo(
        val type: ErrorType,
        val message: String,
        val exception: Throwable? = null,
        val timestamp: Long = System.currentTimeMillis()
    )
}