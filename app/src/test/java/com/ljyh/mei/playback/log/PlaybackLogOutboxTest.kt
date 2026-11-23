package com.ljyh.mei.playback.log

import android.app.Application
import com.ljyh.mei.data.network.netease.playbackLogOwner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE, application = Application::class)
class PlaybackLogOutboxTest {
    @get:Rule val directory = TemporaryFolder()
    private fun log(id: String = "id", owner: String = "owner", now: Long = 1000) =
        PendingPlaybackLog(id, owner, now, "ua\n", "bi\n", ByteArray(100) { it.toByte() })

    @Test fun restartPreservesUnacknowledgedChannelAndStableEnvelopeInputs() = runBlocking {
        val file = java.io.File(directory.root, "outbox.json")
        val first = log()
        PlaybackLogOutbox(file).add(first)
        PlaybackLogOutbox(file).accepted(first.id, "ua")
        val reloaded = PlaybackLogOutbox(file).pending("owner", 2000).single()
        assertEquals(setOf("ua"), reloaded.accepted)
        assertArrayEquals(first.entropy, reloaded.entropy)
        assertEquals(first.fileName("bi"), reloaded.fileName("bi"))
        assertTrue(reloaded.fileName("bi").matches(Regex("flush_bi_[0-9]+_[0-9]+_[0-9]+")))
        PlaybackLogOutbox(file).accepted(first.id, "bi")
        assertTrue(PlaybackLogOutbox(file).pending("owner", 2000).isEmpty())
    }

    @Test fun changedAccountAndExpiredRecordsAreDiscarded() = runBlocking {
        val box = PlaybackLogOutbox(java.io.File(directory.root, "outbox.json"))
        box.add(log(owner = "account-a"))
        assertTrue(box.pending("account-b", 2000).isEmpty())
        box.add(log(now = 0))
        assertTrue(box.pending("owner", PlaybackLogOutbox.MAX_AGE_MS + 1).isEmpty())
    }

    @Test fun storageBudgetEvictsOldestWithoutWritingAnUnreadableQueue() = runBlocking {
        val file = java.io.File(directory.root, "outbox.json")
        val box = PlaybackLogOutbox(file, maxBytes = 2048)
        repeat(4) { index -> box.add(log(id = "record-$index").copy(ua = "x".repeat(700), bi = "y".repeat(700))) }
        assertTrue(file.length() <= 2048)
        val restored = PlaybackLogOutbox(file, maxBytes = 2048).pending("owner", 2000)
        assertEquals(listOf("record-3"), restored.map { it.id })
    }

    @Test fun retriesAreBoundedAndIdentityHashIncludesAccountTokenAndDevice() = runBlocking {
        val box = PlaybackLogOutbox(java.io.File(directory.root, "outbox.json"))
        box.add(log())
        repeat(PlaybackLogOutbox.MAX_ATTEMPTS) { box.failed("id") }
        assertTrue(box.pending("owner", 2000).isEmpty())
        val owner = playbackLogOwner("123", "test-token-a", "test-device-a")
        assertNotEquals(owner, playbackLogOwner("456", "test-token-a", "test-device-a"))
        assertNotEquals(owner, playbackLogOwner("123", "test-token-b", "test-device-a"))
        assertNotEquals(owner, playbackLogOwner("123", "test-token-a", "test-device-b"))
        assertEquals(64, owner.length)
        assertFalse(owner.contains("test-token"))
    }
}
