package com.ljyh.mei.data.model

data class SearchArtist(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val aliases: List<String>
)

data class SearchAlbum(
    val id: Long,
    val title: String,
    val coverUrl: String,
    val size: Int,
    val artists: List<SearchArtist>
)

data class SearchPlaylist(
    val id: Long,
    val title: String,
    val coverUrl: String,
    val creatorId: Long,
    val creatorName: String,
    val creatorAvatarUrl: String,
    val trackCount: Int
)

data class SearchResults(
    val songs: List<MediaMetadata>,
    val artists: List<SearchArtist>,
    val albums: List<SearchAlbum>,
    val playlists: List<SearchPlaylist>
)

data class SearchSuggestions(
    val songs: List<Suggestion>,
    val artists: List<Suggestion>,
    val albums: List<Suggestion>
) {
    data class Suggestion(val id: Long, val name: String)
}
