package com.ljyh.mei.ui.screen.artist

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.domain.ArtistReleaseCategory
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
    private val category: ArtistReleaseCategory = ArtistReleaseCategory.All,
) : PagingSource<Int, ArtistAlbum.HotAlbum>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ArtistAlbum.HotAlbum> {
        var offset = params.key ?: 0
        val skippedIds = mutableSetOf<Long>()
        return try {
            while (true) {
                when (val result = repository.getArtistAlbums(artistId, offset, params.loadSize)) {
                    is Resource.Success -> {
                        val page = result.data
                        if (page.code != 200) return LoadResult.Error(IllegalStateException("歌手专辑接口返回 ${page.code}"))
                        val matching = page.hotAlbums.filter { category.matches(it.type) }
                        val next = (offset + page.hotAlbums.size).takeIf { page.more && page.hotAlbums.isNotEmpty() }
                        if (matching.isNotEmpty() || next == null) return LoadResult.Page(
                            data = matching, prevKey = null, nextKey = next,
                        )
                        // An empty first filtered page must not leave the presenter with nothing to request.
                        if (page.hotAlbums.none { it.id !in skippedIds }) {
                            return LoadResult.Error(IllegalStateException("发行作品分页重复，请重试"))
                        }
                        skippedIds += page.hotAlbums.map { it.id }
                        offset = next
                    }
                    is Resource.Error -> return LoadResult.Error(IllegalStateException(result.message))
                    Resource.Loading -> return LoadResult.Error(IllegalStateException("歌手专辑仍在加载"))
                }
            }
            @Suppress("UNREACHABLE_CODE") LoadResult.Error(IllegalStateException("发行作品暂未加载"))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ArtistAlbum.HotAlbum>): Int? = null
}
