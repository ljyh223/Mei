package com.ljyh.mei.playback

import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 智能预加载策略管理器
 * 根据网络状况、播放行为和用户习惯智能调整预加载策略
 */
class PreloadStrategyManager(
    private val player: ExoPlayer,
    private val scope: CoroutineScope
) {

    private val TAG = "PreloadStrategyManager"

    // 预加载策略配置
    data class PreloadConfig(
        var windowSize: Int = 10,          // 预加载窗口大小
        var threshold: Int = 3,            // 触发预加载的阈值
        var batchSize: Int = 20,           // 每批加载数量
        var networkAware: Boolean = true,  // 是否根据网络状况调整
        var adaptive: Boolean = true       // 是否自适应调整
    )

    private var config = PreloadConfig()
    private var isMonitoring = false
    private var monitoringJob: kotlinx.coroutines.Job? = null

    // 播放行为统计
    private var playbackHistory = mutableListOf<PlaybackEvent>()
    private var averagePlayDuration: Long = 0
    private var skipCount: Int = 0

    /**
     * 开始预加载监控
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        monitoringJob = scope.launch {
            while (isMonitoring) {
                analyzePlaybackBehavior()
                adjustPreloadStrategy()
                delay(30000) // 每30秒分析一次
            }
        }
    }

    /**
     * 停止预加载监控
     */
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * 分析播放行为
     */
    private fun analyzePlaybackBehavior() {
        val currentTime = System.currentTimeMillis()
        
        // 记录播放事件
        if (player.isPlaying) {
            playbackHistory.add(PlaybackEvent(PlaybackEventType.PLAYING, currentTime))
        }

        // 清理过期的历史记录（保留最近10分钟）
        playbackHistory.removeAll { currentTime - it.timestamp > 600000 }

        // 计算平均播放时长
        calculateAveragePlayDuration()

        // 计算跳过次数
        calculateSkipCount()
    }

    /**
     * 调整预加载策略
     */
    private fun adjustPreloadStrategy() {
        if (!config.adaptive) return

        // 根据网络状况调整
        if (config.networkAware) {
            adjustForNetworkConditions()
        }

        // 根据播放行为调整
        adjustForPlaybackBehavior()

        // 确保配置在合理范围内
        ensureConfigBounds()

        Log.d(TAG, "Adjusted preload config: $config")
    }

    /**
     * 根据网络状况调整策略
     */
    private fun adjustForNetworkConditions() {
        // 这里可以添加实际的网络状况检测
        // 暂时使用固定逻辑
        val isGoodNetwork = true // 假设网络良好
        
        if (isGoodNetwork) {
            // 网络良好时增加预加载
            config.windowSize = 15
            config.batchSize = 25
        } else {
            // 网络较差时减少预加载
            config.windowSize = 5
            config.batchSize = 10
        }
    }

    /**
     * 根据播放行为调整策略
     */
    private fun adjustForPlaybackBehavior() {
        // 如果用户经常跳过歌曲，减少预加载
        if (skipCount > 3) {
            config.windowSize = maxOf(3, config.windowSize - 2)
            config.batchSize = maxOf(10, config.batchSize - 5)
        }

        // 如果平均播放时长较长，增加预加载
        if (averagePlayDuration > 120000) { // 超过2分钟
            config.windowSize = minOf(20, config.windowSize + 2)
            config.batchSize = minOf(30, config.batchSize + 5)
        }

        // 如果播放频繁但时间短（可能是浏览模式），减少预加载
        if (playbackHistory.size > 10 && averagePlayDuration < 30000) {
            config.windowSize = maxOf(5, config.windowSize - 3)
            config.batchSize = maxOf(15, config.batchSize - 8)
        }
    }

    /**
     * 确保配置在合理范围内
     */
    private fun ensureConfigBounds() {
        config.windowSize = config.windowSize.coerceIn(3, 20)
        config.threshold = config.threshold.coerceIn(1, 5)
        config.batchSize = config.batchSize.coerceIn(10, 30)
    }

    /**
     * 计算平均播放时长
     */
    private fun calculateAveragePlayDuration() {
        if (playbackHistory.size < 2) {
            averagePlayDuration = 0
            return
        }

        var totalDuration = 0L
        var count = 0

        for (i in 1 until playbackHistory.size) {
            val event1 = playbackHistory[i - 1]
            val event2 = playbackHistory[i]
            
            if (event1.type == PlaybackEventType.PLAYING && event2.type == PlaybackEventType.PAUSED) {
                totalDuration += event2.timestamp - event1.timestamp
                count++
            }
        }

        averagePlayDuration = if (count > 0) totalDuration / count else 0
    }

    /**
     * 计算跳过次数
     */
    private fun calculateSkipCount() {
        skipCount = playbackHistory.count { it.type == PlaybackEventType.SKIPPED }
    }

    /**
     * 记录跳过事件
     */
    fun recordSkip() {
        playbackHistory.add(PlaybackEvent(PlaybackEventType.SKIPPED, System.currentTimeMillis()))
    }

    /**
     * 记录播放事件
     */
    fun recordPlay() {
        playbackHistory.add(PlaybackEvent(PlaybackEventType.PLAYING, System.currentTimeMillis()))
    }

    /**
     * 记录暂停事件
     */
    fun recordPause() {
        playbackHistory.add(PlaybackEvent(PlaybackEventType.PAUSED, System.currentTimeMillis()))
    }

    /**
     * 获取当前预加载配置
     */
    fun getConfig(): PreloadConfig = config.copy()

    /**
     * 设置预加载配置
     */
    fun setConfig(newConfig: PreloadConfig) {
        config = newConfig.copy()
        ensureConfigBounds()
    }

    /**
     * 播放事件类型
     */
    enum class PlaybackEventType {
        PLAYING, PAUSED, SKIPPED
    }

    /**
     * 播放事件
     */
    data class PlaybackEvent(
        val type: PlaybackEventType,
        val timestamp: Long
    )

    /**
     * 预加载建议
     */
    data class PreloadAdvice(
        val shouldPreload: Boolean,
        val preloadSize: Int,
        val urgency: UrgencyLevel
    ) {
        enum class UrgencyLevel {
            LOW, MEDIUM, HIGH
        }
    }

    /**
     * 获取预加载建议
     */
    fun getPreloadAdvice(currentPosition: Int, totalItems: Int, loadedItems: Int): PreloadAdvice {
        val remaining = totalItems - loadedItems
        val distanceToEnd = totalItems - currentPosition

        return when {
            distanceToEnd <= config.threshold && remaining > 0 -> 
                PreloadAdvice(true, config.batchSize, PreloadAdvice.UrgencyLevel.HIGH)
            
            distanceToEnd <= config.threshold * 2 && remaining > 0 -> 
                PreloadAdvice(true, config.batchSize / 2, PreloadAdvice.UrgencyLevel.MEDIUM)
            
            else -> 
                PreloadAdvice(false, 0, PreloadAdvice.UrgencyLevel.LOW)
        }
    }
}