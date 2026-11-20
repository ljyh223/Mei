package com.ljyh.mei.data.repository

import com.ljyh.mei.data.repository.artwork.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AppleArtistArtworkTest {
    private fun image(path: String = "photo", width: Int = 1600, height: Int = 1600) =
        """{"url":"https://is1-ssl.mzstatic.com/image/thumb/$path/{w}x{h}bb.jpg","width":$width,"height":$height,"bgColor":"563d31"}"""
    private fun item(extra: String = "", portrait: String = image()) = Json.parseToJsonElement(
        """{"id":"123","attributes":{"name":"Richard Wagner","artwork":$portrait$extra}}"""
    ).jsonObject

    @Test fun parsesPortraitBannerAndTransparentWordmarkIndependently() {
        val result = parseAppleArtistArtwork(item(
            """, "editorialArtwork":{"musicContentColorLogoTrimmed":${image("logo", 4800, 1200)},"storeFlowcase":${image("banner", 4320, 1080)}}"""
        ))!!
        assertEquals("Richard Wagner", result.name)
        assertEquals("563d31", result.portrait.background)
        assertEquals(4320, result.banner!!.width)
        assertTrue(result.logo!!.url.endsWith("{w}x{h}bb.png"))
        assertTrue(result.portrait.url.endsWith("{w}x{h}bb.jpg"))
    }

    @Test fun missingOrMalformedOptionalAssetsKeepThePortraitAndTextFallback() {
        val result = parseAppleArtistArtwork(item(
            """, "editorialArtwork":{"musicContentColorLogoTrimmed":{"url":"bad"},"subscriptionHero":null}"""
        ))!!
        assertNull(result.logo)
        assertNull(result.banner)
        assertNull(parseAppleArtistArtwork(item(portrait = "null")))
        assertNull(parseAppleArtistArtwork(item(portrait = image().replace("\"width\":1600", "\"width\":{}"))))
    }

    @Test fun invalidImageOriginsAndMissingIdentityCannotActivateTheNewPresentation() {
        assertNull(parseAppleArtistArtwork(item(portrait = image().replace("is1-ssl.mzstatic.com", "mzstatic.com.example.org"))))
        assertNull(parseAppleArtistArtwork(item(portrait = image().replace("https://", "http://"))))
        assertNull(parseAppleArtistArtwork(Json.parseToJsonElement("""{"attributes":{"artwork":${image()}}}""").jsonObject))
    }

    private fun candidates(vararg names: Pair<String, String>) = buildJsonArray {
        names.forEach { (id, name) -> add(buildJsonObject {
            put("id", id); putJsonObject("attributes") { put("name", name) }
        }) }
    }

    @Test fun translatedNamesMatchOnlyAnExplicitAliasAndPartialNamesNeverMatch() {
        val hits = candidates("1" to "Richard Wagner", "2" to "Richard Wagner Orchestra")
        assertEquals("1", matchAppleArtist(hits, listOf("理查德·瓦格纳", "Richard Wagner")))
        assertNull(matchAppleArtist(hits, listOf("Wagner")))
        assertEquals("1", matchAppleArtist(candidates("1" to "Beyoncé"), listOf("Beyonce")))
    }

    @Test fun homonymousArtistsAreRejectedWhileDuplicateRecordsForTheSameIdAreAllowed() {
        assertNull(matchAppleArtist(candidates("1" to "Artist", "2" to "Artist"), listOf("Artist")))
        assertEquals("1", matchAppleArtist(candidates("1" to "Artist", "1" to "Artist"), listOf("Artist")))
        assertNull(matchAppleArtist(candidates("invalid" to "Artist"), listOf("Artist")))
    }
}
