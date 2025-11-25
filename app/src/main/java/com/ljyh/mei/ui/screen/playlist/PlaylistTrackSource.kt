package com.ljyh.mei.ui.screen.playlist

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.PlaylistDetail
import com.ljyh.mei.data.model.api.GetSongDetails
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.data.network.api.ApiService

class PlaylistTrackSource(
    private val apiService: ApiService,
    private val firstData: List<PlaylistDetail.Playlist.Track>,
    private val ids: List<String>
) : PagingSource<Int, MediaMetadata>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaMetadata> {
        val pageSize = params.loadSize
        val offset = params.key ?: 0

        return try {
            val data: List<PlaylistDetail.Playlist.Track>

            if (offset < firstData.size) {
                // ★第一页，直接返回服务器提供的 tracks（数量不固定）
                data = firstData.subList(offset, firstData.size)
            } else {
                // ★后续页，从 trackIds 分页取 ID
                val end = minOf(offset + pageSize, ids.size)

                if (offset >= end) {
                    data = emptyList()
                } else {
                    val pageIds = ids.subList(offset, end).joinToString(",")
                    data = apiService.getSongDetail(GetSongDetails(pageIds)).songs
                }
            }

            val mapped = data.map { it.toMediaMetadata() }

            // ★下一页起点：offset + 当前取出的数量（不是固定 20）
            val nextKey = if (mapped.isEmpty() || offset + data.size >= ids.size) {
                null
            } else {
                offset + data.size
            }

            LoadResult.Page(
                data = mapped,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaMetadata>): Int? = null
}
