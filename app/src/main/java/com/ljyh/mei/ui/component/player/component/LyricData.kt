package com.ljyh.mei.ui.component.player.component

import androidx.compose.ui.text.TextLayoutResult
import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.qq.u.LyricResult
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser


// 单句歌词（对应 [] 包裹的内容）
data class LyricLine(
    val lyric: String,
    val startTimeMs: Long,   // 句子开始时间（毫秒）
    val durationMs: Long,    // 句子总时长（毫秒）
    val words: List<LyricWord>, // 逐字列表
    var measuredWidth: Float? = null, // 句子宽度（px）
    var translation: String? = null,

    // 新增：用于缓存测量结果的字段
    @Volatile var textLayoutResult: TextLayoutResult? = null,
    @Volatile var wordMeasures: List<WordMeasure>? = null,
    @Volatile var translationLayoutResult: TextLayoutResult? = null
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
    val lyricLine: SyncedLyrics
)


sealed class LyricSourceData(val source: LyricSource, val priority: Int) {
    data class NetEase(val lyric: Lyric) : LyricSourceData(LyricSource.NetEaseCloudMusic, 2)
    data class QQMusic(val lyric: LyricResult.MusicMusichallSongPlayLyricInfoGetPlayLyricInfo.Data) : LyricSourceData(LyricSource.QQMusic, 1)
}

enum class LyricSource {
    Empty,
    NetEaseCloudMusic,
    QQMusic
}
data class SungState(
    val fullyHighlightedLines: Int, // 已被完全高亮的行数
    val partialHighlightWidth: Float   // 最后活动行上的高亮宽度 (像素)
)
data class WordMeasure(
    val text: String,
    val startOffset: Float, // 单词在整行文本中的起始X坐标
    val endOffset: Float    // 单词在整行文本中的结束X坐标
)
