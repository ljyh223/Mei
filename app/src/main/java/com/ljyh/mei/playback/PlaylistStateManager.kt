package com.ljyh.mei.playback

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media3.common.MediaItem
import com.ljyh.mei.data.model.PlaylistDetail
import com.ljyh.mei.data.model.toMiniPlaylistDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 歌单状态管理器
 * 负责管理当前播放歌单的状态、进度和历史记录
 */
class PlaylistStateManager {

    // 当前歌单状态
    private val _playlistState = MutableStateFlow<PlaylistState>(PlaylistState.Idle)
    val playlistState: StateFlow<PlaylistState> = _playlistState

    // 播放历史记录
    private val playbackHistory = mutableListOf<PlaybackHistoryItem>()

    // 当前播放位置
    private var currentPosition: Int = 0

    /**
     * 设置当前歌单
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setCurrentPlaylist(playlistDetail: PlaylistDetail, startPosition: Int = 0) {
        currentPosition = startPosition
        _playlistState.value = PlaylistState.Playing(
            playlist = playlistDetail.toMiniPlaylistDetail(),
            currentPosition = startPosition,
            totalTracks = playlistDetail.playlist.trackCount
        )
        
        // 记录播放历史
        recordPlaybackHistory(playlistDetail, startPosition)
    }

    /**
     * 更新播放位置
     */
    fun updatePosition(position: Int) {
        currentPosition = position
        val currentState = _playlistState.value
        if (currentState is PlaylistState.Playing) {
            _playlistState.value = currentState.copy(currentPosition = position)
        }
    }

    /**
     * 记录播放历史
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun recordPlaybackHistory(playlistDetail: PlaylistDetail, position: Int) {
        val historyItem = PlaybackHistoryItem(
            playlistId = playlistDetail.playlist.Id,
            playlistName = playlistDetail.playlist.name,
            position = position,
            timestamp = System.currentTimeMillis()
        )
        
        // 移除重复的历史记录
        playbackHistory.removeAll { it.playlistId == playlistDetail.playlist.Id }
        
        // 添加到历史记录开头
        playbackHistory.add(0, historyItem)
        
        // 限制历史记录数量
        if (playbackHistory.size > 50) {
            playbackHistory.removeLast()
        }
    }

    /**
     * 获取播放历史
     */
    fun getPlaybackHistory(): List<PlaybackHistoryItem> {
        return playbackHistory.toList()
    }

    /**
     * 清除播放历史
     */
    fun clearPlaybackHistory() {
        playbackHistory.clear()
    }

    /**
     * 获取最近播放的歌单
     */
    fun getRecentPlaylists(limit: Int = 10): List<PlaybackHistoryItem> {
        return playbackHistory.distinctBy { it.playlistId }.take(limit)
    }

    /**
     * 保存播放状态
     */
    fun savePlaybackState() {
        val currentState = _playlistState.value
        if (currentState is PlaylistState.Playing) {
            // 这里可以添加持久化存储逻辑
            // 例如保存到SharedPreferences或数据库
        }
    }

    /**
     * 恢复播放状态
     */
    fun restorePlaybackState() {
        // 这里可以添加上次播放状态的恢复逻辑
        // 例如从SharedPreferences或数据库读取
    }

    /**
     * 重置状态
     */
    fun reset() {
        _playlistState.value = PlaylistState.Idle
        currentPosition = 0
    }

    /**
     * 歌单状态
     */
    sealed class PlaylistState {
        object Idle : PlaylistState()
        data class Playing(
            val playlist: com.ljyh.mei.data.model.MiniPlaylistDetail,
            val currentPosition: Int,
            val totalTracks: Int
        ) : PlaylistState()
        data class Error(val message: String) : PlaylistState()
        object Completed : PlaylistState()
    }

    /**
     * 播放历史项
     */
    data class PlaybackHistoryItem(
        val playlistId: Long,
        val playlistName: String,
        val position: Int,
        val timestamp: Long
    )

    /**
     * 播放统计
     */
    data class PlaybackStatistics(
        val totalPlayTime: Long,        // 总播放时间（毫秒）
        val tracksPlayed: Int,          // 播放过的歌曲数量
        val completedPlaylists: Int,    // 完整播放的歌单数量
        val favoriteGenres: List<String> // 最喜欢的音乐类型
    )

    /**
     * 播放进度
     */
    data class PlaybackProgress(
        val playlistId: Long,
        val trackPosition: Int,         // 当前播放的歌曲位置
        val playbackPosition: Long,     // 当前歌曲的播放位置（毫秒）
        val totalDuration: Long,        // 歌曲总时长（毫秒）
        val lastUpdated: Long           // 最后更新时间
    )

    /**
     * 歌单播放统计
     */
    data class PlaylistStatistics(
        val playlistId: Long,
        val playCount: Int,             // 播放次数
        val averageCompletion: Float,   // 平均完成度（0-1）
        val favoriteTracks: List<Long>, // 最常播放的歌曲
        val lastPlayed: Long            // 最后播放时间
    )
}