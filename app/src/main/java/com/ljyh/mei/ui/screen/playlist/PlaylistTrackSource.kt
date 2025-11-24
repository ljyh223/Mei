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
) : PagingSource<Int, MediaMetadata>() { // 注意：直接返回 MediaMetadata

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaMetadata> {
        // key 如果为 null，说明是第一次加载，但在你的逻辑里第一次数据已经在 firstData 里了
        // 这里我们要小心处理 offset。你的逻辑是 offset 代表列表索引
        val offset = params.key ?: 0

        return try {
            val songs: List<PlaylistDetail.Playlist.Track> = if (offset == 0) {
                firstData
            } else {
                // 【修复】：防止数组越界
                val end = minOf(offset + 20, ids.size)
                if (offset >= end) {
                    emptyList()
                } else {
                    // 取出这一页需要的 IDs
                    val pageIds = ids.subList(offset, end).joinToString(",")
                    // 网络请求
                    apiService.getSongDetail(GetSongDetails(pageIds)).songs
                }
            }

            // 【转换】：在这里直接转成 UI 需要的 MediaMetadata
            val mediaMetadataList = songs.map { it.toMediaMetadata() }

            // 计算下一个 key
            // 如果取回的数据为空，或者当前 offset + 20 已经超过了 ids 总数，就没有下一页了
            val nextKey = if (mediaMetadataList.isEmpty() || (offset + 20) >= ids.size) {
                null
            } else {
                offset + 20
            }

            LoadResult.Page(
                data = mediaMetadataList,
                prevKey = null, // 我们只向下滑动，不需要处理向上的 prevKey
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaMetadata>): Int? = null
}