package com.ljyh.mei.ui.component.player.overlay

import com.ljyh.mei.ui.component.player.OverlayState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSheetNavigationTest {
    @Test
    fun nestedSheetReturnsThroughPreviousContentsWithoutClosingContainer() {
        val navigation = PlayerSheetNavigation()
        navigation.show(OverlayState.MoreAction)
        navigation.show(OverlayState.AddToPlaylist(42))
        navigation.show(OverlayState.CreatePlaylist)

        assertEquals(OverlayState.CreatePlaylist, navigation.current.value)
        assertTrue(navigation.canGoBack)
        navigation.goBack()
        assertEquals(OverlayState.AddToPlaylist(42), navigation.current.value)
        assertTrue(navigation.isReturning)
        navigation.goBack()
        assertEquals(OverlayState.MoreAction, navigation.current.value)
        navigation.goBack()
        assertEquals(OverlayState.None, navigation.current.value)
        assertFalse(navigation.canGoBack)
    }

    @Test
    fun dismissClearsHistoryAndReopeningStartsFresh() {
        val navigation = PlayerSheetNavigation()
        navigation.show(OverlayState.MoreAction)
        navigation.show(OverlayState.SleepTimer)
        navigation.dismiss()
        navigation.show(OverlayState.Playlist)

        assertEquals(OverlayState.Playlist, navigation.current.value)
        assertFalse(navigation.canGoBack)
        assertFalse(navigation.isReturning)
        navigation.show(OverlayState.Playlist)
        assertFalse(navigation.canGoBack)
    }
}
