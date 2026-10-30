package com.ljyh.mei.ui.model

import com.ljyh.mei.ui.component.player.overlay.PlayerPanel
import com.ljyh.mei.ui.component.player.overlay.nextPlayerPanel
import org.junit.Assert.*
import org.junit.Test

class PlayerActionProfileTest {
    @Test fun tabletDefaultsReplaceLyricsWithCommentsWithoutChangingPhoneSettings() {
        assertEquals(listOf(PlayerAction.PLAY_MODE, PlayerAction.QUEUE, PlayerAction.COMMENTS), PlayerActionProfile.Tablet.parse(""))
        assertEquals(listOf(PlayerAction.PLAY_MODE, PlayerAction.QUEUE, PlayerAction.LYRICS), PlayerActionProfile.Phone.parse(""))
        assertEquals(listOf(PlayerAction.MORE, PlayerAction.SLEEP_TIMER), PlayerActionProfile.Phone.parse("more,sleep"))
        assertFalse(PlayerActionProfile.Tablet.candidates.contains(PlayerAction.LYRICS))
        assertFalse(PlayerActionProfile.Phone.candidates.contains(PlayerAction.MORE))
    }
    @Test fun corruptAndOldSettingsAreSanitizedAndNeverLeaveAnEmptyToolbar() {
        assertEquals(PlayerActionProfile.Tablet.defaults, PlayerActionProfile.Tablet.parse("unknown,lyrics,more"))
        assertEquals(listOf(PlayerAction.PLAY_MODE, PlayerAction.SHARE, PlayerAction.QUEUE),
            PlayerActionProfile.Tablet.parse("share,unknown,share,queue,mode"))
        assertEquals(5, PlayerActionProfile.Tablet.parse("queue,comment,sleep,add,download,share,song_info").size)
        assertEquals(PlayerActionProfile.Phone.defaults, PlayerActionProfile.Phone.parse("unknown"))
    }
    @Test fun reorderCannotMoveOrDisplacePinnedPlaybackMode() {
        val profile = PlayerActionProfile.Tablet
        val defaults = profile.defaults
        assertEquals(defaults, profile.move(defaults, 0, 2))
        assertEquals(defaults, profile.move(defaults, 2, 0))
        assertEquals(listOf(PlayerAction.PLAY_MODE, PlayerAction.COMMENTS, PlayerAction.QUEUE), profile.move(defaults, 2, 1))
        assertEquals(defaults, profile.move(defaults, -1, 1))
        val serialized = PlayerAction.toSettings(profile.move(defaults, 2, 1))
        assertEquals(listOf(PlayerAction.PLAY_MODE, PlayerAction.COMMENTS, PlayerAction.QUEUE), profile.parse(serialized))
    }
    @Test fun menuAndToolbarUseTheSameCommands() {
        assertEquals(PlayerAction.DOWNLOAD, MoreAction.DOWNLOAD.playerAction)
        assertEquals(PlayerAction.COMMENTS, MoreAction.COMMENT.playerAction)
        assertEquals(PlayerAction.QUEUE, MoreAction.VIEW_PLAYLIST.playerAction)
        assertNull(MoreAction.BOTTOM_ACTION.playerAction)
        assertTrue(MoreAction.entries.filterNot { it == MoreAction.BOTTOM_ACTION }.all { it.playerAction != null })
    }
    @Test fun toolbarPanelToggleAndMenuSelectionHaveDifferentIntent() {
        assertEquals(PlayerPanel.Queue, nextPlayerPanel(PlayerPanel.Lyrics, PlayerPanel.Queue))
        assertEquals(PlayerPanel.Comments, nextPlayerPanel(PlayerPanel.Queue, PlayerPanel.Comments))
        assertEquals(PlayerPanel.Lyrics, nextPlayerPanel(PlayerPanel.Comments, PlayerPanel.Comments))
        assertEquals(PlayerPanel.Queue, nextPlayerPanel(PlayerPanel.Queue, PlayerPanel.Queue, toggle = false))
    }
    @Test fun shareRequiresAPublicLinkAndMetadataActionsRequireASong() {
        assertFalse(PlayerAction.SHARE.isAvailable(hasSong = true, canShare = false))
        assertTrue(PlayerAction.SHARE.isAvailable(hasSong = true, canShare = true))
        assertFalse(PlayerAction.DOWNLOAD.isAvailable(hasSong = false, canShare = false))
        assertFalse(PlayerAction.COMMENTS.isAvailable(hasSong = false, canShare = false))
        assertTrue(PlayerAction.SLEEP_TIMER.isAvailable(hasSong = false, canShare = false))
    }
}
