package com.ljyh.mei.ui.model

import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.response.MiniPlaylistDetail

enum class PlaylistHeaderStyle { Standard, Chart, Radar, Liked }

/** Special playlist metadata takes precedence over names supplied by users. */
fun MiniPlaylistDetail.headerStyle(): PlaylistHeaderStyle = when {
    specialType == 5 -> PlaylistHeaderStyle.Liked
    specialType == 10 || !toplistType.isNullOrBlank() -> PlaylistHeaderStyle.Chart
    name.contains("雷达") && creatorUserType == 10 -> PlaylistHeaderStyle.Radar
    else -> PlaylistHeaderStyle.Standard
}

data class UiPlaylist(
    val id: Long,
    val title: String,
    val count: Int,
    val subscriberCount: Long,
    val cover: String,
    val coverList: List<String>, // 或者 List<String> 如果要支持拼贴画
    val creatorName: String,
    val isCreator: Boolean = false,
    val description: String? = null,
    val tracks: List<MediaMetadata>, // 假设所有的列表都转成了 MediaMetadata
    val trackCount: Int = tracks.size,
    val playCount: Long? = null, // 每日推荐可能没有播放量
    val isSubscribed: Boolean = false, // 收藏状态
    val headerStyle: PlaylistHeaderStyle = PlaylistHeaderStyle.Standard,
    val updateTime: Long = 0,
)
