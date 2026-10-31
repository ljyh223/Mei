package com.ljyh.mei.data.network

import com.ljyh.mei.data.model.api.SearchResponseParser
import com.ljyh.mei.data.model.domain.SearchMatch
import io.ktor.http.parseQueryString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SearchContractTest {
    @Test
    fun overviewUsesV3XeapiFormWithFreshAntiCheatAndSingleResponseEncryptionFlag() {
        val request = buildSearchOverviewRequest("歌手 & album + 新曲")
        assertEquals("interface3.music.163.com", request.url.host)
        assertEquals("/xeapi/search/complex/page/v3", request.url.encodedPath)
        assertEquals(AntiCheatTokenVersion.V2, request.antiCheatTokenVersion)
        assertEquals("page_search_all", request.pageId)
        val envelope = Json.parseToJsonElement(request.encryptionPlaintext().decodeToString()).jsonObject
        assertEquals("e_r=true", envelope.getValue("queryString").jsonPrimitive.content)
        assertFalse(envelope.containsKey("body"))
        val form = parseQueryString(envelope.getValue("content").jsonPrimitive.content)
        assertEquals("歌手 & album + 新曲", form["keyword"])
        assertEquals("typing", form["channel"])
        assertEquals("normal", form["scene"])
        assertEquals("true", form["newStyle"])
        assertFalse(form.names().any { it in setOf("adextjson", "deviceId", "searchUuid", "cookie", "header") })
        assertEquals("t3", Json.parseToJsonElement(form["extInfo"]!!).jsonObject["searchUI"]?.jsonPrimitive?.content)
    }

    @Test
    fun overviewReadsHeterogeneousBlocksAndKeepsRecommendationsSeparate() {
        val song = """{"id":1,"name":"Song","ar":[{"id":2,"name":"Artist"}],"al":{"id":3,"name":"Album","picUrl":"cover"},"dt":120000,"tns":["译名"]}"""
        val response = Json.parseToJsonElement("""{
          "code":200,"data":{"cursor":{"page":0},"blocks":[
            {"blockCode":"search_block_best_match","resources":[
              {"resourceType":"artist","baseInfo":{"artistDTO":{"id":2,"name":"Artist","alias":["别名"],"picUrl":"avatar"}}},
              {"resourceType":"album","baseInfo":{"albumData":{"id":3,"name":"Album","size":12,"publishTime":1429632000000,"artists":[{"id":2,"name":"Artist"}]}}}
            ]},
            {"blockCode":"search_block_song","hlWords":["Artist"],"resources":[
              {"resourceType":"song","baseInfo":{"simpleSongData":$song}},
              {"resourceType":"song","baseInfo":{"simpleSongData":$song}}
            ]},
            {"blockCode":"search_block_song_rec","resources":[
              {"resourceType":"song","baseInfo":{"simpleSongData":$song}},
              {"resourceType":"song","baseInfo":{"simpleSongData":{"id":4,"name":"Recommendation","al":{},"ar":[]}}}
            ]},
            {"blockCode":"search_block_playlist","more":{"totalHit":null,"text":"查看全部999个歌单"},"resources":[
              {"resourceType":"playlist","baseInfo":{"pubPlaylistData":{"id":5,"name":"Playlist","trackCount":12,"playCount":25331042304,"creator":{"userId":6,"nickname":"Creator"}}}}
            ]},
            {"blockCode":"search_block_future","resources":[{"resourceType":"unknown","baseInfo":{}}]},
            {"blockCode":"search_block_album","resources":[{"resourceType":"album","baseInfo":null}]}
          ]}}
        """).jsonObject
        val result = SearchResponseParser.overview(response)
        assertEquals(listOf(1L), result.songs.map { it.id })
        assertEquals("译名", result.songs.single().tns)
        assertEquals("cover", result.songs.single().coverUrl)
        assertEquals(listOf(4L), result.recommendations.map { it.id })
        assertEquals(listOf("Artist"), result.highlightWords)
        assertEquals("别名", (result.bestMatches.first() as SearchMatch.Artist).artist.aliases.single())
        assertEquals(1429632000000L, (result.bestMatches.last() as SearchMatch.Album).album.publishTime)
        assertEquals(25_331_042_304L, result.playlists.single().playCount)
        assertNull(result.totalCount)
        assertFalse(result.hasMore)
    }

    @Test
    fun categoriesCountRawRowsForOffsetsButDeduplicateVisibleItems() {
        val response = Json.parseToJsonElement("""{"code":200,"result":{"artistCount":5,"artists":[
          {"id":1,"name":"Artist","picUrl":null,"alias":null},
          {"id":1,"name":"Artist"},{"id":0,"name":"Invalid"}
        ]}}""").jsonObject
        val result = SearchResponseParser.page(response, 100, offset = 1, limit = 3)
        assertEquals(4, result.nextOffset)
        assertEquals(5, result.totalCount)
        assertEquals(1, result.artists.size)
        assertTrue(result.hasMore)
        assertTrue(SearchResponseParser.page(response, 100, offset = 3, limit = 3).hasMore.not())
    }

    @Test
    fun absentCountsDoNotInventTotalsAndEmptyPageStopsPagination() {
        val response = Json.parseToJsonElement("""{"code":200,"result":{"albums":[] ,"hasMore":true}}""").jsonObject
        val result = SearchResponseParser.page(response, 10, 30, 30)
        assertNull(result.totalCount)
        assertFalse(result.hasMore)
        assertEquals(30, result.nextOffset)
    }

    @Test
    fun failureAndMalformedSuccessCannotMasqueradeAsEmptySearchResults() {
        for (raw in listOf("""{"code":301}""", """{"code":200}""")) {
            val response = Json.parseToJsonElement(raw).jsonObject
            assertThrows(RuntimeException::class.java) {
                SearchResponseParser.page(response, 1, 0, 30)
            }
            assertThrows(RuntimeException::class.java) { SearchResponseParser.overview(response) }
        }
    }
}
