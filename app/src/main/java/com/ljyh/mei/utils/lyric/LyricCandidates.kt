package com.ljyh.mei.utils.lyric

import com.ljyh.mei.ui.model.LyricCandidate
import com.ljyh.mei.ui.model.LyricData
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.ui.model.LyricVariant
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

internal fun buildLyricCandidates(
    sources: List<LyricSourceData>,
    isPureMusic: Boolean,
): List<LyricCandidate> {
    val candidates = mutableListOf<LyricCandidate>()

    fun add(variant: LyricVariant, source: LyricSourceData, expectedSource: LyricSource) {
        val data = runCatching { mergeLyrics(listOf(source), isPureMusic) }.getOrNull()
            ?.takeIf { !it.isPureMusic && it.source == expectedSource && it.lyricLine.lines.isNotEmpty() }
            ?: return
        candidates += LyricCandidate(variant, data, previewLyric(data))
    }

    sources.filterIsInstance<LyricSourceData.AM>().firstOrNull()?.let {
        add(LyricVariant.AppleMusic, it, LyricSource.AM)
    }
    sources.filterIsInstance<LyricSourceData.NetEase>().firstOrNull()?.let { source ->
        val lyric = source.lyric
        if (!lyric.yrc?.lyric.isNullOrBlank()) {
            add(
                LyricVariant.NeteaseYrc,
                LyricSourceData.NetEase(lyric.copy(lrc = lyric.lrc.copy(lyric = ""))),
                LyricSource.NetEaseCloudMusic,
            )
        }
        if (lyric.lrc.lyric.isNotBlank()) {
            add(
                LyricVariant.NeteaseLrc,
                LyricSourceData.NetEase(lyric.copy(yrc = null)),
                LyricSource.NetEaseCloudMusic,
            )
        }
    }
    sources.filterIsInstance<LyricSourceData.QQMusic>().firstOrNull()?.let { source ->
        if (source.isQRC && source.lyric.original.isNotBlank()) {
            add(LyricVariant.QqQrc, source.copy(lrcContent = null), LyricSource.QQMusic)
        }
        val lrc = source.lrcContent?.takeIf(String::isNotBlank)
            ?: source.lyric.original.takeIf { !source.isQRC && it.isNotBlank() }
        if (lrc != null) {
            add(
                LyricVariant.QqLrc,
                source.copy(lyric = source.lyric.copy(original = lrc), isQRC = false),
                LyricSource.QQMusic,
            )
        }
    }
    return candidates
}

internal fun lyricVariantOf(data: LyricData): LyricVariant? {
    if (data.isPureMusic) return null
    return when (data.source) {
        LyricSource.AM -> LyricVariant.AppleMusic
        LyricSource.NetEaseCloudMusic -> if (data.isVerbatim) LyricVariant.NeteaseYrc else LyricVariant.NeteaseLrc
        LyricSource.QQMusic -> if (data.isVerbatim) LyricVariant.QqQrc else LyricVariant.QqLrc
        else -> null
    }
}

internal data class ResolvedLyricChoice(
    val data: LyricData,
    val active: LyricVariant?,
    val preferredAvailable: Boolean,
)

internal fun resolveLyricChoice(
    requested: LyricVariant,
    autoData: LyricData,
    candidates: List<LyricCandidate>,
): ResolvedLyricChoice {
    val preferred = candidates.firstOrNull { it.variant == requested }
    val data = preferred?.data ?: autoData
    return ResolvedLyricChoice(data, preferred?.variant ?: lyricVariantOf(data), preferred != null)
}

private fun previewLyric(data: LyricData): String = data.lyricLine.lines
    .asSequence()
    .map { line ->
        when (line) {
            is KaraokeLine -> line.syllables.joinToString("") { it.content }
            is SyncedLine -> line.content
            else -> ""
        }.trim()
    }
    .firstOrNull(String::isNotBlank)
    .orEmpty()
