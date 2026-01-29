package com.ljyh.mei.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector

enum class MoreAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val frequency: Int, // 使用频率，数字越大表示越常用 (0-10)
    val riskLevel: Int, // 风险等级，数字越大表示误触风险越高 (0-10)
) {
    // --- 歌曲操作 ---
    ADD_TO_PLAYLIST("add", "添加到歌单", Icons.Rounded.Add, 8, 2), // 常用，低风险

    SHARE("share", "分享", Icons.Rounded.Share, 5, 3),  // 中等使用，中等风险

    DOWNLOAD("download", "下载", Icons.Rounded.Download, 6, 4), // 离线，中等风险

    DELETE("delete", "删除", Icons.Rounded.Delete, 1, 9), // 低频，高风险 (手滑就没了)

    // --- 播放列表操作 ---
    VIEW_PLAYLIST("view_playlist", "查看歌单", Icons.AutoMirrored.Rounded.PlaylistAdd, 7, 1), // 跳转

    // --- 高级功能 ---
    SLEEP_TIMER("sleep", "睡眠定时", Icons.Rounded.Bedtime, 3, 2), // 辅助功能
    // 播放界面底部功能
    BOTTOM_ACTION("bottom_action", "播放界面底部标签", Icons.Rounded.Tune, 4, 4);

    companion object {
        // 简单工厂方法
        fun fromId(id: String?): MoreAction? {
            return entries.find { it.id == id }
        }
    }
}

enum class SortOrder {
    FREQUENCY, // 常用的靠前
    RISK, // 低风险的靠前
}