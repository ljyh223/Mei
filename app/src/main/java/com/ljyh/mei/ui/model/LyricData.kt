package com.ljyh.mei.ui.model

import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.qq.u.LyricResult
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics


data class LyricData(
    val isVerbatim: Boolean = false,
    val source: LyricSource = LyricSource.Empty,
    val lyricLine: SyncedLyrics
)


sealed class LyricSourceData(val source: LyricSource, val priority: Int) {
    data class NetEase(val lyric: Lyric) : LyricSourceData(LyricSource.NetEaseCloudMusic, 2)
    data class QQMusic(val lyric: LyricResult.MusicMusichallSongPlayLyricInfoGetPlayLyricInfo.Data) :
        LyricSourceData(LyricSource.QQMusic, 1)

    data class AM(val lyric: String) : LyricSourceData(LyricSource.AM, 3)
}

enum class LyricSource {
    Empty,
    NetEaseCloudMusic,
    QQMusic,
    AM,
}