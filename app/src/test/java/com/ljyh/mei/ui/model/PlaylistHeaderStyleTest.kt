package com.ljyh.mei.ui.model

import com.ljyh.mei.data.model.response.PlaylistDetail
import com.ljyh.mei.data.model.response.toDomain
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistHeaderStyleTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun detail(
        name: String = "普通歌单",
        extra: String = "",
        creator: String = """{"nickname":"用户","userId":123}""",
    ) = json.decodeFromString<PlaylistDetail>(
        """{"code":200,"playlist":{
            "coverImgUrl":"https://example.com/playlist.jpg","creator":$creator,
            "id":1,"name":"$name","playCount":10,"subscribed":false,
            "subscribedCount":0,"trackCount":0,"trackIds":[],"tracks":[]$extra
        }}"""
    ).toDomain()

    @Test
    fun oldResponsesKeepStandardHeaderAndOriginalCoverEvenWithoutTracks() {
        val playlist = detail()
        assertEquals(PlaylistHeaderStyle.Standard, playlist.headerStyle())
        assertEquals("https://example.com/playlist.jpg", playlist.coverUrl)
        assertEquals(0L, playlist.updateTime)
        assertEquals(0, playlist.creatorUserType)
    }

    @Test
    fun userSuppliedNamesDoNotImpersonateSpecialPlaylists() {
        listOf("我喜欢的音乐", "私人雷达", "我的排行榜").forEach {
            assertEquals(PlaylistHeaderStyle.Standard, detail(name = it).headerStyle())
        }
    }

    @Test
    fun specialTypesSelectLikedAndChartHeadersWithoutDependingOnTitle() {
        assertEquals(PlaylistHeaderStyle.Liked, detail(extra = ""","specialType":5""").headerStyle())
        assertEquals(PlaylistHeaderStyle.Chart, detail(extra = ""","specialType":10""").headerStyle())
        assertEquals(PlaylistHeaderStyle.Chart, detail(extra = ""","ToplistType":"S" """).headerStyle())
    }

    @Test
    fun radarRequiresAnOfficialCreatorAndKeepsUpdateTime() {
        val playlist = detail(
            name = "私人雷达",
            extra = ""","updateTime":1792886400000""",
            creator = """{"nickname":"官方账号","userId":456,"userType":10}""",
        )
        assertEquals(PlaylistHeaderStyle.Radar, playlist.headerStyle())
        assertEquals(10, playlist.creatorUserType)
        assertEquals(1792886400000L, playlist.updateTime)
        assertEquals(PlaylistHeaderStyle.Standard, detail(
            creator = """{"nickname":"官方账号","userId":456,"userType":10}""",
        ).headerStyle())
        assertEquals(PlaylistHeaderStyle.Standard, detail(
            name = "私人雷达",
            creator = """{"nickname":"官方雷达","userId":456,"userType":4,"authStatus":1}""",
        ).headerStyle())
    }

    @Test
    fun accountIdOrVerificationCannotReplaceOfficialUserType() {
        listOf(
            """{"nickname":"网易云音乐","userId":48353}""",
            """{"nickname":"网易云音乐","userId":48353,"userType":0}""",
            """{"nickname":"认证账号","userId":456,"userType":4,"authStatus":1}""",
        ).forEach { creator ->
            assertEquals(PlaylistHeaderStyle.Standard, detail(
                name = "私人雷达", creator = creator,
            ).headerStyle())
        }
    }

    @Test
    fun officialCreatorStillUsesLikedAndChartStylesWhenApplicable() {
        val official = """{"nickname":"官方账号","userId":789,"userType":10}"""
        assertEquals(PlaylistHeaderStyle.Liked, detail(
            name = "私人雷达", extra = ""","specialType":5""", creator = official,
        ).headerStyle())
        assertEquals(PlaylistHeaderStyle.Chart, detail(
            name = "私人雷达", extra = ""","specialType":10""", creator = official,
        ).headerStyle())
    }
}
