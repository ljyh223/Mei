package com.ljyh.mei.ui.screen.main.library

import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.ui.model.Album

enum class LibraryCollection(val title: String) {
    Created("我创建的歌单"), Collected("收藏歌单"), Albums("收藏专辑");
}

internal fun LibraryContentUiState.likedPlaylist(): Playlist? = createdPlaylists.firstOrNull {
    it.author == profile.userId && it.title.endsWith("喜欢的音乐")
}

/** The featured playlist remains part of Created, and is shown only once on the overview. */
internal fun LibraryContentUiState.createdPlaylistOverview(): List<Playlist> {
    val likedId = likedPlaylist()?.id
    return createdPlaylists.distinctBy { it.id }.filterNot { it.id == likedId }
}

internal data class LibraryDashboardSelection(
    val liked: Playlist?,
    val topCreated: Playlist?,
    val recentCollected: List<Playlist>,
    val remainingCreated: List<Playlist>,
)

internal fun LibraryContentUiState.dashboardSelection(): LibraryDashboardSelection {
    val created = createdPlaylistOverview()
    return LibraryDashboardSelection(
        liked = likedPlaylist(),
        topCreated = created.firstOrNull(),
        recentCollected = recentCollectedPlaylists.distinctBy { it.id }.take(2),
        remainingCreated = created.drop(1),
    )
}

/** Retain the service's collection order instead of applying the personal listening score. */
internal fun recentCollectedIds(
    playlists: List<com.ljyh.mei.data.model.domain.UserPlaylistItem>, userId: String,
): List<String> = playlists.asSequence().filter { it.creator.userId.toString() != userId }
    .map { it.id.toString() }.distinct().take(2).toList()

/** Only the first three tracks are needed for the shared liked-playlist artwork composition. */
internal fun likedArtworkTrackIds(detail: com.ljyh.mei.data.model.response.MiniPlaylistDetail): List<Long> =
    detail.trackIds.ifEmpty { detail.tracks.map { it.id } }.distinct().take(3)

/** Playlist track metadata may be partial or ordered differently from trackIds. */
internal fun firstLikedTrackId(detail: com.ljyh.mei.data.model.response.MiniPlaylistDetail): Long? =
    detail.trackIds.firstOrNull() ?: detail.tracks.firstOrNull()?.id

internal fun LibraryContentUiState.playlistsIn(collection: LibraryCollection): List<Playlist> = when (collection) {
    LibraryCollection.Created -> listOfNotNull(likedPlaylist()) + createdPlaylistOverview()
    LibraryCollection.Collected -> collectedPlaylists.distinctBy { it.id }
    LibraryCollection.Albums -> emptyList()
}

data class LibrarySearchMatches(val playlists: List<Playlist>, val albums: List<Album>)

internal fun searchLibrary(content: LibraryContentUiState?, query: String): LibrarySearchMatches {
    val terms = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (content == null || terms.isEmpty()) return LibrarySearchMatches(emptyList(), emptyList())
    fun matches(vararg fields: String): Boolean = terms.all { term -> fields.any { it.contains(term, ignoreCase = true) } }
    return LibrarySearchMatches(
        playlists = (content.createdPlaylists + content.collectedPlaylists).distinctBy { it.id }
            .filter { matches(it.title, it.authorName) },
        albums = content.albums.filter { matches(it.title, it.artist.joinToString(" ") { artist -> artist.name }) },
    )
}
