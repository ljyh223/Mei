package com.ljyh.mei.playback.queue

import android.util.Log
import androidx.media3.common.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class QueueTrackKey(val id: String, val kind: QueueMediaKind)
internal val MediaItem.queueTrackKey: QueueTrackKey get() = QueueTrackKey(mediaId, queueMediaKind)

/** Resolves each catalogue separately, including equal numeric IDs from different catalogues. */
class QueueMetadataResolver(
    private val catalogueDetails: suspend (List<String>) -> List<MediaItem>,
    private val localDetails: suspend (List<String>) -> List<MediaItem>,
    private val externalDetails: suspend (List<String>) -> List<MediaItem>,
) {
    internal suspend fun resolve(keys: Collection<QueueTrackKey>): Map<QueueTrackKey, MediaItem> =
        withContext(Dispatchers.IO) {
            buildMap {
                keys.distinct().groupBy { it.kind }.forEach { (kind, group) ->
                    val ids = group.map { it.id }
                    val items = try {
                        when (kind) {
                            QueueMediaKind.NETEASE -> catalogueDetails(ids)
                            QueueMediaKind.LOCAL -> localDetails(ids)
                            QueueMediaKind.MUSIC_FREE -> externalDetails(ids)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w("QueueMetadata", "Failed to load $kind metadata", error)
                        emptyList()
                    }
                    val requestedIds = ids.toSet()
                    items.filter { it.mediaId in requestedIds }.forEach { put(QueueTrackKey(it.mediaId, kind), it) }
                }
            }
        }
}
