package com.ljyh.mei.data.model.domain

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
    val artists: List<SearchArtist>,
    val publishTime: Long? = null,
)

data class SearchPlaylist(
    val id: Long,
    val title: String,
    val coverUrl: String,
    val creatorId: Long,
    val creatorName: String,
    val creatorAvatarUrl: String,
    val trackCount: Int,
    val playCount: Long? = null,
)

data class SearchResults(
    val songs: List<MediaMetadata> = emptyList(),
    val artists: List<SearchArtist> = emptyList(),
    val albums: List<SearchAlbum> = emptyList(),
    val playlists: List<SearchPlaylist> = emptyList(),
    val bestMatches: List<SearchMatch> = emptyList(),
    val recommendations: List<MediaMetadata> = emptyList(),
    val highlightWords: List<String> = emptyList(),
    val totalCount: Int? = null,
    val hasMore: Boolean = false,
    val nextOffset: Int = 0,
) {
    val isEmpty: Boolean get() = songs.isEmpty() && artists.isEmpty() && albums.isEmpty() &&
        playlists.isEmpty() && bestMatches.isEmpty() && recommendations.isEmpty()

    fun append(page: SearchResults) = copy(
        songs = (songs + page.songs).distinctBy { it.id },
        artists = (artists + page.artists).distinctBy { it.id },
        albums = (albums + page.albums).distinctBy { it.id },
        playlists = (playlists + page.playlists).distinctBy { it.id },
        totalCount = page.totalCount ?: totalCount,
        hasMore = page.hasMore,
        nextOffset = page.nextOffset,
    )
}

sealed interface SearchMatch {
    data class Artist(val artist: SearchArtist) : SearchMatch
    data class Album(val album: SearchAlbum) : SearchMatch
    data class Song(val song: MediaMetadata) : SearchMatch
    data class Playlist(val playlist: SearchPlaylist) : SearchMatch

    val key: String get() = when (this) {
        is Artist -> "artist-${artist.id}"
        is Album -> "album-${album.id}"
        is Song -> "song-${song.id}"
        is Playlist -> "playlist-${playlist.id}"
    }
}

data class SearchSuggestions(
    val songs: List<Suggestion>,
    val artists: List<Suggestion>,
    val albums: List<Suggestion>
) {
    data class Suggestion(val id: Long, val name: String)
}
