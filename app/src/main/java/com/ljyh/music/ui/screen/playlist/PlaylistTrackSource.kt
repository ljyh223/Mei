package com.ljyh.music.ui.screen.playlist

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ljyh.music.data.model.PlaylistDetail
import com.ljyh.music.data.network.ApiService

class PlaylistTrackSource(private val apiService: ApiService, val id:String) : PagingSource<Int, PlaylistDetail.Playlist.Track>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PlaylistDetail.Playlist.Track> {
        val page = params.key ?: 1
        val pageSize = params.loadSize
        return try {
            val response = apiService.getPlaylistTracks(id, pageSize, pageSize * (page - 1))
            val data = response.songs

            LoadResult.Page(
                data = data,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (data.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
    override fun getRefreshKey(state: PagingState<Int, PlaylistDetail.Playlist.Track>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1) ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}