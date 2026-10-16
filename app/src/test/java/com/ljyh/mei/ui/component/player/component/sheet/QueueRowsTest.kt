package com.ljyh.mei.ui.component.player.component.sheet

import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class QueueRowsTest {
    @Test
    fun duplicateSongsKeepDistinctKeysAcrossMoveAndMetadataReplacement() {
        val first = item("same")
        val second = item("same")
        val other = item("other")
        val queue = QueueRows(listOf(first, second, other))
        val firstKey = queue.rows[0].key
        val secondKey = queue.rows[1].key
        assertNotEquals(firstKey, secondKey)

        queue.move(1, 0)
        queue.sync(listOf(second, first, other))
        assertEquals(listOf(secondKey, firstKey), queue.rows.take(2).map(QueueRow::key))

        queue.sync(listOf(item("same"), first, other))
        assertEquals(listOf(secondKey, firstKey), queue.rows.take(2).map(QueueRow::key))
    }

    private fun item(id: String) = MediaItem.Builder().setMediaId(id).build()
}
