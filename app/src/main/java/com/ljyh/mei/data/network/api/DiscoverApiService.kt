package com.ljyh.mei.data.network.api

import com.ljyh.mei.data.network.NeteaseHttp
import kotlinx.serialization.Serializable
import javax.inject.Inject

/** Paths and encryption checked against API Enhanced and anonymous live responses. */
class DiscoverApiService @Inject constructor(private val http: NeteaseHttp) {
    private val base = "https://music.163.com"

    suspend fun categories(): DiscoverCategoriesResponse =
        http.post(base, "/eapi/playlist/catalogue", emptyMap<String, String>(), cryptoMode = "eapi")

    suspend fun hotCategories(): DiscoverHotCategoriesResponse =
        http.post(base, "/weapi/playlist/hottags", emptyMap<String, String>(), cryptoMode = "weapi")

    suspend fun playlists(category: String, order: String, offset: Int, limit: Int): DiscoverPlaylistsResponse =
        http.post(base, "/weapi/playlist/list", mapOf(
            "cat" to category, "order" to order, "offset" to offset.toString(),
            "limit" to limit.toString(), "total" to "true",
        ), cryptoMode = "weapi")

    suspend fun albums(): DiscoverAlbumsResponse =
        http.post(base, "/weapi/discovery/newAlbum", emptyMap<String, String>(), cryptoMode = "weapi")

    suspend fun charts(): DiscoverChartsResponse =
        http.post(base, "/weapi/toplist/detail", emptyMap<String, String>(), cryptoMode = "weapi")
}

@Serializable
data class DiscoverCategoriesResponse(
    val code: Int = 0,
    val categories: Map<String, String> = emptyMap(),
    val sub: List<DiscoverCategoryDto> = emptyList(),
)

@Serializable
data class DiscoverCategoryDto(val name: String = "", val category: Int = -1)

@Serializable
data class DiscoverHotCategoriesResponse(val code: Int = 0, val tags: List<DiscoverTagDto> = emptyList())

@Serializable
data class DiscoverTagDto(val name: String = "")

@Serializable
data class DiscoverPlaylistsResponse(
    val code: Int = 0,
    val playlists: List<DiscoverPlaylistDto> = emptyList(),
    val more: Boolean = false,
)

@Serializable
data class DiscoverPlaylistDto(
    val id: Long = 0, val name: String = "", val coverImgUrl: String = "", val playCount: Long = 0,
)

@Serializable
data class DiscoverAlbumsResponse(val code: Int = 0, val albums: List<DiscoverAlbumDto> = emptyList())

@Serializable
data class DiscoverAlbumDto(
    val id: Long = 0, val name: String = "", val picUrl: String = "",
    val artists: List<DiscoverArtistDto> = emptyList(), val artist: DiscoverArtistDto? = null,
)

@Serializable
data class DiscoverArtistDto(val name: String = "")

@Serializable
data class DiscoverChartsResponse(val code: Int = 0, val list: List<DiscoverChartDto> = emptyList())

@Serializable
data class DiscoverChartDto(
    val id: Long = 0, val name: String = "", val coverImgUrl: String = "",
    val updateFrequency: String? = null, val tracks: List<DiscoverChartTrackDto>? = null,
)

@Serializable
data class DiscoverChartTrackDto(val first: String = "", val second: String = "")
