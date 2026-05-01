package com.ljyh.mei.utils.lyric


import com.ljyh.mei.utils.lyric.xml.SimpleXmlParser
import com.ljyh.mei.utils.lyric.xml.XmlElement
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.karaoke.PhoneticLevel
import com.mocharealm.accompanist.lyrics.core.model.karaoke.copy
import com.mocharealm.accompanist.lyrics.core.model.karaoke.mapper.contentToString
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.ILyricsParser
import com.mocharealm.accompanist.lyrics.core.utils.PhoneticProvider
import kotlin.collections.get
class TTMLParser(
    private val fallbackPhoneticProvider: PhoneticProvider? = null,
) : ILyricsParser {
    override fun canParse(content: String): Boolean =
        content.contains("http://www.w3.org/ns/ttml")

    override fun parse(lines: List<String>): SyncedLyrics {
        return parse(lines.joinToString("") { it.trimIndent() })
    }

    // Workaround for AMLL and other tools not strictly following the spec
    private fun preformattingTTML(content: String): String =
        content
            .replace("  ", "")
            .replace(" </span><span", "</span> <span")
            .replace(",</span><span", ",</span> <span")

    private fun decodeXmlEntities(text: String): String {
        if (!text.contains('&')) return text
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
    }

    private fun splitTranslationByBracket(text: String): Pair<String, String?> {
        if (!text.endsWith('）')) return Pair(text.trim(), null)
        val bracketStart = text.lastIndexOf('（')
        if (bracketStart == -1) return Pair(text.trim(), null)

        val outside = text.substring(0, bracketStart).trim()
        val inside = text.substring(bracketStart + 1, text.length - 1).trim()
        return Pair(outside, inside.ifEmpty { null })
    }

    private fun XmlElement.attr(vararg names: String) =
        attributes.firstOrNull { it.name in names }?.value

    private fun XmlElement.hasRole(role: String) =
        attributes.any { it.name.endsWith(":role") && it.value == role }

    override fun parse(content: String): SyncedLyrics {
        val root = SimpleXmlParser().parse(preformattingTTML(content))

        val agentAlignments = parseMetadata(root)
        val translations = parseITunesTranslations(root)
        val transliterations = parseITunesTransliterations(root)

        val parsedLines = findAllPElements(root).mapNotNull { pElement ->
            parseSingleLine(pElement, agentAlignments, translations, transliterations)
        }

        val syncedLyrics = SyncedLyrics(lines = parsedLines.sortedBy { it.start })
        return applyFallbackPhonetics(syncedLyrics)
    }

    private fun extractAllText(element: XmlElement): String {
        val sb = StringBuilder()
        sb.append(element.text)
        for (child in element.children) {
            // we don't extract from translation or ruby spans if they are just metadata, but for safe fallback let's just extract all text that isn't translation
            if (child.name == "span" && (child.hasRole("x-translation") || child.hasRole("x-bg") || child.hasRole("x-roman"))) continue
            sb.append(extractAllText(child))
        }
        return sb.toString()
    }

    private fun parseSingleLine(
        p: XmlElement,
        alignments: Map<String, KaraokeAlignment>,
        translations: Map<String, String>,
        transliterations: Map<String, List<String>>
    ): ISyncedLine? {
        val start = p.attr("begin")?.parseAsTime() ?: return null
        val end = p.attr("end")?.parseAsTime() ?: return null
        val agentId = p.attr("ttm:agent")
        val itunesKey = p.attr("itunes:key", "key")

        // 1. 解析主音轨音节
        var syllables = parseSyllablesFromChildren(p.children)
        transliterations[itunesKey]?.let { phonetics ->
            if (phonetics.size == syllables.size) {
                syllables = syllables.mapIndexed { i, s -> s.copy(phonetic = phonetics[i]) }
            }
        }

        // 2. 解析主音轨注音 (Line Level)
        val linePhonetic =
            p.children.firstOrNull { it.name == "span" && it.hasRole("x-roman") }?.text?.trim()

        // 3. 解析主音轨翻译
        val inlineTranslation = p.children.firstOrNull {
            it.name == "span" && it.hasRole("x-translation") && !it.hasRole("x-bg")
        }?.text?.trim()
        val itunesTranslationPair = translations[itunesKey]?.let { splitTranslationByBracket(it) }

        // 4. 解析和声轨 (Background Vocals)
        val accompanimentLines = p.children
            .filter { it.name == "span" && it.hasRole("x-bg") }
            .mapNotNull { bgSpan ->
                parseAccompaniment(
                    bgSpan,
                    itunesKey,
                    alignments[agentId],
                    translations
                )
            }

        if (syllables.isEmpty() && accompanimentLines.isEmpty()) {
            val content = decodeXmlEntities(extractAllText(p)).trim()
            if (content.isEmpty()) return null
            return SyncedLine(
                content = content,
                translation = inlineTranslation ?: itunesTranslationPair?.first,
                start = start,
                end = end
            )
        }

        return KaraokeLine.MainKaraokeLine(
            syllables = syllables,
            translation = inlineTranslation ?: itunesTranslationPair?.first,
            alignment = alignments[agentId] ?: KaraokeAlignment.Start,
            start = start,
            end = end,
            accompanimentLines = accompanimentLines.ifEmpty { null },
            phonetic = linePhonetic
        )
    }

    private fun parseAccompaniment(
        bgSpan: XmlElement,
        parentKey: String?,
        alignment: KaraokeAlignment?,
        translations: Map<String, String>
    ): KaraokeLine.AccompanimentKaraokeLine? {
        val syllables = parseSyllablesFromChildren(bgSpan.children)
        if (syllables.isEmpty()) return null

        val bgKey = bgSpan.attr("itunes:key", "key") ?: parentKey
        val bgTranslation =
            bgSpan.children.firstOrNull { it.hasRole("x-translation") }?.text?.trim()
                ?: translations[bgKey]?.let {
                    splitTranslationByBracket(it).let { p ->
                        p.second ?: p.first
                    }
                }

        return KaraokeLine.AccompanimentKaraokeLine(
            syllables = syllables,
            translation = bgTranslation,
            alignment = alignment ?: KaraokeAlignment.Start,
            start = bgSpan.attr("begin")?.parseAsTime() ?: syllables.first().start,
            end = bgSpan.attr("end")?.parseAsTime() ?: syllables.last().end
        )
    }

    private fun parseITunesTranslations(element: XmlElement): Map<String, String> {
        val translations = mutableMapOf<String, String>()
        fun findTranslations(elem: XmlElement) {
            if (elem.name == "translation" || elem.name.endsWith(":translation")) {
                elem.children.forEach { textElem ->
                    if (textElem.name == "text") {
                        val key = textElem.attributes.find { it.name == "for" }?.value
                        val value = textElem.text
                        if (key != null && value.isNotBlank()) {
                            translations[key] = value.trim()
                        }
                    }
                }
            }
            elem.children.forEach { findTranslations(it) }
        }
        findTranslations(element)
        return translations
    }

    private fun parseITunesTransliterations(element: XmlElement): Map<String, List<String>> {
        val transliterations = mutableMapOf<String, List<String>>()

        // 递归寻找 <transliterations> 节点
        fun findTransliterations(elem: XmlElement) {
            if (elem.name == "transliterations" || elem.name.endsWith(":transliterations")) {
                elem.children.forEach { transElem ->
                    if (transElem.name == "transliteration" || transElem.name.endsWith(":transliteration")) {
                        transElem.children.forEach { textElem ->
                            if (textElem.name == "text") {
                                val key = textElem.attributes.find { it.name == "for" }?.value
                                // 提取所有内部 span 的文本作为音标列表
                                val phoneticSpans = textElem.children
                                    .filter { it.name == "span" }
                                    .map { decodeXmlEntities(it.text).trim() }

                                if (key != null && phoneticSpans.isNotEmpty()) {
                                    transliterations[key] = phoneticSpans
                                }
                            }
                        }
                    }
                }
            }
            elem.children.forEach { findTransliterations(it) }
        }

        findTransliterations(element)
        return transliterations
    }

    private fun applyFallbackPhonetics(syncedLyrics: SyncedLyrics): SyncedLyrics {
        val provider = fallbackPhoneticProvider ?: return syncedLyrics
        val processedLines = syncedLyrics.lines.map { line ->
            if (line !is KaraokeLine) return@map line

            // 如果当前行已有任何形式的发音（行级或音节级），则不进行 fallback
            val hasExistingPhonetic = !line.phonetic.isNullOrBlank() ||
                    line.syllables.any { !it.phonetic.isNullOrBlank() }

            if (hasExistingPhonetic) return@map line

            return@map when (provider.phoneticLevel) {
                PhoneticLevel.LINE -> {
                    line.copy(phonetic = provider.getPhonetic(line.syllables.contentToString()))
                }

                PhoneticLevel.SYLLABLE -> {
                    val newSyllables = line.syllables.map { syllable ->
                        syllable.copy(phonetic = provider.getPhonetic(syllable.content))
                    }
                    line.copy(syllables = newSyllables)
                }
            }
        }

        return SyncedLyrics(lines = processedLines)
    }

    /**
     * Parses a list of XmlElement children to extract KaraokeSyllables.
     * This function intelligently handles spacing by checking for `#text` nodes between `<span>` elements.
     */
    private fun parseSyllablesFromChildren(children: List<XmlElement>): List<KaraokeSyllable> {
        val syllables = mutableListOf<KaraokeSyllable>()
        for (i in children.indices) {
            val child = children[i]

            // We only care about <span> elements that are not for translation or background roles at this level.
            if (child.name == "span" && child.attributes.none {
                    it.name.endsWith(":role") && (it.value == "x-translation" || it.value == "x-bg")
                }) {
                val spanBegin = child.attributes.find { it.name == "begin" }?.value
                val spanEnd = child.attributes.find { it.name == "end" }?.value

                if (spanBegin != null && spanEnd != null && child.text.isNotEmpty()) {

                    var syllableContent = decodeXmlEntities(child.text)

                    val nextSibling = children.getOrNull(i + 1)
                    if (nextSibling != null && nextSibling.name == "#text") {
                        syllableContent += decodeXmlEntities(nextSibling.text)
                    }

                    syllables.add(
                        KaraokeSyllable(
                            content = syllableContent,
                            start = spanBegin.parseAsTime(),
                            end = spanEnd.parseAsTime()
                        )
                    )
                }
            }
        }

        // Trim the trailing space from the very last syllable of the line.
        if (syllables.isNotEmpty()) {
            val last = syllables.last()
            syllables[syllables.lastIndex] =
                last.copy(content = last.content.trimEnd())
        }

        return syllables
    }

    private fun parseMetadata(element: XmlElement): Map<String, KaraokeAlignment> {
        fun findMetadata(elem: XmlElement): XmlElement? {
            if (elem.name == "metadata") return elem
            return elem.children.firstNotNullOfOrNull { findMetadata(it) }
        }

        val metadata = findMetadata(element) ?: return emptyMap()

        return metadata.children
            .filter { it.name.endsWith(":agent") || it.name == "agent" }
            .mapIndexed { index, agent ->
                val id = agent.attributes.find {
                    it.name == "xml:id" || it.name == "id"
                }?.value ?: ""
                id to if (index == 0) KaraokeAlignment.Start else KaraokeAlignment.End
            }.toMap()
    }

    private fun getAlignmentFromAgent(
        element: XmlElement,
        agentAlignments: Map<String, KaraokeAlignment>
    ): KaraokeAlignment {
        val agentId = element.attributes.find { it.name == "ttm:agent" }?.value
        return agentAlignments[agentId] ?: KaraokeAlignment.Start
    }

    private fun findAllPElements(element: XmlElement): List<XmlElement> {
        val pElements = mutableListOf<XmlElement>()
        if (element.name == "p") {
            pElements.add(element)
        }
        element.children.forEach { child ->
            pElements.addAll(findAllPElements(child))
        }
        return pElements
    }
}