package com.ljyh.music.ui.screen.playlist

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.data.model.api.GetSongDetails
import com.ljyh.music.data.network.api.ApiService

class PlaylistTrackSource(
    private val apiService: ApiService,
    // 这是第一次加载的data
    private val firstData: List<PlaylistDetail.Playlist.Track>,
    private val ids: List<String>
) : PagingSource<Int, PlaylistDetail.Playlist.Track>() {

    // 服务器实际分页参数（根据最新请求日志）
    private companion object {
        const val INITIAL_OFFSET = 0  // 第一页之后的分页起点
        const val PAGE_SIZE = 20       // 实际观察到的分页大小
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PlaylistDetail.Playlist.Track> {
        val currentKey = params.key ?: INITIAL_OFFSET  // 直接从offset开始
        val offset = currentKey


        return try {
            val data = if (offset == 0) {
                firstData
            } else {
                // 如果不是第一次加载，那么就取ids后20个的id，去请求数据
                apiService.getSongDetail(GetSongDetails(ids.subList(offset, offset + PAGE_SIZE).joinToString(","))).songs
            }


            LoadResult.Page(
                data = data,
                prevKey = if (offset == INITIAL_OFFSET) null else offset - PAGE_SIZE,
                nextKey = if (data.isEmpty()) null else offset + PAGE_SIZE
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PlaylistDetail.Playlist.Track>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(PAGE_SIZE)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(PAGE_SIZE)
        }
    }
}