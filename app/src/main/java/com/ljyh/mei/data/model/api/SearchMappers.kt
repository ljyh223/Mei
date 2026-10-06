package com.ljyh.mei.data.model.api

import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.SearchAlbum
import com.ljyh.mei.data.model.SearchArtist
import com.ljyh.mei.data.model.SearchPlaylist
import com.ljyh.mei.data.model.SearchResults
import com.ljyh.mei.data.model.SearchSuggestions
import com.ljyh.mei.utils.netease.NeteaseUtils.getResourceLink

internal fun SearchResult.toDomain(): SearchResults = SearchResults(
    songs = result.songs.orEmpty().map { it.toMediaMetadata() },
    artists = result.artists.orEmpty().map { it.toDomain() },
    albums = result.albums.orEmpty().map { it.toDomain() },
    playlists = result.playlists.orEmpty().map { it.toDomain() }
)

internal fun SearchSuggest.toDomain(): SearchSuggestions = SearchSuggestions(
    songs = result.songs.orEmpty().map { SearchSuggestions.Suggestion(it.id, it.name) },
    artists = result.artists.orEmpty().map { SearchSuggestions.Suggestion(it.id.toLong(), it.name) },
    albums = result.albums.orEmpty().map { SearchSuggestions.Suggestion(it.id.toLong(), it.name) }
)

private fun SearchResult.Result.Song.toMediaMetadata(): MediaMetadata = MediaMetadata(
    id = id,
    title = name,
    coverUrl = getResourceLink(album.picId.toString(), "jpg"),
    artists = artists.map {
        MediaMetadata.Artist(
            name = it.name,
            id = it.id,
            picUrl = getResourceLink(it.picId.toString(), "jpg"),
            alias = it.alias
        )
    },
    duration = duration,
    album = MediaMetadata.Album(id = album.id, title = album.name)
)

private fun SearchResult.Result.Artist.toDomain() = SearchArtist(
    id = id,
    name = name,
    imageUrl = picUrl,
    aliases = alias
)

private fun SearchResult.Result.Album.toDomain() = SearchAlbum(
    id = id,
    title = name,
    coverUrl = picUrl,
    size = size,
    artists = artists.map { it.toDomain() }
)

private fun SearchResult.Result.Playlist.toDomain() = SearchPlaylist(
    id = id,
    title = name,
    coverUrl = coverImgUrl,
    creatorId = creator.userId,
    creatorName = creator.nickname,
    creatorAvatarUrl = creator.avatarUrl,
    trackCount = trackCount
)
