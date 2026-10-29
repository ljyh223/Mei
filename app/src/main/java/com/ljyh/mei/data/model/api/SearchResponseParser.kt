package com.ljyh.mei.data.model.api

import com.ljyh.mei.data.model.domain.*
import com.ljyh.mei.utils.netease.NeteaseUtils.getResourceLink
import kotlinx.serialization.json.*

/** Maps the two search response shapes without retaining tracking or client navigation payloads. */
internal object SearchResponseParser {
    fun page(response: JsonObject, type: Int, offset: Int, limit: Int): SearchResults {
        checkCode(response)
        val result = requireNotNull(response.obj("result")) { "搜索响应缺少结果" }
        val (key, countKey) = when (type) {
            1 -> "songs" to "songCount"
            100 -> "artists" to "artistCount"
            10 -> "albums" to "albumCount"
            1000 -> "playlists" to "playlistCount"
            else -> error("不支持的搜索分类")
        }
        val rows = result.objects(key)
        val count = result.number(countKey)?.toInt()?.takeIf { it >= 0 }
        val next = offset + rows.size
        val more = (result["hasMore"] as? JsonPrimitive)?.booleanOrNull
            ?: (result["more"] as? JsonPrimitive)?.booleanOrNull
            ?: count?.let { next < it }
            ?: (rows.size >= limit)
        return SearchResults(
            songs = if (type == 1) rows.mapNotNull(::song).distinctBy { it.id } else emptyList(),
            artists = if (type == 100) rows.mapNotNull(::artist).distinctBy { it.id } else emptyList(),
            albums = if (type == 10) rows.mapNotNull(::album).distinctBy { it.id } else emptyList(),
            playlists = if (type == 1000) rows.mapNotNull(::playlist).distinctBy { it.id } else emptyList(),
            totalCount = count,
            hasMore = rows.isNotEmpty() && more,
            nextOffset = next,
        )
    }

    fun overview(response: JsonObject): SearchResults {
        checkCode(response)
        val data = requireNotNull(response.obj("data")) { "搜索响应缺少结果" }
        require(data["blocks"] is JsonArray) { "搜索响应缺少内容分区" }
        val songs = mutableListOf<MediaMetadata>()
        val artists = mutableListOf<SearchArtist>()
        val albums = mutableListOf<SearchAlbum>()
        val playlists = mutableListOf<SearchPlaylist>()
        val recommendations = mutableListOf<MediaMetadata>()
        val matches = mutableListOf<SearchMatch>()
        val highlights = mutableListOf<String>()
        for (block in data.objects("blocks")) {
            highlights += block.strings("hlWords")
            for (resource in block.objects("resources")) {
                val base = resource.obj("baseInfo") ?: continue
                val match = when (resource.text("resourceType")) {
                    "song" -> base.obj("simpleSongData")?.let(::song)?.let(SearchMatch::Song)
                    "artist" -> base.obj("artistDTO")?.let(::artist)?.let(SearchMatch::Artist)
                    "album" -> base.obj("albumData")?.let(::album)?.let(SearchMatch::Album)
                    "playlist" -> base.obj("pubPlaylistData")?.let(::playlist)?.let(SearchMatch::Playlist)
                    else -> null
                } ?: continue
                when (block.text("blockCode")) {
                    "search_block_best_match" -> matches += match
                    "search_block_song" -> if (match is SearchMatch.Song) songs += match.song
                    "search_block_artist" -> if (match is SearchMatch.Artist) artists += match.artist
                    "search_block_album" -> if (match is SearchMatch.Album) albums += match.album
                    "search_block_playlist" -> if (match is SearchMatch.Playlist) playlists += match.playlist
                    "search_block_song_rec" -> if (match is SearchMatch.Song) recommendations += match.song
                }
            }
        }
        return SearchResults(
            songs = songs.distinctBy { it.id }, artists = artists.distinctBy { it.id },
            albums = albums.distinctBy { it.id }, playlists = playlists.distinctBy { it.id },
            bestMatches = matches.distinctBy { it.key },
            recommendations = recommendations.distinctBy { it.id }.filter { rec -> songs.none { it.id == rec.id } },
            highlightWords = highlights.filter(String::isNotBlank).distinct(),
        )
    }

    private fun checkCode(response: JsonObject) {
        check(response.number("code") == 200L) { "搜索暂不可用，请稍后重试" }
    }

    private fun song(value: JsonObject): MediaMetadata? {
        val id = value.number("id")?.takeIf { it > 0 } ?: return null
        val title = value.text("name").takeIf(String::isNotBlank) ?: return null
        val record = value.obj("al") ?: value.obj("album") ?: JsonObject(emptyMap())
        val performers = value.objects("ar").ifEmpty { value.objects("artists") }
        return MediaMetadata(
            id = id, title = title, coverUrl = record.cover(),
            artists = performers.map { MediaMetadata.Artist(
                id = it.number("id") ?: 0, name = it.text("name"),
                picUrl = it.text("picUrl").ifBlank { null }, alias = it.strings("alias"),
            ) },
            duration = value.number("dt") ?: value.number("duration") ?: 0,
            album = MediaMetadata.Album(record.number("id") ?: 0, record.text("name")),
            tns = (value.strings("tns") + value.strings("transNames") + value.strings("alia")).firstOrNull(),
        )
    }

    private fun artist(value: JsonObject): SearchArtist? {
        val id = value.number("id")?.takeIf { it > 0 } ?: return null
        val name = value.text("name").takeIf(String::isNotBlank) ?: return null
        return SearchArtist(id, name, value.text("picUrl").ifBlank { value.text("img1v1Url") }.ifBlank { null },
            (value.strings("alias") + value.strings("transNames")).distinct())
    }

    private fun album(value: JsonObject): SearchAlbum? {
        val id = value.number("id")?.takeIf { it > 0 } ?: return null
        val title = value.text("name").takeIf(String::isNotBlank) ?: return null
        val performers = value.objects("artists").ifEmpty { listOfNotNull(value.obj("artist")) }
        return SearchAlbum(id, title, value.cover(), value.number("size")?.toInt() ?: 0,
            performers.mapNotNull(::artist), value.number("publishTime")?.takeIf { it > 0 })
    }

    private fun playlist(value: JsonObject): SearchPlaylist? {
        val id = value.number("id")?.takeIf { it > 0 } ?: return null
        val title = value.text("name").takeIf(String::isNotBlank) ?: return null
        val creator = value.obj("creator") ?: JsonObject(emptyMap())
        return SearchPlaylist(id, title, value.text("coverImgUrl"), creator.number("userId") ?: 0,
            creator.text("nickname"), creator.text("avatarUrl"), value.number("trackCount")?.toInt() ?: 0,
            value.number("playCount"))
    }

    private fun JsonObject.cover(): String = text("picUrl").ifBlank {
        (number("picId") ?: number("pic"))?.takeIf { it > 0 }?.let { getResourceLink(it.toString(), "jpg") }.orEmpty()
    }
    private fun JsonObject.obj(key: String) = this[key] as? JsonObject
    private fun JsonObject.objects(key: String) = (this[key] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
    private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
    private fun JsonObject.number(key: String) = (this[key] as? JsonPrimitive)?.longOrNull
    private fun JsonObject.strings(key: String) = (this[key] as? JsonArray).orEmpty()
        .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
}
