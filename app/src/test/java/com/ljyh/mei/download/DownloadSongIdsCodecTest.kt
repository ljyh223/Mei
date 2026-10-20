package com.ljyh.mei.download

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DownloadSongIdsCodecTest {
    @Test
    fun readsLegacyQueuedWorkWithoutLosingOrderOrDuplicates() {
        assertEquals(
            listOf("123", "local_中文", "123"),
            DownloadSongIdsCodec.decode("""["123","local_\u4e2d\u6587","123"]"""),
        )
    }

    @Test
    fun newWorkUsesSameStringArrayFormat() {
        val ids = listOf("123", "456", "123")
        assertEquals("""["123","456","123"]""", DownloadSongIdsCodec.encode(ids))
        assertEquals(ids, DownloadSongIdsCodec.decode(DownloadSongIdsCodec.encode(ids)))
    }

    @Test
    fun emptyTaskRemainsAnEmptyArray() {
        assertEquals("[]", DownloadSongIdsCodec.encode(emptyList()))
        assertEquals(emptyList<String>(), DownloadSongIdsCodec.decode("[]"))
    }

    @Test
    fun invalidWorkDataFailsParsingInsteadOfProducingNull() {
        for (value in listOf("", "{", "null", "{}", "[null]")) {
            assertThrows(SerializationException::class.java) { DownloadSongIdsCodec.decode(value) }
        }
    }
}
