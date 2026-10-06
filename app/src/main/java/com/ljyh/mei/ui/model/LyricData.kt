package com.ljyh.mei.ui.model

import com.ljyh.mei.data.model.Lyric
import com.ljyh.unblockneteasemusic.model.MusicLyrics
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics


data class LyricData(
    val isVerbatim: Boolean = false,
    val isPureMusic: Boolean = false,
    val source: LyricSource = LyricSource.Empty,
    val lyricLine: SyncedLyrics
)


sealed class LyricSourceData(val source: LyricSource, val priority: Int) {
    data class NetEase(val lyric: Lyric) : LyricSourceData(LyricSource.NetEaseCloudMusic, 2)
    data class QQMusic(
        val lyric: MusicLyrics,
        val isQRC: Boolean = true,
        val lrcContent: String? = null
    ) : LyricSourceData(LyricSource.QQMusic, 1)

    data class AM(val lyric: String) : LyricSourceData(LyricSource.AM, 3)
}

enum class LyricSource {
    Empty,
    NetEaseCloudMusic,
    QQMusic,
    AM,
    Loading
}
