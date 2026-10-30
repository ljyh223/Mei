package com.ljyh.mei.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class PlayerAction(val id: String, val label: String, val icon: ImageVector) {
    PLAY_MODE("mode", "播放模式", Icons.Rounded.Repeat),
    QUEUE("queue", "播放队列", Icons.AutoMirrored.Rounded.QueueMusic),
    LYRICS("lyrics", "歌词显示", Icons.Rounded.Lyrics),
    COMMENTS("comment", "查看评论", Icons.AutoMirrored.Rounded.Comment),
    SLEEP_TIMER("sleep", "睡眠定时", Icons.Rounded.Bedtime),
    ADD_TO_PLAYLIST("add", "添加到歌单", Icons.Rounded.Add),
    DOWNLOAD("download", "下载", Icons.Rounded.Download),
    SHARE("share", "分享", Icons.Rounded.Share),
    SONG_INFO("song_info", "歌曲信息", Icons.Rounded.Info),
    // Existing phone configurations may keep this shortcut; new configurations use the title menu.
    MORE("more", "更多", Icons.Rounded.MoreVert);

    fun isAvailable(hasSong: Boolean, canShare: Boolean): Boolean = when (this) {
        PLAY_MODE, QUEUE, SLEEP_TIMER, MORE -> true
        SHARE -> hasSong && canShare
        else -> hasSong
    }

    companion object {
        val defaultActions = listOf(PLAY_MODE, QUEUE, LYRICS)
        fun fromSettings(value: String): List<PlayerAction> = PlayerActionProfile.Phone.parse(value)
        fun toSettings(actions: List<PlayerAction>): String = actions.joinToString(",") { it.id }
    }
}

/** Persistent toolbar policy is independent of the current window's single/two-pane arrangement. */
enum class PlayerActionProfile {
    Phone, Tablet;

    val defaults: List<PlayerAction> get() = if (this == Tablet)
        listOf(PlayerAction.PLAY_MODE, PlayerAction.QUEUE, PlayerAction.COMMENTS)
    else PlayerAction.defaultActions

    val candidates: List<PlayerAction> get() = PlayerAction.entries.filter {
        it != PlayerAction.MORE && (this == Phone || it != PlayerAction.LYRICS)
    }

    fun parse(value: String): List<PlayerAction> {
        val decoded = value.split(',').mapNotNull { id -> PlayerAction.entries.find { it.id == id.trim() } }
            .filter { it in candidates || (this == Phone && it == PlayerAction.MORE) }.distinct()
        if (decoded.isEmpty()) return defaults
        // Keep valid legacy phone arrangements, including a deliberately omitted playback mode.
        return if (this == Phone) decoded.take(5)
        else (listOf(PlayerAction.PLAY_MODE) + decoded.filterNot { it == PlayerAction.PLAY_MODE }).take(5)
    }

    fun move(actions: List<PlayerAction>, from: Int, to: Int): List<PlayerAction> {
        if (from !in actions.indices || to !in actions.indices) return actions
        if (this == Tablet && (from == 0 || to == 0)) return actions
        return actions.toMutableList().apply { add(to, removeAt(from)) }
    }
}
