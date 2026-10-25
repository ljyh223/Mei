package com.ljyh.mei.utils.share

import com.ljyh.mei.data.model.domain.AlbumOverview
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaMetadata
import com.ljyh.mei.data.model.room.Song
import com.ljyh.mei.data.model.room.SourceType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class MediaShareLinkTest {
    private fun song(id: Long = 42) = MediaMetadata(
        id = id, title = " 再会 ", coverUrl = "https://example.com/cover.jpg", duration = 1000,
        artists = listOf(MediaMetadata.Artist(1, "Vaundy"), MediaMetadata.Artist(2, " "),
            MediaMetadata.Artist(1, "Vaundy")),
        album = MediaMetadata.Album(7, "strobo"),
    )

    private fun storedSong(id: String = "local_42", type: SourceType = SourceType.LOCAL, matched: Long? = null) = Song(
        id = id, title = "再会", artist = listOf("Vaundy"), album = "strobo", cover = "content://cover/1",
        duration = 10, path = "/storage/emulated/0/Music/song.flac", sourceType = type,
        matchedNeteaseId = matched,
    )

    @Test
    fun songSharesThePublicPageWithReadableMetadata() {
        val content = requireNotNull(MediaShareLink.song(song()))
        assertEquals("https://music.163.com/song?id=42", content.url)
        assertEquals("分享歌曲《再会》 - Vaundy\nhttps://music.163.com/song?id=42", content.text)
        assertFalse(content.text.contains("example.com"))
    }

    @Test
    fun playlistAndAlbumUseTheirOwnPageTypesAndPreserveLongIds() {
        val playlist = requireNotNull(MediaShareLink.playlist(Long.MAX_VALUE, "夜晚", "创建者"))
        assertEquals("https://music.163.com/playlist?id=${Long.MAX_VALUE}", playlist.url)
        val album = requireNotNull(MediaShareLink.album(AlbumOverview(7, "strobo", "", "", 3,
            listOf("Vaundy", " ", "Vaundy"))))
        assertEquals("https://music.163.com/album?id=7", album.url)
        assertEquals("Vaundy", album.subtitle)
    }

    @Test
    fun placeholderAndDailyRecommendationIdsDoNotProducePublicLinks() {
        for (id in listOf(0L, -1L, Long.MIN_VALUE)) {
            assertNull(MediaShareLink.song(song(id)))
            assertNull(MediaShareLink.playlist(id, "每日推荐", ""))
            assertNull(MediaShareLink.album(AlbumOverview(id, "", "", "", 0, emptyList())))
        }
    }

    @Test
    fun multilineNamesDoNotInjectExtraLinkLinesAndMissingCreditsAreOmitted() {
        val content = requireNotNull(MediaShareLink.playlist(7, "  夜晚\n音乐\t精选 ", " "))
        assertEquals("夜晚 音乐 精选", content.title)
        assertEquals("分享歌单《夜晚 音乐 精选》\n${content.url}", content.text)
        assertEquals("歌单", MediaShareLink.playlist(7, " ", "")?.title)
    }

    @Test
    fun positiveLocalIdsAndHashedIdsNeverBecomeFakeNeteaseLinks() {
        for (id in listOf("local_42", "local_track-abc", "42")) {
            val metadata = storedSong(id = id).toMediaMetadata()
            assertTrue(metadata.id > 0)
            assertEquals(0L, metadata.neteaseId)
            assertNull(MediaShareLink.song(metadata))
        }
    }

    @Test
    fun matchedLocalSongSharesTheMatchWhileRetainingItsPlaybackId() {
        val metadata = storedSong(matched = 1234).toMediaMetadata()
        assertEquals(42L, metadata.id)
        assertEquals(1234L, metadata.neteaseId)
        assertEquals("https://music.163.com/song?id=1234", MediaShareLink.song(metadata)?.url)
        assertFalse(requireNotNull(MediaShareLink.song(metadata)).text.contains("/storage/"))
        for (invalidMatch in listOf(0L, -1L)) {
            assertNull(MediaShareLink.song(storedSong(matched = invalidMatch).toMediaMetadata()))
        }
    }

    @Test
    fun downloadedSongKeepsItsPublicCatalogueId() {
        val metadata = storedSong(id = "5678", type = SourceType.DOWNLOAD).toMediaMetadata()
        assertEquals("https://music.163.com/song?id=5678", MediaShareLink.song(metadata)?.url)
    }

    @Test
    fun oldSerializedMetadataKeepsItsCatalogueIdAndNewLocalMetadataKeepsTheUnavailableSentinel() {
        val legacy = """{"id":42,"title":"再会","coverUrl":"","artists":[],"duration":1000,"album":{"id":7,"title":"strobo"}}"""
        assertEquals(42L, Json.decodeFromString<MediaMetadata>(legacy).neteaseId)
        val local = storedSong().toMediaMetadata()
        assertEquals(0L, Json.decodeFromString<MediaMetadata>(Json.encodeToString(local)).neteaseId)
    }
}
