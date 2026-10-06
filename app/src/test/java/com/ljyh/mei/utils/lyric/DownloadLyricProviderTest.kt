package com.ljyh.mei.utils.lyric

import com.ljyh.mei.data.model.response.Lyric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadLyricProviderTest {

    @Test
    fun `pure music writes only the friendly placeholder`() {
        val lyric = lyric(
            lrc = "{\"t\":-1,\"c\":[{\"tx\":\"作曲:\"}]}",
            pureMusic = true
        )

        assertEquals("[00:00.00]纯音乐，请欣赏", lyric.toEmbeddedLyric())
    }

    @Test
    fun `source lrc is preferred over word-synced lyrics`() {
        val yrc = "[1000,2000](1000,500,0)逐(1500,500,0)字"
        val ytlrc = "[00:01.000]translation"

        val embedded = lyric(
            lrc = "[00:01.00]line lyric",
            yrc = yrc,
            ytlrc = ytlrc,
            tlyric = "[00:01.00]fallback translation"
        ).toEmbeddedLyric()

        assertEquals(
            "[00:01.00]line lyric\n[00:01.00]fallback translation",
            embedded
        )
    }

    @Test
    fun `yrc falls back to readable lrc with translation`() {
        val yrc = "[1000,2000](1000,500,0)Down (1500,500,0)for"
        val translation = "[00:01.00]翻译"

        assertEquals(
            "[00:01.000]Down for\n[00:01.000]翻译",
            lyric(yrc = yrc, tlyric = translation).toEmbeddedLyric()
        )
    }

    @Test
    fun `ttml is preserved when original embedding is enabled`() {
        val ttml = ttml()

        assertEquals(
            ttml,
            selectEmbeddedLyric(null, ttml, null, embedOriginalTtml = true)
        )
    }

    @Test
    fun `ttml is converted to readable lrc by default`() {
        assertEquals(
            "[00:01.000]Hello world",
            selectEmbeddedLyric(null, ttml(), null, embedOriginalTtml = false)
        )
    }

    @Test
    fun `ttml fallback keeps words and translation together`() {
        val ttml = """<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata"><body><div><p begin="00:00.201" end="00:03.000"><span begin="00:00.201" end="00:00.425">Down </span><span begin="00:00.425" end="00:00.745">for </span><span begin="00:00.745" end="00:03.000">one night</span><span ttm:role="x-translation">陪我一夜就好</span></p></div></body></tt>"""

        assertEquals(
            "[00:00.201]Down for one night\n[00:00.201]陪我一夜就好",
            selectEmbeddedLyric(null, ttml, null, embedOriginalTtml = false),
        )
    }

    @Test
    fun `qrc is converted to readable lrc even when original ttml is enabled`() {
        val qrc = QqLyricPayload(
            content = "[1000,2000]逐(1000,500)字(1500,500)",
            translation = "[00:01.000]translation",
            isQrc = true
        )

        assertEquals(
            "[00:01.000]逐字\n[00:01.000]translation",
            selectEmbeddedLyric(null, null, qrc, embedOriginalTtml = true)
        )
    }

    @Test
    fun `source lrc is preferred to ttml when original ttml is disabled`() {
        val source = lyric(lrc = "[00:01.00]Down for one night")

        assertEquals(
            "[00:01.00]Down for one night",
            selectEmbeddedLyric(source, ttml(), null, embedOriginalTtml = false),
        )
    }

    @Test
    fun `plain lrc keeps ytlrc translation when tlyric is absent`() {
        assertEquals(
            "[00:01.00]Down for one night\n[00:01.000]陪我一夜就好",
            lyric(
                lrc = "[00:01.00]Down for one night",
                ytlrc = "[00:01.000]陪我一夜就好",
            ).toEmbeddedLyric(),
        )
    }

    @Test
    fun `lrc and translation are embedded when yrc has no valid timeline`() {
        val lrc = "[00:01.00]main line"
        val translation = "[00:01.00]translated line"

        assertEquals(
            "$lrc\n$translation",
            lyric(lrc = lrc, yrc = "word timing", tlyric = translation).toEmbeddedLyric()
        )
    }

    @Test
    fun `contributor metadata is removed from otherwise valid lyrics`() {
        val metadata = "{\"t\":-1,\"c\":[{\"tx\":\"作词:\"},{\"tx\":\"artist\"}]}"

        assertEquals(
            "[00:01.00]main line",
            lyric(lrc = "$metadata\n[00:01.00]main line").toEmbeddedLyric()
        )
    }

    @Test
    fun `metadata without a timeline is not embedded as lyrics`() {
        val metadata = "{\"t\":-1,\"c\":[{\"tx\":\"作词:\"}]}"

        assertNull(lyric(lrc = metadata).toEmbeddedLyric())
    }

    private fun ttml() = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="00:01.000" end="00:02.000"><span begin="00:01.000" end="00:01.500">Hello  </span><span begin="00:01.500" end="00:02.000">world</span></p></div></body></tt>"""

    private fun lyric(
        lrc: String = "",
        yrc: String? = null,
        tlyric: String? = null,
        ytlrc: String? = null,
        pureMusic: Boolean = false
    ) = Lyric(
        code = 200,
        klyric = Lyric.Klyric(lyric = "", version = 0),
        lrc = Lyric.Lrc(lyric = lrc, version = 1),
        qfy = false,
        romalrc = null,
        sfy = false,
        sgc = false,
        tlyric = tlyric?.let { Lyric.Tlyric(lyric = it, version = 1) },
        ytlrc = ytlrc?.let { Lyric.Tlyric(lyric = it, version = 1) },
        yrc = yrc?.let { Lyric.Yrc(lyric = it, version = 1) },
        pureMusic = pureMusic
    )
}
