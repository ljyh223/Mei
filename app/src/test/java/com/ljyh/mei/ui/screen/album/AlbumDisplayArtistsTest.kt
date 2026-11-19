package com.ljyh.mei.ui.screen.album

import com.ljyh.mei.data.model.domain.AlbumOverview
import com.ljyh.mei.data.model.domain.MediaMetadata
import org.junit.Assert.*
import org.junit.Test

class AlbumDisplayArtistsTest {
    @Test fun participatingArtistsDoNotDuplicateTheAlbumArtist() {
        val album = AlbumOverview(1, "Album", "", "", 2, listOf("Main"), listOf(10))
        val track = MediaMetadata(2, "Song", "", listOf(MediaMetadata.Artist(10, "Main"), MediaMetadata.Artist(20, "Guest")), 1000, MediaMetadata.Album(1, "Album"))
        assertEquals(listOf(10L, 20L), albumDisplayArtists(album, listOf(track, track)).map { it.id })
    }
    @Test fun missingIdsStillKeepTheArtistNamesVisible() {
        val album = AlbumOverview(1, "Album", "", "", 0, listOf("Main"))
        assertEquals(listOf(MediaMetadata.Artist(0, "Main")), albumDisplayArtists(album, emptyList()))
    }
    @Test fun packageLinerKeepsTheOriginalAlbumOrderTitlesAndDurations() {
        val first = MediaMetadata(7, "相同曲名", "", emptyList(), 191_000, MediaMetadata.Album(1, "专辑"))
        val second = first.copy(id = 3, duration = 207_000)
        val liner = albumPackageTracks(listOf(first, second))
        assertEquals(listOf(1, 2), liner.map { it.number })
        assertEquals(listOf("相同曲名", "相同曲名"), liner.map { it.title })
        assertEquals(listOf(191_000L, 207_000L), liner.map { it.durationMillis })
    }

}
