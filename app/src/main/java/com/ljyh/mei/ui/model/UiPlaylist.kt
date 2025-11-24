package com.ljyh.mei.ui.model

import com.ljyh.mei.data.model.MediaMetadata

data class UiPlaylist(
    val id: String,
    val title: String,
    val count: Int,
    val subscriberCount: Long,
    val coverUrl: List<String>, // 或者 List<String> 如果要支持拼贴画
    val creatorName: String,
    val isCreate: Boolean = false,
    val description: String? = null,
    val tracks: List<MediaMetadata>, // 假设所有的列表都转成了 MediaMetadata
    val trackCount: Int = tracks.size,
    val playCount: Long? = null, // 每日推荐可能没有播放量
    val isSubscribed: Boolean = false // 收藏状态
)