package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.domain.*
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.DiscoverApiService
import com.ljyh.mei.data.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface DiscoverDataSource {
    suspend fun categories(): Resource<List<DiscoverCategory>>
    suspend fun hotCategories(): Resource<List<String>>
    suspend fun playlists(query: DiscoverPlaylistQuery, offset: Int): Resource<DiscoverPlaylistPage>
    suspend fun albums(): Resource<List<DiscoverAlbum>>
    suspend fun charts(): Resource<List<DiscoverChart>>
}

class DiscoverRepository @Inject constructor(private val api: DiscoverApiService) : DiscoverDataSource {
    override suspend fun categories() = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.categories()
            check(response.code == 200) { "分类加载失败 (${response.code})" }
            response.sub.sortedBy { it.category }.mapNotNull { item ->
                val group = response.categories[item.category.toString()] ?: return@mapNotNull null
                item.name.takeIf(String::isNotBlank)?.let { DiscoverCategory(it, group) }
            }.distinctBy { it.name }
        }
    }

    override suspend fun hotCategories() = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.hotCategories()
            check(response.code == 200) { "热门分类加载失败 (${response.code})" }
            response.tags.map { it.name }.filter(String::isNotBlank).distinct()
        }
    }

    override suspend fun playlists(query: DiscoverPlaylistQuery, offset: Int) = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.playlists(query.category, query.order.wireValue, offset, 24)
            check(response.code == 200) { "歌单加载失败 (${response.code})" }
            val items = response.playlists.filter { it.id > 0 && it.name.isNotBlank() }.map {
                DiscoverPlaylist(it.id, it.name, it.coverImgUrl, it.playCount)
            }.distinctBy { it.id }
            DiscoverPlaylistPage(items, offset + response.playlists.size, response.more && items.isNotEmpty())
        }
    }

    override suspend fun albums() = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.albums()
            check(response.code == 200) { "新碟加载失败 (${response.code})" }
            response.albums.filter { it.id > 0 && it.name.isNotBlank() }.map {
                DiscoverAlbum(it.id, it.name, it.picUrl,
                    it.artists.map { artist -> artist.name }.filter(String::isNotBlank).joinToString(" / ")
                        .ifBlank { it.artist?.name.orEmpty() })
            }.distinctBy { it.id }
        }
    }

    override suspend fun charts() = withContext(Dispatchers.IO) {
        safeApiCall {
            val response = api.charts()
            check(response.code == 200) { "榜单加载失败 (${response.code})" }
            response.list.filter { it.id > 0 && it.name.isNotBlank() }.map {
                DiscoverChart(it.id, it.name, it.coverImgUrl, it.updateFrequency.orEmpty(),
                    it.tracks.orEmpty().take(3).map { song ->
                        listOf(song.first, song.second).filter(String::isNotBlank).joinToString(" · ")
                    }.filter(String::isNotBlank))
            }.distinctBy { it.id }
        }
    }
}
