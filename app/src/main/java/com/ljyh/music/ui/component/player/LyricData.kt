package com.ljyh.music.ui.component.player

import com.ljyh.music.data.model.Lyric
import com.ljyh.music.data.model.qq.c.LyricCmd


// 单句歌词（对应 [] 包裹的内容）
data class LyricLine(
    val lyric: String,
    val startTimeMs: Long,   // 句子开始时间（毫秒）
    val durationMs: Long,    // 句子总时长（毫秒）
    val words: List<LyricWord>, // 逐字列表
    var measuredWidth: Float? = null, // 句子宽度（px）
    var translation: String? = null
)

data class LyricWord(
    val startTimeMs: Long,   // 开始时间（毫秒）（全局的，而非相对本行的开始时间）
    val durationMs: Long,    // 持续时间（毫秒）
    val text: String,        // 文字内容
    var progress: Float = 0f // 动画进度（0-1）
)


data class LyricLineA(
    val time: Long,
    val lyric: String,
)

data class LyricData(
    val isVerbatim: Boolean = false,
    val source: LyricSource = LyricSource.Empty,
    val lyricLine: List<LyricLine>
)

enum class LyricSource {
    Empty,
    NetEaseCloudMusic,
    QQMusic
}