package com.ljyh.mei.ui.screen.playlist

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.repository.PlaylistRepository

class PlaylistTrackSource(
    private val repository: PlaylistRepository,
    private val firstData: List<MediaMetadata>,
    private val ids: List<String>
) : PagingSource<Int, MediaMetadata>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaMetadata> {
        val pageSize = params.loadSize
        val offset = params.key ?: 0

        return try {
            val end = minOf(offset + pageSize, ids.size)
            val pageIds = ids.subList(offset.coerceAtMost(end), end)
            val tracksById = firstData.associateBy { it.id.toString() }.toMutableMap()
            val missingIds = pageIds.filterNot(tracksById::containsKey)
            if (missingIds.isNotEmpty()) {
                repository.getSongDetails(missingIds).forEach { track ->
                    tracksById[track.id.toString()] = track
                }
            }
            val data = pageIds.mapNotNull(tracksById::get)
            val nextKey = end.takeIf { it < ids.size }

            LoadResult.Page(
                data = data,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaMetadata>): Int? = null
}
