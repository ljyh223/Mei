package com.ljyh.mei.data.model.domain

data class DiscoverCategory(val name: String, val group: String)
data class DiscoverPlaylist(val id: Long, val name: String, val cover: String, val playCount: Long)
data class DiscoverPlaylistPage(val items: List<DiscoverPlaylist>, val nextOffset: Int, val hasMore: Boolean)
data class DiscoverAlbum(val id: Long, val name: String, val cover: String, val artist: String)
data class DiscoverChart(val id: Long, val name: String, val cover: String, val updateFrequency: String, val tracks: List<String>)

enum class DiscoverOrder(val wireValue: String, val label: String) {
    Hot("hot", "热门"), New("new", "最新"),
}

data class DiscoverPlaylistQuery(val category: String = "全部", val order: DiscoverOrder = DiscoverOrder.Hot)
