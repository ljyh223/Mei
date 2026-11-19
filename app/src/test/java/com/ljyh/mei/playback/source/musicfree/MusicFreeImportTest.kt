package com.ljyh.mei.playback.source.musicfree

import org.junit.Assert.*
import org.junit.Test

class MusicFreeImportTest {
    @Test fun readsTheStandardSubscriptionShapeWithoutChangingProxyUrls() {
        val url = "https://proxy.example/https://raw.example/plugins/source.js?version=1"
        val document = musicFreeImportDocument("""{"desc":"Test subscription","plugins":[
            {"name":"Source","version":"1.2.3","url":"$url"}
        ]}""".toByteArray(), "https://example.com/main.json") as MusicFreeImportDocument.Subscription
        assertEquals("Test subscription", document.manifest.description)
        assertEquals(MusicFreeSubscriptionEntry("Source", url, "1.2.3"), document.manifest.plugins.single())
    }

    @Test fun resolvesRelativeUrlsAgainstTheFinalManifestLocationAndDeduplicatesByUrl() {
        val document = musicFreeImportDocument(("\uFEFF  " + """{"plugins":[
            {"name":"A","url":"../plugins/a.js"},
            {"name":"Duplicate","url":"https://example.com/plugins/a.js"},
            {"name":"B","url":"/plugins/b.js"}
        ]}""").toByteArray(), "https://example.com/sub/main.json") as MusicFreeImportDocument.Subscription
        assertEquals(listOf("A", "B"), document.manifest.plugins.map { it.name })
        assertEquals(listOf("https://example.com/plugins/a.js", "https://example.com/plugins/b.js"), document.manifest.plugins.map { it.url })
    }

    @Test fun scriptBytesPassThroughForTheExistingPluginImporter() {
        val script = "module.exports={platform:'Fixture',search(){return {data:[]}}};".toByteArray()
        assertArrayEquals(script, (musicFreeImportDocument(script, "https://example.com/plugin") as MusicFreeImportDocument.Script).bytes)
    }

    @Test fun rejectsMalformedOrUnboundedManifestsAndInvalidDownloadUrls() {
        val invalid = listOf("{}", "[]", "{bad json", "{\"plugins\":[]}", "{\"plugins\":[null]}") +
            listOf("", "file:///tmp/a.js", "javascript:alert(1)", "ftp://example.com/a.js", "https://user:pass@example.com/a.js").map { url ->
                """{"plugins":[{"name":"A","url":"$url"}]}"""
            } + """{"plugins":[${List(101) { "{\"url\":\"/a.js\"}" }.joinToString(",")}]}"""
        invalid.forEach { raw ->
            assertThrows(raw, MusicFreeImportException::class.java) { musicFreeImportDocument(raw.toByteArray(), "https://example.com/sub.json") }
        }
        assertThrows(MusicFreeImportException::class.java) { musicFreeImportDocument(byteArrayOf(0xc3.toByte(), 0x28), "https://example.com/sub.json") }
    }

    @Test fun scriptFailuresCannotExposePrivateMessagesInTheImportReport() {
        val reason = musicFreeImportError(IllegalStateException("https://example.com/?secret=private"))
        assertFalse(reason.contains("secret"))
        assertFalse(reason.contains("private"))
    }
}
