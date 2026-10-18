package com.ljyh.mei.ui.screen.artist

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.repository.ArtistRepository
import kotlinx.coroutines.CancellationException

internal class ArtistSongsPagingSource(
    private val repository: ArtistRepository,
    private val artistId: String,
) : PagingSource<Int, ArtistSong.HotSong>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ArtistSong.HotSong> {
        val offset = params.key ?: 0
        return try {
            when (val result = repository.getArtistSongsPage(artistId, offset, params.loadSize)) {
                is Resource.Success -> {
                    val page = result.data
                    if (page.code != 200) return LoadResult.Error(IllegalStateException("歌手歌曲接口返回 ${page.code}"))
                    LoadResult.Page(
                        data = page.songs,
                        prevKey = null,
                        nextKey = (offset + page.songs.size).takeIf { page.more && page.songs.isNotEmpty() },
                    )
                }
                is Resource.Error -> LoadResult.Error(IllegalStateException(result.message))
                Resource.Loading -> LoadResult.Error(IllegalStateException("歌手歌曲仍在加载"))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ArtistSong.HotSong>): Int? = null
}

internal class ArtistAlbumsPagingSource(
    private val repository: ArtistRepository,
    private val artistId: String,
) : PagingSource<Int, ArtistAlbum.HotAlbum>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ArtistAlbum.HotAlbum> {
        val offset = params.key ?: 0
        return try {
            when (val result = repository.getArtistAlbums(artistId, offset, params.loadSize)) {
                is Resource.Success -> {
                    val page = result.data
                    if (page.code != 200) return LoadResult.Error(IllegalStateException("歌手专辑接口返回 ${page.code}"))
                    LoadResult.Page(
                        data = page.hotAlbums,
                        prevKey = null,
                        nextKey = (offset + page.hotAlbums.size).takeIf { page.more && page.hotAlbums.isNotEmpty() },
                    )
                }
                is Resource.Error -> LoadResult.Error(IllegalStateException(result.message))
                Resource.Loading -> LoadResult.Error(IllegalStateException("歌手专辑仍在加载"))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ArtistAlbum.HotAlbum>): Int? = null
}
