package com.ljyh.mei.data.model.response

import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.model.api.SearchResult
import com.ljyh.mei.data.model.domain.metadata
import com.ljyh.mei.data.model.domain.toDomain
import com.ljyh.mei.data.model.eapi.HomePageResourceShow
import com.ljyh.mei.data.model.response.AlbumDetail
import com.ljyh.mei.data.model.response.ListenDataRealtimeResponse
import com.ljyh.mei.data.model.response.ListenDataReportResponse
import com.ljyh.mei.data.model.response.Lyric
import com.ljyh.mei.data.model.response.SongUrl
import com.ljyh.mei.data.model.response.UserAccount
import com.ljyh.mei.data.model.response.UserPlaylist
import com.ljyh.mei.data.model.weapi.Comment
import com.ljyh.mei.data.model.weapi.EveryDaySongs
import com.ljyh.mei.data.model.weapi.FloorComment
import com.ljyh.mei.data.model.weapi.HighQualityPlaylistResult
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NeteaseResponseDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesDailyRecommendationWithOnlyConsumedSongFields() {
        val response = json.decodeFromString<EveryDaySongs>(
            """{"code":200,"data":{"dailySongs":[{"id":7,"name":"Song","dt":180000,"tns":["Alt"],"ar":[{"id":3,"name":"Artist","alias":[]}],"al":{"id":5,"name":"Album","picUrl":"cover"},"newServerField":true}],"serverMetadata":{}}}"""
        )

        val song = response.data.dailySongs.single()
        assertEquals(7L, song.id)
        assertEquals("Artist", song.ar.single().name)
        assertEquals("cover", song.al.picUrl)
        assertEquals("Alt", song.tns?.first())
    }

    @Test
    fun decodesSearchSongAndIgnoresUnconsumedResponseFields() {
        val response = json.decodeFromString<SearchResult>(
            """{"code":200,"result":{"songs":[{"id":11,"name":"Song","duration":210000,"artists":[{"id":4,"name":"Artist","picId":400,"alias":["Alias"],"unused":1}],"album":{"id":8,"name":"Album","picId":800,"unused":true},"fee":0}],"artists":[],"albums":[],"playlists":[]},"trp":{"rules":[]}}"""
        )

        val song = response.result.songs!!.single()
        assertEquals(11L, song.id)
        assertEquals(210000L, song.duration)
        assertEquals(800L, song.album.picId)
    }

    @Test
    fun decodesPlaylistDetailUsingTheCompactNestedTrackShape() {
        val response = json.decodeFromString<PlaylistDetail>(
            """{"code":200,"playlist":{"id":9,"name":"Playlist","description":null,"coverImgUrl":"cover","playCount":12,"subscribed":false,"subscribedCount":2,"trackCount":1,"creator":{"userId":6,"nickname":"Creator"},"trackIds":[{"id":7,"serverField":"ignored"}],"tracks":[{"id":7,"name":"Song","dt":180000,"tns":["Alt"],"al":{"id":8,"name":"Album","picUrl":"album-cover"},"ar":[{"id":4,"name":"Artist","alias":[]}],"privilege":{"code":0}}],"largeUnusedObject":{"ignored":true}}}"""
        )

        val domain = response.toDomain()
        assertEquals(9L, domain.id)
        assertEquals(7L, domain.tracks.single().id)
        assertEquals("album-cover", domain.tracks.single().coverUrl)
        assertEquals(7L, domain.trackIds.single())
    }

    @Test
    fun decodesArtistResponsesUsingFieldsConsumedByTheArtistScreen() {
        val detail = json.decodeFromString<ArtistDetail>(
            """{"code":200,"data":{"artist":{"name":"Artist","cover":"cover","avatar":"avatar","transNames":["Translated"],"alias":["Alias"],"identities":["Singer"],"briefDesc":"Bio","albumSize":4,"musicSize":12,"mvSize":2,"newField":"ignored"},"secondaryExpertIdentiy":[{"expertIdentiyName":"Composer","expertIdentiyCount":3,"extra":1}],"unconsumed":true}}"""
        )
        val album = json.decodeFromString<ArtistAlbum>(
            """{"code":200,"hotAlbums":[{"id":5,"name":"Album","picUrl":"cover","size":9,"artists":[{"id":2,"name":"Artist","unused":true}],"unused":true}],"artist":{"unused":true}}"""
        )
        val songs = json.decodeFromString<ArtistSong>(
            """{"code":200,"hotSongs":[{"id":7,"name":"Song","dt":180000,"ar":[{"id":2,"name":"Artist","alia":["Alt"],"unused":true}],"al":{"id":5,"name":"Album","pic":500},"unused":true}]}"""
        )

        assertEquals("Artist", detail.data.artist.name)
        assertEquals(3, detail.data.secondaryExpertIdentiy.single().expertIdentiyCount)
        assertEquals("cover", album.hotAlbums.single().picUrl)
        assertEquals("Artist", album.hotAlbums.single().artists.single().name)
        assertEquals("Alt", songs.hotSongs.single().artists.single().aliases.single())
        assertEquals(500L, songs.hotSongs.single().album.pictureId)
    }

    @Test
    fun decodesPlaybackUrlAndLyricResponsesWithSparseFields() {
        val url = json.decodeFromString<SongUrl>(
            """{"code":200,"data":[{"url":"https://music.invalid/song.mp3","br":320000,"unused":"metadata"}]}"""
        )
        val lyric = json.decodeFromString<Lyric>(
            """{"code":200,"lrc":{"lyric":"[00:01.00]line","version":8},"tlyric":{"lyric":"translation"},"yrc":{"lyric":"[100,200](0,100,0)word","version":1},"pureMusic":false,"unused":true}"""
        )

        assertEquals("https://music.invalid/song.mp3", url.data.single().url)
        assertEquals("[00:01.00]line", lyric.lrc.lyric)
        assertEquals("translation", lyric.tlyric?.lyric)
        assertEquals("[100,200](0,100,0)word", lyric.yrc?.lyric)
    }

    @Test
    fun decodesUnavailablePlaybackUrlWithNullQualityMetadata() {
        val response = json.decodeFromString<SongUrl>(
            """{"code":200,"data":[{"id":185697,"url":null,"encodeType":null,"level":null,"br":0}]}"""
        )

        assertEquals(185697L, response.data.single().id)
        assertNull(response.data.single().url)
    }

    @Test
    fun decodesCommentPagesAndFloorRepliesWithConsumedFields() {
        val page = json.decodeFromString<Comment>(
            """{"code":200,"data":{"comments":[{"commentId":9,"content":"hello","time":123,"timeStr":"just now","liked":true,"likedCount":2,"ipLocation":{"location":"Shanghai","ip":"hidden"},"showFloorComment":{"replyCount":4,"unused":1},"user":{"avatarUrl":"avatar","nickname":"listener","unused":true},"unused":true}],"hasMore":true,"totalCount":88,"unused":true}}"""
        )
        val replies = json.decodeFromString<FloorComment>(
            """{"code":200,"data":{"comments":[{"content":"reply","timeStr":"now","likedCount":1,"ipLocation":{"location":"Beijing"},"user":{"avatarUrl":"reply-avatar","nickname":"reply-user"},"unused":1}],"unused":true}}"""
        )

        assertEquals(88, page.data.totalCount)
        assertEquals(4, page.data.comments.single().showFloorComment?.replyCount)
        assertEquals("listener", page.data.comments.single().user.nickname)
        assertEquals("Beijing", replies.data.comments.single().ipLocation.location)
    }

    @Test
    fun decodesAlbumDetailsWithCompactSongAndAlbumFields() {
        val response = json.decodeFromString<AlbumDetail>(
            """{"code":200,"resourceState":true,"album":{"id":5,"name":"Album","picUrl":"cover","size":1,"description":"description","artists":[{"name":"Artist","unused":true}],"unused":true},"songs":[{"id":7,"name":"Song","dt":190000,"ar":[{"id":3,"name":"Artist","unused":true}],"al":{"id":5,"name":"Album","pic":500},"unused":true}],"unused":true}"""
        )

        val song = response.songs.single()
        assertEquals("Album", response.album.name)
        assertEquals("Artist", response.album.artists.single().name)
        assertEquals(500L, song.album.pictureId)
        assertEquals(190000L, song.duration)
    }

    @Test
    fun decodesUserAndListeningReportPayloadsUsingConsumedFields() {
        val account = json.decodeFromString<UserAccount>(
            """{"code":200,"account":{"opaque":"accepted"},"profile":{"nickname":"Listener","signature":"Bio","avatarUrl":"avatar","userId":7,"unused":true}}"""
        )
        val playlists = json.decodeFromString<UserPlaylist>(
            """{"code":200,"more":false,"playlist":[{"id":8,"name":"Mix","coverImgUrl":"cover","playCount":100,"trackCount":3,"creator":{"userId":7,"nickname":"Listener","avatarUrl":"avatar","unused":true},"unused":true}]}"""
        )
        val highQuality = json.decodeFromString<HighQualityPlaylistResult>(
            """{"code":200,"playlists":[{"id":9,"name":"HQ","coverImgUrl":"hq-cover","playCount":200}],"more":false,"lasttime":0,"total":1,"unused":true}"""
        )
        val realtime = json.decodeFromString<ListenDataRealtimeResponse>(
            """{"code":200,"data":{"listenTimeDistributionBlock":{"playDuration":90,"listenDays":4,"durationDetails":[{"duration":15,"unused":true}],"achievementTitle":{"mainTitle":"Listening","subTitle":"This week"}},"weekTodayListenBlock":{"songCount":2,"redCount":1,"coverUrls":[]},"unused":true}}"""
        )
        val report = json.decodeFromString<ListenDataReportResponse>(
            """{"code":200,"data":{"listenTimeBlock":{"playDurationText":"+20%"},"topStyleBlock":{"genreName":"Rock"},"topArtistBlock":{"sections":[{"artistName":"A"}]},"topSongBlock":{"sections":[{"songName":"S"}]}}}"""
        )

        assertEquals("Listener", account.profile?.nickname)
        assertEquals(8L, playlists.playlist.single().id)
        assertEquals(9L, highQuality.playlists.single().id)
        assertEquals(90, realtime.data?.listenTimeDistributionBlock?.playDuration)
        assertEquals("A", report.data?.topArtistBlock?.sections?.single()?.artistName)
        assertEquals("Listener", account.toDomain().profile?.nickname)
        assertEquals("Mix", playlists.toDomain().playlists.single().name)
        assertEquals("HQ", highQuality.toDomain().playlists.single().name)
    }

    @Test
    fun decodesPolymorphicHomeBlocksWithoutRequiringUnusedMetadata() {
        val response = json.decodeFromString<HomePageResourceShow>(
            """{"code":200,"data":{"blocks":[{"positionCode":"PAGE_RECOMMEND_RADAR","dslData":{"radar":{"title":"Radar","resources":[{"resourceId":"9","title":"Mix","coverImg":"cover"}]}}},{"positionCode":"PAGE_RECOMMEND_PRIVATE_RCMD_SONG","dslData":{"home_common":{"header":{"title":"Songs"},"content":{"items":[{"items":[{"resourceId":"7","title":"Song","coverUrl":"cover","artistName":"Artist"}]}]}}}}],"serverOnly":{"ignored":true}}}"""
        )

        assertEquals(2, response.data.blocks.size)
        assertEquals("PAGE_RECOMMEND_RADAR", response.data.blocks.first().positionCode)
        assertEquals(true, response.data.blocks.first().dslData.containsKey("radar"))
        val resource = response.data.blocks.first().dslData["radar"]!!
            .jsonObject["resources"]!!.jsonArray.single()
        val card = json.decodeFromJsonElement<
            HomePageResourceShow.Data.Block.DslData.BlockResource.Resource
        >(resource)
        assertEquals("Mix", card.title)
    }

    @Test
    fun decodesHomeCardsWithNullSubtitleAndNumericSongId() {
        val playlist = json.decodeFromString<
            HomePageResourceShow.Data.Block.DslData.BlockResource.Resource
        >(
            """{"resourceId":"9","title":"Popular tracks","coverImg":"cover","subTitle":null,"resourceExtInfo":null,"resourceInteractInfo":{"playCount":"100"}}"""
        )
        val daily = json.decodeFromString<
            HomePageResourceShow.Data.Block.DslData.BlockResource.Resource
        >(
            """{"resourceId":"10","resourceType":"star","singleLineTitle":"Daily","extInfo":{"songId":12345}}"""
        )

        assertEquals("Popular tracks", playlist.title)
        assertNull(playlist.subTitle)
        assertEquals("100", playlist.resourceInteractInfo?.playCount)
        assertEquals("12345", daily.extInfo.songIdText)
    }

    @Test
    fun decodesSparseCloudPhotoAndCollectedAlbumResponses() {
        val photos = json.decodeFromString<AlbumPhoto>(
            """{"code":200,"data":{"records":[{"imageUrl":"photo","serverOnly":true}],"page":{"cursor":"next"}}}"""
        )
        val albums = json.decodeFromString<UserAlbumList>(
            """{"code":200,"data":[{"id":1,"name":"Album","picUrl":"cover","size":4,"artists":[{"id":2,"name":"Artist"}]}],"count":1}"""
        )

        assertEquals("photo", photos.data.records.single().imageUrl)
        assertEquals(200, albums.code)
        assertEquals("Artist", albums.data.single().artists.single().name)
    }
}
