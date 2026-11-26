package com.ljyh.mei.playback.queue

import android.app.Application
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.core.app.ApplicationProvider
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.metadata
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.playback.transition.TransitionDeckPlayer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE, application = Application::class)
class PlaybackQueueManagerTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val resources = mutableListOf<Pair<PlaybackQueueManager, Player>>()

    @After fun release() {
        resources.forEach { (manager, player) -> manager.release(); player.release() }
        scope.cancel()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun lateFmResponseCannotAppendToASelectedPlaylist() {
        val response = CompletableDeferred<List<MediaItem>>()
        val returned = AtomicBoolean(false)
        val (manager, player) = fixture(fm = {
            withContext(NonCancellable) { response.await() }.also { returned.set(true) }
        })
        manager.startFmMode(track("1"))
        idle()
        manager.playQueue(queue("10", "11"))
        idle()
        response.complete(listOf(track("2")))
        await { returned.get() }
        assertEquals(listOf("10", "11"), ids(player))
        assertFalse(manager.isFmMode)
    }

    @Test fun supersededFmRequestCannotResetTheNewSessionsFetchGuard() {
        val first = CompletableDeferred<List<MediaItem>>()
        val second = CompletableDeferred<List<MediaItem>>()
        var calls = 0
        val (manager, player) = fixture(fm = {
            calls++
            withContext(NonCancellable) { if (calls == 1) first.await() else second.await() }
        })
        manager.startFmMode(track("1")); idle()
        manager.startFmMode(track("10")); idle()
        first.complete(listOf(track("2"))); idle()
        second.complete(listOf(track("11"), track("11"), track("10"))); idle()
        assertEquals(2, calls)
        assertEquals(listOf("10", "11"), ids(player))
    }

    @Test fun seedlessFmReplacesTheOldQueueAndItsPlaybackModes() {
        val (manager, player) = fixture()
        manager.playQueue(queue("1", "2"), startInShuffleMode = true); idle()
        player.repeatMode = Player.REPEAT_MODE_ONE
        manager.startFmModeById(null); idle()
        assertTrue(manager.isFmMode)
        assertEquals(0, player.mediaItemCount)
        assertFalse(player.shuffleModeEnabled)
        assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode)
        assertTrue(player.playWhenReady)
    }

    @Test fun fmSeedDoesNotWaitForMetadataAndCannotOverrideALaterPlaylist() {
        val details = CompletableDeferred<List<MediaItem>>()
        val (manager, player) = fixture(resolver = resolver(catalogue = { withContext(NonCancellable) { details.await() } }))
        manager.startFmModeById("1"); idle()
        assertEquals(listOf("1"), ids(player))
        manager.playQueue(queue("10")); idle()
        details.complete(listOf(track("1", "Old FM seed"))); idle()
        assertEquals(listOf("10"), ids(player))
        assertFalse(manager.isFmMode)
    }

    @Test fun olderSuspendingQueueCannotReplaceTheLastSelectedQueue() {
        val status = CompletableDeferred<Queue.Status>()
        val old = object : Queue {
            override val id = "old"
            override val title = "Old"
            override suspend fun getInitialStatus() = withContext(NonCancellable) { status.await() }
        }
        val (manager, player) = fixture()
        manager.playQueue(old); idle()
        manager.playQueue(queue("10", "11")); idle()
        status.complete(Queue.Status("Old", listOf("1" to track("1")), 0)); idle()
        assertEquals(listOf("10", "11"), ids(player))
    }

    @Test fun shuffleRowsAndDraggingFollowActualPlaybackOrderOnBothPlayers() {
        for (wrapped in listOf(false, true)) {
            val (manager, player) = fixture(wrapped = wrapped)
            observe(manager)
            manager.playQueue(queue("0", "1", "2", "3")); idle()
            player.applyQueueShuffleOrder(listOf(2, 0, 3, 1))
            manager.setShuffleModeEnabled(true); idle()
            assertEquals(listOf("2", "0", "3", "1"), manager.state.value.items.map { it.item.mediaId })
            val from = player.getMediaItemAt(1).queueEntryId!!
            val target = player.getMediaItemAt(3).queueEntryId!!
            assertTrue(manager.moveEntry(from, target)); idle()
            assertEquals(listOf(2, 0, 1, 3), player.queueOrderIndices())
            assertEquals(listOf("0", "1", "2", "3"), ids(player))
            assertEquals(listOf("2", "0", "1", "3"), manager.state.value.items.map { it.item.mediaId })
            player.seekTo(0, 0)
            player.seekToNextMediaItem()
            assertEquals("1", player.currentMediaItem!!.mediaId)
        }
    }

    @Test fun duplicateSongsKeepIdentityForMoveSeekAndRemoveAfterMetadataReplacement() {
        val (manager, player) = fixture()
        observe(manager)
        manager.playQueue(queue("1", "1", "2")); idle()
        val first = player.getMediaItemAt(0).queueEntryId!!
        val second = player.getMediaItemAt(1).queueEntryId!!
        assertNotEquals(first, second)
        assertTrue(manager.moveEntry(second, first)); idle()
        player.replaceMediaItem(0, track("1", "Updated").withQueueEntry(null, second))
        manager.seekToEntry(second); idle()
        assertEquals(second, manager.state.value.currentEntryId)
        assertEquals("Updated", player.currentMediaItem?.metadata?.title)
        assertTrue(manager.removeEntry(first)); idle()
        assertEquals(listOf(second, player.getMediaItemAt(1).queueEntryId), manager.state.value.items.map { it.entryId })
        assertFalse(manager.removeEntry(first))
        assertTrue(manager.removeEntry(second))
        assertFalse(manager.removeEntry(player.getMediaItemAt(0).queueEntryId!!))
    }

    @Test fun playNextIsNextInShuffleAndSurvivesSnapshotRestore() {
        val (manager, player) = fixture(wrapped = true)
        manager.playQueue(queue("0", "1", "2")); idle()
        player.applyQueueShuffleOrder(listOf(2, 0, 1))
        manager.setShuffleModeEnabled(true); idle()
        player.seekTo(0, 0)
        manager.playNext(listOf(track("9"), track("8"))); idle()
        val before = player.queueOrderIndices().map { player.getMediaItemAt(it).mediaId }
        assertEquals(listOf("2", "0", "9", "8", "1"), before)
        val snapshot = requireNotNull(manager.captureSnapshot(player.currentMediaItem?.metadata))
        val decoded = requireNotNull(PlaybackQueueSnapshotCodec.decode(PlaybackQueueSnapshotCodec.encode(snapshot)))
        val (restored, nextPlayer) = fixture(wrapped = true)
        assertTrue(restored.restore(decoded, shuffleEnabled = true, repeatMode = Player.REPEAT_MODE_ALL)); idle()
        assertEquals(before, nextPlayer.queueOrderIndices().map { nextPlayer.getMediaItemAt(it).mediaId })
        assertEquals(player.currentMediaItem?.queueEntryId, nextPlayer.currentMediaItem?.queueEntryId)
        nextPlayer.seekToNextMediaItem()
        assertEquals("9", nextPlayer.currentMediaItem?.mediaId)
        assertFalse(nextPlayer.playWhenReady)
    }

    @Test fun latentShuffleOrderIsRestoredEvenWhenShuffleIsCurrentlyDisabled() {
        val (manager, player) = fixture()
        manager.playQueue(queue("0", "1", "2")); idle()
        player.applyQueueShuffleOrder(listOf(2, 0, 1))
        val snapshot = requireNotNull(manager.captureSnapshot(null))
        val (nextManager, nextPlayer) = fixture()
        nextManager.restore(snapshot, false, Player.REPEAT_MODE_ONE); idle()
        assertFalse(nextPlayer.shuffleModeEnabled)
        assertEquals(Player.REPEAT_MODE_ONE, nextPlayer.repeatMode)
        nextManager.setShuffleModeEnabled(true); idle()
        assertEquals(listOf(2, 0, 1), nextPlayer.queueOrderIndices())
    }

    @Test fun restoredLocalAndCatalogueSongsWithEqualIdsUseSeparateMetadata() {
        val requests = ConcurrentLinkedQueue<String>()
        val loader = resolver(
            catalogue = { ids -> requests += "netease:${ids.joinToString()}"; ids.map { track(it, "Cloud $it") } },
            local = { ids -> requests += "local:${ids.joinToString()}"; ids.map { track(it, "Local $it") } },
        )
        val (manager, player) = fixture(resolver = loader)
        val snapshot = PlaybackQueueSnapshot(
            ids = listOf("42", "42"),
            entries = listOf(
                QueueEntrySnapshot("local", QueueSource(QueueSource.Type.LOCAL), QueueMediaKind.LOCAL),
                QueueEntrySnapshot("cloud", QueueSource(QueueSource.Type.ALBUM), QueueMediaKind.NETEASE),
            ),
        )
        manager.restore(snapshot, false, Player.REPEAT_MODE_ALL)
        await { (0..1).all { player.getMediaItemAt(it).metadata != null } }
        assertEquals(listOf("Local 42", "Cloud 42"), (0..1).map { player.getMediaItemAt(it).metadata?.title })
        assertEquals(setOf("local:42", "netease:42"), requests.toSet())
        assertEquals(QueueMediaKind.LOCAL, player.getMediaItemAt(0).queueMediaKind)
        assertEquals("local", player.getMediaItemAt(0).queueEntryId)
    }

    @Test fun missingLocalMetadataNeverFallsBackToTheCatalogueAndCanRetry() {
        var localAvailable = false
        val cloudRequested = AtomicBoolean(false)
        val loader = resolver(
            catalogue = { cloudRequested.set(true); emptyList() },
            local = { ids -> if (localAvailable) ids.map { track(it, "Recovered local") } else emptyList() },
        )
        val (manager, player) = fixture(resolver = loader)
        manager.playQueue(ListQueue("local_test", items = (0..19).map { id ->
            id.toString() to if (id == 10) null else track(id.toString())
        })); idle()
        val entryId = player.getMediaItemAt(10).queueEntryId!!
        val firstAttempt = manager.loadMetadataForEntries(listOf(entryId))
        await { firstAttempt.isCompleted }
        assertNull(player.getMediaItemAt(10).metadata)
        localAvailable = true
        manager.loadMetadataForEntries(listOf(entryId))
        await { player.getMediaItemAt(10).metadata != null }
        assertEquals("Recovered local", player.getMediaItemAt(10).metadata?.title)
        assertFalse(cloudRequested.get())
    }

    @Test fun closedPanelDoesNotBuildAUiQueueAndResubscriptionReadsTheCurrentPlayer() {
        val (manager, player) = fixture()
        manager.playQueue(queue("1", "2")); idle()
        assertTrue(manager.state.value.items.isEmpty())
        val observer = observe(manager); idle()
        assertEquals(listOf("1", "2"), manager.state.value.items.map { it.item.mediaId })
        observer.cancel(); idle()
        manager.addToQueue(listOf(track("3"))); idle()
        assertEquals(2, manager.state.value.items.size)
        observe(manager); idle()
        assertEquals(ids(player), manager.state.value.items.map { it.item.mediaId })
    }

    @Test fun emptyQueueClearsAndRestoreCannotOverwriteAnAlreadySelectedQueue() {
        val (manager, player) = fixture()
        manager.playQueue(queue("1", "2")); idle()
        assertFalse(manager.restore(PlaybackQueueSnapshot(listOf("3")), false, Player.REPEAT_MODE_ALL))
        assertEquals(listOf("1", "2"), ids(player))
        manager.playQueue(EmptyQueue); idle()
        assertEquals(0, player.mediaItemCount)
    }

    @Test fun metadataRequestsUseOccurrenceIdsAfterShuffleAndMoves() {
        val requested = ConcurrentLinkedQueue<String>()
        val (manager, player) = fixture(resolver = resolver(catalogue = { ids -> requested.addAll(ids); ids.map { track(it) } }))
        manager.playQueue(ListQueue("large", items = (0..19).map { it.toString() to null })); idle()
        await { player.getMediaItemAt(1).metadata != null }
        val key = player.getMediaItemAt(10).queueEntryId!!
        player.moveMediaItem(10, 15)
        manager.loadMetadataForEntries(listOf(key))
        await { player.getMediaItemAt(15).metadata != null }
        assertEquals("10", player.getMediaItemAt(15).mediaId)
        assertTrue(requested.contains("10"))
    }

    @Test fun startupRestoreDoesNotOverrideAnEmptyFmOrPendingUserQueue() {
        val (fm, fmPlayer) = fixture()
        fm.startFmModeById(null); idle()
        assertFalse(fm.restore(PlaybackQueueSnapshot(listOf("99")), false, Player.REPEAT_MODE_ALL))
        assertTrue(fm.isFmMode)
        assertEquals(0, fmPlayer.mediaItemCount)
        val (pending, player) = fixture()
        val status = CompletableDeferred<Queue.Status>()
        pending.playQueue(object : Queue {
            override val id = "pending"
            override val title: String? = null
            override suspend fun getInitialStatus() = status.await()
        }); idle()
        assertFalse(pending.restore(PlaybackQueueSnapshot(listOf("99")), false, Player.REPEAT_MODE_ALL))
        status.complete(Queue.Status(null, listOf("1" to track("1")), 0)); idle()
        assertEquals(listOf("1"), ids(player))
    }

    @Test fun localNegativeIdsAndMusicFreeIdsDoNotShareMetadataOrLoseTheirKindWhenReadded() {
        val requests = ConcurrentLinkedQueue<String>()
        val resolver = QueueMetadataResolver(
            catalogueDetails = { error("Local IDs must not reach the catalogue") },
            localDetails = { ids -> requests += "local"; ids.map { track(it, "Local") } },
            externalDetails = { ids -> requests += "plugin"; ids.map { track(it, "Plugin") } },
        )
        val (manager, player) = fixture(resolver = resolver)
        manager.restore(PlaybackQueueSnapshot(listOf("-42", "-42"), entries = listOf(
            QueueEntrySnapshot("local", QueueSource(QueueSource.Type.LOCAL)),
            QueueEntrySnapshot("plugin", mediaKind = QueueMediaKind.MUSIC_FREE),
        )), false, Player.REPEAT_MODE_ALL)
        await { (0..1).all { player.getMediaItemAt(it).metadata != null } }
        assertEquals(listOf("Local", "Plugin"), (0..1).map { player.getMediaItemAt(it).metadata?.title })
        assertEquals(setOf("local", "plugin"), requests.toSet())
        manager.addToQueue(listOf(player.getMediaItemAt(0)), QueueSource(QueueSource.Type.MANUAL)); idle()
        assertEquals(QueueMediaKind.LOCAL, player.getMediaItemAt(2).queueMediaKind)
        assertNotEquals(player.getMediaItemAt(0).queueEntryId, player.getMediaItemAt(2).queueEntryId)
    }

    @Test fun modeCyclingUsesCurrentPlayerStateAndHonoursLoopPreference() {
        for (loop in listOf(false, true)) {
            val (manager, player) = fixture(loop = loop)
            manager.playQueue(queue("1", "2")); idle()
            val listMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            assertEquals(listMode, player.repeatMode)
            manager.switchPlayMode(); idle()
            assertTrue(player.shuffleModeEnabled)
            assertEquals(listMode, player.repeatMode)
            manager.switchPlayMode(); idle()
            assertFalse(player.shuffleModeEnabled)
            assertEquals(Player.REPEAT_MODE_ONE, player.repeatMode)
            manager.switchPlayMode(); idle()
            assertFalse(player.shuffleModeEnabled)
            assertEquals(listMode, player.repeatMode)
        }
    }

    @Test fun seedlessFmStartsWithFetchedItemsAndKeepsItsDedicatedModes() {
        val (manager, player) = fixture(fm = { listOf(track("10"), track("11")) })
        manager.playQueue(queue("1", "2"), startInShuffleMode = true); idle()
        manager.startFmModeById(null); idle()
        assertEquals(listOf("10", "11"), ids(player))
        assertTrue(manager.isFmMode)
        assertTrue(player.playWhenReady)
        manager.switchPlayMode(); idle()
        assertFalse(player.shuffleModeEnabled)
        assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode)
    }

    @Test fun shuffleEditsInvalidateThePreparedTransitionAndNotifyPersistence() {
        var saved = 0
        val (manager, player) = fixture(wrapped = true, changed = { saved++ })
        manager.playQueue(queue("0", "1", "2")); idle()
        manager.setShuffleModeEnabled(true); idle()
        var invalidated = 0
        (player as TransitionDeckPlayer).onQueueEdited = { invalidated++ }
        val order = player.readQueueState().items
        val before = saved
        assertTrue(manager.moveEntry(order[0].entryId, order[1].entryId))
        assertTrue(invalidated > 0)
        assertTrue(saved > before)
    }

    private fun fixture(
        wrapped: Boolean = false,
        loop: Boolean = true,
        changed: () -> Unit = {},
        resolver: QueueMetadataResolver = resolver(),
        fm: suspend () -> List<MediaItem> = { CompletableDeferred<List<MediaItem>>().await() },
    ): Pair<PlaybackQueueManager, Player> {
        fun engine(): ExoPlayer {
            val context = ApplicationProvider.getApplicationContext<Application>()
            return ExoPlayer.Builder(context).setMediaSourceFactory(
                DefaultMediaSourceFactory(context).setDataSourceFactory { ByteArrayDataSource(byteArrayOf()) },
            ).build()
        }
        val player = if (wrapped) TransitionDeckPlayer(engine(), engine(), AudioAttributes.DEFAULT) else engine()
        val manager = PlaybackQueueManager(player, resolver, fm, scope, loopPlaybackEnabled = { loop }, onQueueChanged = changed)
        resources += manager to player
        return manager to player
    }

    private fun resolver(
        catalogue: suspend (List<String>) -> List<MediaItem> = { emptyList() },
        local: suspend (List<String>) -> List<MediaItem> = { emptyList() },
    ) = QueueMetadataResolver(catalogue, local, externalDetails = { emptyList() })

    private fun observe(manager: PlaybackQueueManager) = scope.launch(start = CoroutineStart.UNDISPATCHED) { manager.state.collect {} }
    private fun ids(player: Player) = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < deadline) { idle(); Thread.sleep(2) }
        idle()
        assertTrue("Queue operation did not finish", condition())
    }
    private fun queue(vararg ids: String) = ListQueue("test", items = ids.map { it to track(it) })
    private fun track(id: String, title: String = "Track $id") = MediaMetadata(
        id = id.toLong(), title = title, coverUrl = "", artists = emptyList(), duration = 180_000,
        album = MediaMetadata.Album(0, "Album"),
    ).toMediaItem()
}
