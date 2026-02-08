package com.ljyh.mei.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.ui.graphics.vector.ImageVector
enum class PlayerAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val isDefault: Boolean = false
) {
    // 播放模式 (循环/随机)
    PLAY_MODE("mode", "播放模式", Icons.Rounded.Repeat, true),

    // 播放队列
    QUEUE("queue", "播放队列", Icons.AutoMirrored.Rounded.QueueMusic, true),

    // 歌词开关
    LYRICS("lyrics", "歌词显示", Icons.Rounded.Lyrics, true),

    // 喜欢
    // LIKE("like", "我喜欢", Icons.Rounded.Favorite),

    // 睡眠定时器
    SLEEP_TIMER("sleep", "睡眠定时", Icons.Rounded.Bedtime),

    // 添加到歌单
    ADD_TO_PLAYLIST("add", "收藏到歌单", Icons.Rounded.Add),

    // 下载 (如果有)
    DOWNLOAD("download", "下载", Icons.Rounded.Download),

    // 更多菜单
    MORE("more", "更多", Icons.Rounded.MoreVert);

    companion object {
        // 将存储的字符串 (e.g., "mode,queue,lyrics") 转为 List<PlayerAction>
        fun fromSettings(settingString: String): List<PlayerAction> {
            if (settingString.isBlank()) return defaultActions
            return settingString.split(",")
                .mapNotNull { id -> entries.find { it.id == id } }
        }

        // 将 List<PlayerAction> 转为字符串存储
        fun toSettings(actions: List<PlayerAction>): String {
            return actions.joinToString(",") { it.id }
        }

        val defaultActions = listOf(PLAY_MODE, QUEUE, LYRICS)
    }
}