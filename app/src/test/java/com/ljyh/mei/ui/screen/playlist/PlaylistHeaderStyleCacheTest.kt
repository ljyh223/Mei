package com.ljyh.mei.ui.screen.playlist

import com.ljyh.mei.ui.model.PlaylistHeaderStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistHeaderStyleCacheTest {
    @Test
    fun hintsAreScopedToPlaylistAndLatestResultWins() {
        val cache = PlaylistHeaderStyleCache()
        assertNull(cache.get(11))
        cache.put(11, PlaylistHeaderStyle.Chart)
        cache.put(22, PlaylistHeaderStyle.Liked)
        cache.put(11, PlaylistHeaderStyle.Standard)
        assertEquals(PlaylistHeaderStyle.Standard, cache.get(11))
        assertEquals(PlaylistHeaderStyle.Liked, cache.get(22))
        assertNull(cache.get(33))
    }

    @Test
    fun evictsLeastRecentlyUsedHintWhenFull() {
        val cache = PlaylistHeaderStyleCache(capacity = 2)
        cache.put(1, PlaylistHeaderStyle.Standard)
        cache.put(2, PlaylistHeaderStyle.Chart)
        assertEquals(PlaylistHeaderStyle.Standard, cache.get(1))
        cache.put(3, PlaylistHeaderStyle.Liked)
        assertNull(cache.get(2))
        assertEquals(PlaylistHeaderStyle.Standard, cache.get(1))
        assertEquals(PlaylistHeaderStyle.Liked, cache.get(3))
    }
}
