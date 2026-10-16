package com.ljyh.mei.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector

enum class MoreAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
) {
    ADD_TO_PLAYLIST("add", "添加到歌单", Icons.Rounded.Add),
    VIEW_PLAYLIST("view_playlist", "查看播放队列", Icons.AutoMirrored.Rounded.QueueMusic),
    COMMENT("comment", "查看评论", Icons.AutoMirrored.Rounded.Comment),
    DOWNLOAD("download", "下载", Icons.Rounded.Download),
    SONG_INFO("song_info", "歌曲信息", Icons.Rounded.Info),
    SLEEP_TIMER("sleep", "睡眠定时", Icons.Rounded.Bedtime),
    SHARE("share", "分享", Icons.Rounded.Share),
    BOTTOM_ACTION("bottom_action", "播放界面底部标签", Icons.Rounded.Tune);

    companion object {
        fun orderedBy(savedIds: String): List<MoreAction> {
            val actionsById = entries.associateBy(MoreAction::id)
            val saved = savedIds.split(',').mapNotNull(actionsById::get).distinct()
            return saved + entries.filterNot(saved::contains)
        }
    }
}
