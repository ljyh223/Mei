package com.ljyh.mei.playback.queue

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListQueueSelectionTest {
    @Test
    fun `tapped song starts the visible list at that song`() = runBlocking {
        val visibleItems = listOf("first", "tapped", "last").map { it to null }
        val queue = ListQueue(
            id = "playlist_1",
            items = visibleItems,
            startIndex = 0,
            position = 90_000,
        )

        val selected = queue.startingAt("tapped")!!.getInitialStatus()

        assertEquals(visibleItems.map { it.first }, selected.ids.map { it.first })
        assertEquals(1, selected.mediaItemIndex)
        assertEquals("tapped", selected.ids[selected.mediaItemIndex].first)
        assertEquals(0, selected.position)
    }

    @Test
    fun `song absent from the visible list cannot reuse that queue`() {
        val queue = ListQueue(id = "playlist_1", items = listOf("first" to null))

        assertNull(queue.startingAt("other"))
    }
}
