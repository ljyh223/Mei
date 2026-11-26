package com.ljyh.mei.playback.log

import android.app.Application
import com.ljyh.mei.data.network.netease.PlaybackLogAccount
import com.ljyh.mei.data.network.netease.PlaybackLogUpload
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class PlaybackLogDrainerTest {
    @get:Rule val directory = TemporaryFolder()
    private val account = PlaybackLogAccount("123", "fictional-token", "fictional-device", emptyMap())
    private fun box() = PlaybackLogOutbox(File(directory.root, "outbox.json"))
    private fun row(id: String = "id") = PendingPlaybackLog(
        id = id, owner = account.owner, createdAtMs = 1000, ua = "ua", bi = "bi", entropy = ByteArray(100),
    )

    @Test fun acceptedChannelSurvivesFailureAndIsNotSentAgain() = runBlocking {
        val box = box()
        box.add(row())
        var offline = true
        val sent = mutableListOf<String>()
        val drainer = PlaybackLogDrainer(box, { account }, { _, channel, _ ->
            sent += channel
            if (offline && channel == "bi") throw IOException("offline")
            PlaybackLogUpload.ACCEPTED
        }, { 2000 })
        assertEquals(PlaybackLogDrain.RETRY, drainer.drain())
        assertEquals(setOf("ua"), box.pending(account.owner, 2000).single().accepted)
        offline = false
        assertEquals(PlaybackLogDrain.COMPLETE, drainer.drain())
        assertEquals(listOf("ua", "bi", "bi"), sent)
        assertTrue(box.pending(account.owner, 2000).isEmpty())
    }

    @Test fun successfulLargeQueueHandsOffAfterBoundedBatch() = runBlocking {
        val box = box()
        repeat(PlaybackLogDrainer.BATCH_SIZE + 1) { box.add(row("$it")) }
        var requests = 0
        val drainer = PlaybackLogDrainer(box, { account }, { _, _, _ ->
            requests++; PlaybackLogUpload.ACCEPTED
        }, { 2000 })
        assertEquals(PlaybackLogDrain.MORE, drainer.drain())
        assertEquals(PlaybackLogDrainer.BATCH_SIZE * 2, requests)
        assertEquals(1, box.pending(account.owner, 2000).size)
        assertEquals(PlaybackLogDrain.COMPLETE, drainer.drain())
        assertTrue(box.pending(account.owner, 2000).isEmpty())
    }

    @Test fun identityIsRecheckedBetweenChannels() = runBlocking {
        val box = box()
        box.add(row())
        var active: PlaybackLogAccount? = account
        var requests = 0
        val drainer = PlaybackLogDrainer(box, { active }, { _, _, _ ->
            requests++; active = null; PlaybackLogUpload.ACCEPTED
        }, { 2000 })
        assertEquals(PlaybackLogDrain.COMPLETE, drainer.drain())
        assertEquals(1, requests)
        assertTrue(box.pending(account.owner, 2000).isEmpty())
    }

    @Test fun cancellationLeavesUnacknowledgedRecordForNextWorker() = runBlocking {
        val box = box()
        box.add(row())
        val drainer = PlaybackLogDrainer(box, { account }, { _, _, _ -> throw CancellationException() }, { 2000 })
        assertTrue(runCatching { drainer.drain() }.exceptionOrNull() is CancellationException)
        val pending = box.pending(account.owner, 2000).single()
        assertEquals(0, pending.attempts)
        assertTrue(pending.accepted.isEmpty())
    }
}
