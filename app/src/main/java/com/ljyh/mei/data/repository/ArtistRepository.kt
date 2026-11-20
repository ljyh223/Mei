package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.api.ArtistAlbum
import com.ljyh.mei.data.model.api.ArtistDetail
import com.ljyh.mei.data.model.api.ArtistSong
import com.ljyh.mei.data.model.api.ArtistSongsPage
import com.ljyh.mei.data.model.api.GetArtistAlbum
import com.ljyh.mei.data.model.api.GetArtistDetail
import com.ljyh.mei.data.model.api.GetArtistSong
import com.ljyh.mei.data.model.api.GetArtistSongsPage
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.safeApiCall
import com.ljyh.mei.data.network.api.WeApiService
import kotlinx.serialization.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArtistRepository(private val apiService: ApiService, private val weApiService: WeApiService) {
    
    suspend fun isSubscribed(id: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        safeApiCall {
            require(id.toLongOrNull()?.let { it > 0 } == true)
            var offset = 0
            val seen = mutableSetOf<Long>()
            while (true) {
                val page = weApiService.getSubscribedArtists(offset)
                check((page["code"] as? JsonPrimitive)?.intOrNull == 200) { "无法获取收藏状态" }
                val artists = page["data"] as? JsonArray ?: error("收藏列表缺失")
                val ids = artists.mapNotNull { ((it as? JsonObject)?.get("id") as? JsonPrimitive)?.longOrNull }
                check(ids.size == artists.size && ids.all { it > 0 }) { "收藏列表格式无效" }
                if (id.toLong() in ids) return@safeApiCall true
                val more = (page["hasMore"] as? JsonPrimitive)?.booleanOrNull
                    ?: (page["more"] as? JsonPrimitive)?.booleanOrNull
                val count = (page["count"] as? JsonPrimitive)?.intOrNull
                if (more == false || (more == null && count != null && offset + artists.size >= count)) return@safeApiCall false
                if (artists.isEmpty()) {
                    check(more != true) { "收藏列表分页不完整" }
                    return@safeApiCall false
                }
                check(more != null || count != null) { "收藏列表缺少分页信息" }
                check(ids.any { it !in seen }) { "收藏列表分页重复" }
                seen.addAll(ids)
                offset += artists.size
                check(offset <= 10_000) { "收藏列表过大，请稍后重试" }
            }
            @Suppress("UNREACHABLE_CODE") false
        }
    }

    suspend fun setSubscribed(id: String, subscribed: Boolean): Resource<Unit> = withContext(Dispatchers.IO) {
        safeApiCall {
            require(id.toLongOrNull()?.let { it > 0 } == true)
            val result = weApiService.setArtistSubscribed(id, subscribed)
            check(result.code == 200) { "歌手收藏操作失败" }
        }
    }

    suspend fun getArtistDetail(id: String): Resource<ArtistDetail> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getArtistDetail(GetArtistDetail(id = id))
            }
        }
    }

    suspend fun getArtistAlbums(id: String, offset: Int = 0, limit: Int = 10): Resource<ArtistAlbum> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getArtistAlbums(GetArtistAlbum(limit = limit, offset = offset), id)
            }
        }
    }

    suspend fun getArtistSongsPage(id: String, offset: Int, limit: Int): Resource<ArtistSongsPage> =
        withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getArtistSongsPage(GetArtistSongsPage(id = id, offset = offset, limit = limit))
            }
        }

    suspend fun getArtistSongs(id: String): Resource<ArtistSong> {
        return withContext(Dispatchers.IO) {
            safeApiCall {
                apiService.getArtistSongs(GetArtistSong(), id)
            }
        }
    }
}
