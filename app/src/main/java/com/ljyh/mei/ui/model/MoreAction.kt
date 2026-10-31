package com.ljyh.mei.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector

enum class MoreAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val playerAction: PlayerAction? = null,
) {
    ADD_TO_PLAYLIST("add", "添加到歌单", Icons.Rounded.Add, PlayerAction.ADD_TO_PLAYLIST),
    VIEW_PLAYLIST("view_playlist", "查看播放队列", Icons.AutoMirrored.Rounded.QueueMusic, PlayerAction.QUEUE),
    COMMENT("comment", "查看评论", Icons.AutoMirrored.Rounded.Comment, PlayerAction.COMMENTS),
    DOWNLOAD("download", "下载", Icons.Rounded.Download, PlayerAction.DOWNLOAD),
    SONG_INFO("song_info", "歌曲信息", Icons.Rounded.Info, PlayerAction.SONG_INFO),
    SLEEP_TIMER("sleep", "睡眠定时", Icons.Rounded.Bedtime, PlayerAction.SLEEP_TIMER),
    SHARE("share", "分享", Icons.Rounded.Share, PlayerAction.SHARE),
    LYRICS("lyrics", "显示歌词", Icons.Rounded.Lyrics, PlayerAction.LYRICS),
    BOTTOM_ACTION("bottom_action", "自定义播放操作栏", Icons.Rounded.Tune);

    companion object {
        fun orderedBy(savedIds: String): List<MoreAction> {
            val actionsById = entries.associateBy(MoreAction::id)
            val saved = savedIds.split(',').mapNotNull(actionsById::get).distinct()
            return saved + entries.filterNot(saved::contains)
        }
    }
}
