package com.ljyh.mei.utils.lyric

import android.content.Context
import com.ljyh.mei.constants.EmbedOriginalTtmlKey
import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.PlayerRepository
import com.ljyh.mei.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Resolves download lyrics through the app's repositories and prepares an embeddable payload. */
@Singleton
class DownloadLyricProvider @Inject constructor(
    private val repository: PlayerRepository,
    private val lyricPreloader: LyricPreloader,
    @param:ApplicationContext private val context: Context,
) {
    suspend fun getEmbeddedLyric(songId: String): String? = coroutineScope {
        val netEase = async {
            runCatching { repository.getLyricV1(songId) }.getOrNull()
        }
        val ttml = async {
            runCatching { repository.getAMLLyric(songId) }.getOrNull()
        }
        val qq = async { resolveQqLyric(songId) }
        val embedOriginalTtml = context.dataStore.data.first()[EmbedOriginalTtmlKey] ?: false

        selectEmbeddedLyric(
            netEase = (netEase.await() as? Resource.Success)?.data,
            ttml = (ttml.await() as? Resource.Success)?.data,
            qq = qq.await(),
            embedOriginalTtml = embedOriginalTtml
        )
    }

    private suspend fun resolveQqLyric(songId: String): QqLyricPayload? {
        lyricPreloader.resolveQqSource(songId)?.let { return it.toPayload() }

        val detail = repository.getSongDetail(songId) as? Resource.Success ?: return null
        val metadata = detail.data.songs.firstOrNull()?.toMediaMetadata() ?: return null
        return lyricPreloader.resolveQqSource(songId, metadata)?.toPayload()
    }

    private fun com.ljyh.mei.ui.model.LyricSourceData.QQMusic.toPayload() =
        QqLyricPayload(
            content = lyric.lyric,
            translation = lyric.trans.takeIf(String::isNotBlank),
            isQrc = isQRC
        )
}

internal data class QqLyricPayload(
    val content: String,
    val translation: String?,
    val isQrc: Boolean,
)

private const val PURE_MUSIC_LYRIC = "[00:00.00]纯音乐，请欣赏"

private val yrcTimeline = Regex(
    pattern = """(?m)^\s*\[\d+,\d+]\s*.*\(\d+,\d+,\d+\)"""
)
private val lrcTimeline = Regex(
    pattern = """(?m)^\s*\[\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?].*\S.*$"""
)

internal fun selectEmbeddedLyric(
    netEase: Lyric?,
    ttml: String?,
    qq: QqLyricPayload?,
    embedOriginalTtml: Boolean,
): String? {
    if (netEase?.pureMusic == true) return PURE_MUSIC_LYRIC

    val validTtml = ttml?.trim()?.takeIf { TTMLParser().canParse(it) }
    if (validTtml != null) {
        if (embedOriginalTtml) return validTtml
        runCatching { EnhancedLrcEncoder.encode(TTMLParser().parse(validTtml)) }
            .getOrNull()
            ?.let { return it }
    }

    val yrc = netEase?.yrc?.lyric
        ?.withoutContributorMetadata()
        ?.takeIf(yrcTimeline::containsMatchIn)
    if (yrc != null) {
        val translation = sequenceOf(netEase.ytlrc?.lyric, netEase.tlyric?.lyric)
            .mapNotNull { it?.withoutContributorMetadata() }
            .firstOrNull(lrcTimeline::containsMatchIn)
        runCatching { EnhancedLrcEncoder.encode(YRCParser.parse(yrc, translation)) }
            .getOrNull()
            ?.let { return it }
    }

    if (qq?.isQrc == true && QRCParser.canParse(qq.content)) {
        runCatching { EnhancedLrcEncoder.encode(QRCParser.parse(qq.content, qq.translation)) }
            .getOrNull()
            ?.let { return it }
    }

    netEase?.toPlainEmbeddedLrc()?.let { return it }
    if (qq != null && !qq.isQrc) {
        val main = qq.content.withoutContributorMetadata().takeIf(lrcTimeline::containsMatchIn)
        val translation = qq.translation
            ?.withoutContributorMetadata()
            ?.takeIf(lrcTimeline::containsMatchIn)
        if (main != null) return joinLyricBlocks(main, translation)
    }
    return null
}

internal fun Lyric.toEmbeddedLyric(): String? =
    selectEmbeddedLyric(this, ttml = null, qq = null, embedOriginalTtml = false)

private fun Lyric.toPlainEmbeddedLrc(): String? {
    val main = lrc.lyric
        .withoutContributorMetadata()
        .takeIf(lrcTimeline::containsMatchIn)
        ?: return null
    val translation = tlyric?.lyric
        ?.withoutContributorMetadata()
        ?.takeIf(lrcTimeline::containsMatchIn)
    return joinLyricBlocks(main, translation)
}

private fun String.withoutContributorMetadata(): String =
    lineSequence()
        .filterNot { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith("{\"t\":") && trimmed.contains("\"c\":[")
        }
        .joinToString("\n")
        .trim()

private fun joinLyricBlocks(main: String, translation: String?): String =
    if (translation == null) main else "$main\n$translation"
