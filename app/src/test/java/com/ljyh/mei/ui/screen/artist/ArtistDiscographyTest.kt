package com.ljyh.mei.ui.screen.artist

import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.domain.ArtistReleaseCategory
import org.junit.Assert.*
import org.junit.Test

class ArtistDiscographyTest {
    @Test fun usesServerTypeRatherThanTrackCountAndSelectsLatestByDate() {
        val releases = listOf(
            ArtistAlbum.HotAlbum(id = 1, name = "Old LP", type = "专辑", size = 1, publishTime = 100),
            ArtistAlbum.HotAlbum(id = 2, name = "New EP", type = "EP/Single", size = 8, publishTime = 300),
            ArtistAlbum.HotAlbum(id = 3, name = "Single", type = "Single", publishTime = 200),
            ArtistAlbum.HotAlbum(id = 4, name = "Unknown type", size = 1),
        ).map { it.toArtistAlbum() }
        val sections = artistDiscography(releases)
        assertEquals(2L, sections.latest?.id)
        assertEquals(listOf(1L, 4L), sections.albums.map { it.id })
        assertEquals(listOf(2L, 3L), sections.singles.map { it.id })
        assertEquals(8, sections.latest?.size)
        assertEquals("EP/Single", sections.latest?.releaseType)
        assertTrue(ArtistReleaseCategory.fromRoute("singles").matches(" EP "))
        assertEquals(ArtistReleaseCategory.All, ArtistReleaseCategory.fromRoute("invalid"))
    }
}
