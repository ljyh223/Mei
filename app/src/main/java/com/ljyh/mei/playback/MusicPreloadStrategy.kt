package com.ljyh.mei.playback


import androidx.media3.common.C
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import kotlin.math.abs

/**
 * 极简预加载策略：
 * 仅预加载下一首歌曲 (n+1)，且预加载 5 秒长度以确保起播秒开。
 * 其他位置（上一首、下下首）均不预加载，防止 URL 过期浪费。
 */
class MusicPreloadStrategy : TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus> {

    // 当前播放索引，需要由 Service 更新
    var currentPlayingIndex: Int = -1

    override fun getTargetPreloadStatus(rankingData: Int): DefaultPreloadManager.PreloadStatus? {
        // 策略：只预加载下一首
        if (rankingData == currentPlayingIndex + 1) {
            // 参数1: 阶段 (STAGE_LOADED_TO_POSITION_MS)
            // 参数2: 值 (5000ms = 5秒)
            return DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(
                0,
                5000L
            )
        }
        return null
    }
}