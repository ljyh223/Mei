package com.ljyh.mei.playback.queue

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.PLACEHOLDER_URI
import com.ljyh.mei.data.model.domain.createPlaceholder
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.playback.playbackRepeatMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal fun prioritizeInsertedShuffleItems(
    order: List<Int>, currentIndex: Int, insertIndex: Int, count: Int,
): List<Int> {
    val nextOrder = order.filterNot { it in insertIndex until insertIndex + count }.toMutableList()
    val afterCurrent = nextOrder.indexOf(currentIndex).takeIf { it >= 0 }?.plus(1) ?: 0
    nextOrder.addAll(afterCurrent, (insertIndex until insertIndex + count).toList())
    return nextOrder
}

/** Owns queue commands and projects Media3 state. Media3 remains the only live playlist. */
@OptIn(UnstableApi::class)
class PlaybackQueueManager(
    private val player: Player,
    private val metadataResolver: QueueMetadataResolver,
    private val fmRecommendations: suspend () -> List<MediaItem>,
    scope: CoroutineScope,
    private val loopPlaybackEnabled: () -> Boolean = { true },
    private val onQueueChanged: () -> Unit = {},
) : Player.Listener {
    private val commandScope = CoroutineScope(
        scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]) + Dispatchers.Main.immediate,
    )
    private var sessionScope = newSessionScope()
    private var revision = 0L
    private var queueRequestRevision = 0L
    private var queueLoadJob: Job? = null
    private var fetchingFmRevision: Long? = null
    private val loadingTracks = mutableSetOf<QueueTrackKey>()
    private val _state = MutableStateFlow(PlaybackQueueState())
    val state = _state.asStateFlow()
    private val _fmMode = MutableStateFlow(false)
    val fmMode = _fmMode.asStateFlow()
    val isFmMode: Boolean get() = _fmMode.value
    internal val canRestore: Boolean
        get() = revision == 0L && queueRequestRevision == 0L && player.mediaItemCount == 0

    init {
        player.addListener(this)
        // Do not build a complete UI list while the queue panel is closed.
        commandScope.launch {
            _state.subscriptionCount.map { it > 0 }.distinctUntilChanged().collect { observed ->
                if (observed) publishState(force = true)
            }
        }
    }

    private fun newSessionScope() = CoroutineScope(
        commandScope.coroutineContext + SupervisorJob(commandScope.coroutineContext[Job]),
    )

    private fun beginSession(fm: Boolean): Long {
        revision++
        sessionScope.cancel()
        sessionScope = newSessionScope()
        fetchingFmRevision = null
        loadingTracks.clear()
        _fmMode.value = fm
        return revision
    }

    private fun publishState(force: Boolean = false) {
        if (force || _state.subscriptionCount.value > 0) _state.value = player.readQueueState()
    }

    private fun cancelQueueLoad() {
        queueRequestRevision++
        queueLoadJob?.cancel()
    }

    fun playQueue(queue: Queue, startInShuffleMode: Boolean = false, playWhenReady: Boolean = true) {
        commandScope.launch {
            cancelQueueLoad()
            val requestRevision = queueRequestRevision
            queueLoadJob = commandScope.launch loadQueue@ {
                try {
                    val status = queue.getInitialStatus()
                    if (requestRevision != queueRequestRevision) return@loadQueue
                    val items = status.ids.map { (id, item) ->
                        (item ?: createPlaceholder(id)).withQueueEntry(queue.source)
                    }
                    beginSession(fm = false)
                    player.shuffleModeEnabled = false
                    if (items.isEmpty()) {
                        player.clearMediaItems()
                    } else {
                        player.setMediaItems(items, status.mediaItemIndex.coerceIn(items.indices), status.position.toLong().coerceAtLeast(0))
                        player.repeatMode = playbackRepeatMode(loopPlaybackEnabled())
                        player.shuffleModeEnabled = startInShuffleMode
                        player.prepare()
                        player.playWhenReady = playWhenReady
                    }
                    queueChanged()
                    checkAndLoadMetadata()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("QueueManager", "Unable to start queue", error)
                }
            }
        }
    }

    fun startFmMode(seedItem: MediaItem) = startFm(seedItem)
    fun startFmModeById(seedId: String?) = startFm(seedId?.let(::createPlaceholder))

    private fun startFm(seed: MediaItem?) {
        commandScope.launch {
            cancelQueueLoad()
            beginSession(fm = true)
            player.shuffleModeEnabled = false
            player.repeatMode = Player.REPEAT_MODE_ALL
            // Every FM entry point replaces the old queue, including the seedless path.
            player.setMediaItems(listOfNotNull(seed?.withQueueEntry(QueueSource(QueueSource.Type.RADIO))))
            player.prepare()
            player.playWhenReady = true
            queueChanged()
            checkAndLoadMetadata()
            requestFm()
        }
    }

    private fun requestFm() {
        val requestRevision = revision
        if (!isFmMode || fetchingFmRevision == requestRevision) return
        fetchingFmRevision = requestRevision
        sessionScope.launch {
            try {
                val items = fmRecommendations()
                // Also reject late responses from providers which do not cooperate with cancellation.
                if (requestRevision != revision || !isFmMode) return@launch
                val existingIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }.toSet()
                val additions = items.distinctBy { it.mediaId }.filterNot { it.mediaId in existingIds }
                    .map { it.withQueueEntry(QueueSource(QueueSource.Type.RADIO)) }
                if (additions.isNotEmpty()) {
                    player.addMediaItems(additions)
                    if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) player.prepare()
                    queueChanged()
                    checkAndLoadMetadata()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("QueueManager", "Unable to load FM recommendations", error)
            } finally {
                if (fetchingFmRevision == requestRevision) fetchingFmRevision = null
            }
        }
    }

    /** Removes this occurrence only; this does not submit a server-side dislike. */
    fun fmTrashCurrent() {
        commandScope.launch {
            if (!isFmMode) return@launch
            if (player.currentMediaItemIndex in 0 until player.mediaItemCount) {
                player.removeMediaItem(player.currentMediaItemIndex)
                queueChanged()
                requestFm()
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (mediaItem == null) return
        checkAndLoadMetadata()
        if (isFmMode) {
            if (player.mediaItemCount - player.currentMediaItemIndex <= 2) requestFm()
            val currentIndex = player.currentMediaItemIndex
            if (currentIndex > 5) player.removeMediaItems(0, currentIndex - 1)
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_POSITION_DISCONTINUITY)) {
            publishState()
        }
        if (events.containsAny(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED, Player.EVENT_REPEAT_MODE_CHANGED)) {
            checkAndLoadMetadata()
        }
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        commandScope.launch {
            if (isFmMode && enabled) return@launch
            player.shuffleModeEnabled = enabled
            queueChanged()
            checkAndLoadMetadata()
        }
    }

    fun switchPlayMode() {
        commandScope.launch {
            if (isFmMode) return@launch
            when {
                player.shuffleModeEnabled -> {
                    player.shuffleModeEnabled = false
                    player.repeatMode = Player.REPEAT_MODE_ONE
                }
                player.repeatMode == Player.REPEAT_MODE_ONE -> {
                    player.repeatMode = playbackRepeatMode(loopPlaybackEnabled())
                }
                else -> {
                    player.repeatMode = playbackRepeatMode(loopPlaybackEnabled())
                    player.shuffleModeEnabled = true
                }
            }
            queueChanged()
            checkAndLoadMetadata()
        }
    }

    private fun checkAndLoadMetadata(windowSize: Int = 3) {
        sessionScope.launch {
            val timeline = player.currentTimeline
            val current = player.currentMediaItemIndex
            if (timeline.isEmpty || current !in 0 until player.mediaItemCount) return@launch
            val indices = mutableSetOf(current)
            for (forward in listOf(true, false)) {
                var index = current
                repeat(windowSize) {
                    if (index != C.INDEX_UNSET) {
                        index = if (forward) timeline.getNextWindowIndex(index, Player.REPEAT_MODE_ALL, player.shuffleModeEnabled)
                            else timeline.getPreviousWindowIndex(index, Player.REPEAT_MODE_ALL, player.shuffleModeEnabled)
                        if (index != C.INDEX_UNSET) indices += index
                    }
                }
            }
            loadMetadataAtIndices(indices)
        }
    }

    /** IDs refer to displayed occurrences, so shuffle, moves and removals cannot change the target. */
    fun loadMetadataForEntries(entryIds: List<String>): Job = sessionScope.launch {
        val wanted = entryIds.toSet()
        loadMetadataAtIndices((0 until player.mediaItemCount).filter { player.queueEntryKey(it) in wanted })
    }

    private suspend fun loadMetadataAtIndices(indices: Iterable<Int>) {
        val requestRevision = revision
        val wanted = indices.filter { it in 0 until player.mediaItemCount }.mapNotNull { index ->
            val item = player.getMediaItemAt(index)
            item.queueTrackKey.takeIf { item.isQueuePlaceholder && loadingTracks.add(it) }
        }
        if (wanted.isEmpty()) return
        try {
            val resolved = metadataResolver.resolve(wanted)
            if (requestRevision != revision) return
            for (index in 0 until player.mediaItemCount) {
                val current = player.getMediaItemAt(index)
                val replacement = resolved[current.queueTrackKey]
                if (replacement != null && current.isQueuePlaceholder) {
                    val enriched = replacement.withQueueEntry(
                        current.queueSource, player.queueEntryKey(index), current.queueMediaKind,
                    )
                    // Media3 updates the existing source only if its URI and cache key stay the same.
                    player.replaceMediaItem(index, current.buildUpon()
                        .setMediaMetadata(enriched.mediaMetadata)
                        .setTag(enriched.localConfiguration?.tag)
                        .build())
                }
            }
        } finally {
            if (requestRevision == revision) loadingTracks.removeAll(wanted.toSet())
        }
    }

    fun addToQueue(items: List<MediaItem>, source: QueueSource? = null) {
        commandScope.launch {
            if (items.isEmpty()) return@launch
            player.addMediaItems(items.map { it.withQueueEntry(source ?: it.queueSource ?: QueueSource(QueueSource.Type.MANUAL)) })
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            queueChanged()
            checkAndLoadMetadata()
        }
    }

    fun playNext(items: List<MediaItem>, source: QueueSource? = null) {
        commandScope.launch {
            if (items.isEmpty()) return@launch
            val wasEmpty = player.mediaItemCount == 0
            val currentIndex = player.currentMediaItemIndex
            val insertIndex = if (wasEmpty) 0 else currentIndex + 1
            player.addMediaItems(insertIndex, items.map { it.withQueueEntry(source ?: it.queueSource ?: QueueSource(QueueSource.Type.MANUAL)) })
            if (player.shuffleModeEnabled && !wasEmpty) {
                player.applyQueueShuffleOrder(prioritizeInsertedShuffleItems(player.queueOrderIndices(), currentIndex, insertIndex, items.size))
            }
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            if (wasEmpty) player.play()
            queueChanged()
            checkAndLoadMetadata()
        }
    }

    /** These UI commands run on the player's application thread and resolve identity at execution. */
    @androidx.annotation.MainThread
    fun seekToEntry(entryId: String) {
        val index = indexOfEntry(entryId)
        if (index < 0) return
        player.seekToDefaultPosition(index)
        player.playWhenReady = true
        publishState()
    }

    @androidx.annotation.MainThread
    fun removeEntry(entryId: String): Boolean {
        val index = indexOfEntry(entryId)
        if (index < 0 || player.mediaItemCount <= 1) return false
        player.removeMediaItem(index)
        queueChanged()
        return true
    }

    @androidx.annotation.MainThread
    fun moveEntry(entryId: String, targetEntryId: String): Boolean {
        val from = indexOfEntry(entryId)
        val to = indexOfEntry(targetEntryId)
        if (from < 0 || to < 0 || from == to) return false
        if (player.shuffleModeEnabled) {
            val order = player.queueOrderIndices().toMutableList()
            val target = order.indexOf(to)
            order.add(target, order.removeAt(order.indexOf(from)))
            player.applyQueueShuffleOrder(order)
        } else player.moveMediaItem(from, to)
        queueChanged()
        checkAndLoadMetadata()
        return true
    }

    private fun indexOfEntry(entryId: String) =
        (0 until player.mediaItemCount).firstOrNull { player.queueEntryKey(it) == entryId } ?: -1

    private fun queueChanged() {
        publishState()
        onQueueChanged()
    }

    internal fun captureSnapshot(currentSong: MediaMetadata?): PlaybackQueueSnapshot? {
        val current = player.currentMediaItemIndex
        if (current !in 0 until player.mediaItemCount) return null
        val entries = (0 until player.mediaItemCount).map { index ->
            val item = player.getMediaItemAt(index)
            QueueEntrySnapshot(player.queueEntryKey(index), item.queueSource, item.queueMediaKind)
        }
        return PlaybackQueueSnapshot(
            ids = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId },
            currentIndex = current,
            positionMs = player.currentPosition.coerceAtLeast(0),
            currentSong = currentSong?.takeIf { it.id.toString() == player.currentMediaItem?.mediaId },
            isFmMode = isFmMode,
            entries = entries,
            shuffleEntryIds = player.queueOrderIndices(shuffled = true).map { entries[it].entryId },
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )
    }

    internal fun restore(snapshot: PlaybackQueueSnapshot, shuffleEnabled: Boolean, repeatMode: Int): Boolean {
        if (!canRestore) return false
        beginSession(snapshot.isFmMode)
        val items = snapshot.ids.mapIndexed { index, id ->
            val item = if (index == snapshot.currentIndex && snapshot.currentSong != null) snapshot.currentSong.toMediaItem()
                else createPlaceholder(id)
            val entry = snapshot.entries?.getOrNull(index)
            if (entry?.mediaKind != null) item.withQueueEntry(entry.source, entry.entryId, entry.mediaKind)
            else item.withQueueEntry(entry?.source, entry?.entryId ?: java.util.UUID.randomUUID().toString())
        }
        player.shuffleModeEnabled = false
        player.setMediaItems(items, snapshot.currentIndex, snapshot.positionMs)
        snapshot.shuffleEntryIds?.let { ids ->
            val indicesById = items.mapIndexed { index, item -> requireNotNull(item.queueEntryId) to index }.toMap()
            if (ids.size == items.size && ids.toSet() == indicesById.keys) player.applyQueueShuffleOrder(ids.map { indicesById.getValue(it) })
        }
        player.repeatMode = repeatMode
        player.shuffleModeEnabled = !isFmMode && shuffleEnabled
        player.playWhenReady = false
        player.prepare()
        queueChanged()
        checkAndLoadMetadata()
        return true
    }

    fun release() {
        cancelQueueLoad()
        revision++
        player.removeListener(this)
        commandScope.cancel()
        loadingTracks.clear()
    }
}

private val MediaItem.isQueuePlaceholder: Boolean
    get() = localConfiguration?.let { it.uri.toString() == PLACEHOLDER_URI && it.tag == null } == true
