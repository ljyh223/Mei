package com.ljyh.mei.playback.queue

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayNextShuffleTest {
    @Test
    fun insertedItemsFollowCurrentWithoutChangingTheRestOfShuffle() {
        val orderAfterInsert = listOf(4, 0, 2, 5, 1, 3)

        assertEquals(
            listOf(4, 0, 2, 3, 5, 1),
            prioritizeInsertedShuffleItems(orderAfterInsert, currentIndex = 2, insertIndex = 3, count = 1),
        )
    }
}
