package com.ljyh.mei.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MoreActionOrderTest {
    @Test
    fun savedOrderIgnoresRemovedAndDuplicateActionsAndAppendsNewOnes() {
        val actions = MoreAction.orderedBy("song_info,delete,comment,song_info,unknown")

        assertEquals(listOf(MoreAction.SONG_INFO, MoreAction.COMMENT), actions.take(2))
        assertEquals(MoreAction.entries.toSet(), actions.toSet())
        assertEquals(MoreAction.entries.size, actions.size)
        assertFalse(actions.any { it.id == "delete" })
    }
}
