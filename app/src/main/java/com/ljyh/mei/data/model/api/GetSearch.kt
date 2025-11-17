package com.ljyh.mei.data.model.api

import com.google.gson.annotations.SerializedName

data class GetSearch(
    @SerializedName("s")
    val s: String,
    @SerializedName("type")
    val type: Int = 1,
    @SerializedName("limit")
    val limit: Int = 30,
    @SerializedName("offset")
    val offset: Int = 0

)

data class GetSearchSuggest(
    @SerializedName("s")
    val s: String,
    @SerializedName("type")
    val type: String = "web"
)

data class SearchResult(
    @SerializedName("result")
    val result: Result,
    @SerializedName("code")
    val code: Int,
    @SerializedName("trp")
    val trp: Trp
) {
    data class Result(
        @SerializedName("songs")
        val songs: List<Song>,
        @SerializedName("hasMore")
        val hasMore: Boolean,
        @SerializedName("songCount")
        val songCount: Int
    ) {
        data class Song(
            @SerializedName("album")
            val album: Album,
            @SerializedName("fee")
            val fee: Int,
            @SerializedName("duration")
            val duration: Int,
            @SerializedName("rtype")
            val rtype: Int,
            @SerializedName("ftype")
            val ftype: Int,
            @SerializedName("artists")
            val artists: List<Artist>,
            @SerializedName("copyrightId")
            val copyrightId: Int,
            @SerializedName("mvid")
            val mvid: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("alias")
            val alias: List<String>,
            @SerializedName("id")
            val id: Long,
            @SerializedName("mark")
            val mark: Long,
            @SerializedName("status")
            val status: Int,
            @SerializedName("transNames")
            val transNames: List<String>
        ) {
            data class Album(
                @SerializedName("publishTime")
                val publishTime: Long,
                @SerializedName("size")
                val size: Int,
                @SerializedName("artist")
                val artist: Artist,
                @SerializedName("copyrightId")
                val copyrightId: Int,
                @SerializedName("name")
                val name: String,
                @SerializedName("id")
                val id: Int,
                @SerializedName("picId")
                val picId: Long,
                @SerializedName("mark")
                val mark: Int,
                @SerializedName("status")
                val status: Int,
                @SerializedName("alia")
                val alia: List<String>,
                @SerializedName("transNames")
                val transNames: List<String>
            ) {
                data class Artist(
                    @SerializedName("img1v1Url")
                    val img1v1Url: String,
                    @SerializedName("img1v1")
                    val img1v1: Int,
                    @SerializedName("name")
                    val name: String,
                    @SerializedName("alias")
                    val alias: List<Any>,
                    @SerializedName("id")
                    val id: Int,
                    @SerializedName("albumSize")
                    val albumSize: Int,
                    @SerializedName("picId")
                    val picId: Int
                )
            }

            data class Artist(
                @SerializedName("img1v1Url")
                val img1v1Url: String,
                @SerializedName("img1v1")
                val img1v1: Int,
                @SerializedName("name")
                val name: String,
                @SerializedName("alias")
                val alias: List<Any>,
                @SerializedName("id")
                val id: Int,
                @SerializedName("albumSize")
                val albumSize: Int,
                @SerializedName("picId")
                val picId: Int
            )
        }
    }

    data class Trp(
        @SerializedName("rules")
        val rules: List<String>
    )
}

data class SuggestResult(
    @SerializedName("result")
    val result: Result,
    @SerializedName("code")
    val code: Int
) {
    data class Result(
        @SerializedName("albums")
        val albums: List<Album>? = null,
        @SerializedName("artists")
        val artists: List<Artist>? = null,
        @SerializedName("songs")
        val songs: List<Song>? = null,
        @SerializedName("playlists")
        val playlists: List<Playlists>? = null,
        @SerializedName("order")
        val order: List<String>
    ) {
        data class Album(
            @SerializedName("id")
            val id: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("artist")
            val artist: Artist,
            @SerializedName("publishTime")
            val publishTime: Long,
            @SerializedName("size")
            val size: Int,
            @SerializedName("copyrightId")
            val copyrightId: Int,
            @SerializedName("status")
            val status: Int,
            @SerializedName("picId")
            val picId: Long,
            @SerializedName("mark")
            val mark: Int
        ) {
            data class Artist(
                @SerializedName("id")
                val id: Int,
                @SerializedName("name")
                val name: String,
                @SerializedName("picUrl")
                val picUrl: String,
                @SerializedName("alias")
                val alias: List<Any>,
                @SerializedName("albumSize")
                val albumSize: Int,
                @SerializedName("picId")
                val picId: Long,
                @SerializedName("fansGroup")
                val fansGroup: Any,
                @SerializedName("img1v1Url")
                val img1v1Url: String,
                @SerializedName("img1v1")
                val img1v1: Int,
                @SerializedName("trans")
                val trans: Any
            )
        }

        data class Artist(
            @SerializedName("id")
            val id: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("picUrl")
            val picUrl: String,
            @SerializedName("alias")
            val alias: List<Any>,
            @SerializedName("albumSize")
            val albumSize: Int,
            @SerializedName("picId")
            val picId: Long,
            @SerializedName("fansGroup")
            val fansGroup: Any,
            @SerializedName("img1v1Url")
            val img1v1Url: String,
            @SerializedName("accountId")
            val accountId: Int,
            @SerializedName("img1v1")
            val img1v1: Long,
            @SerializedName("trans")
            val trans: Any
        )

        data class Song(
            @SerializedName("id")
            val id: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("artists")
            val artists: List<Artist>,
            @SerializedName("album")
            val album: Album,
            @SerializedName("duration")
            val duration: Int,
            @SerializedName("copyrightId")
            val copyrightId: Int,
            @SerializedName("status")
            val status: Int,
            @SerializedName("alias")
            val alias: List<Any>,
            @SerializedName("rtype")
            val rtype: Int,
            @SerializedName("ftype")
            val ftype: Int,
            @SerializedName("mvid")
            val mvid: Int,
            @SerializedName("fee")
            val fee: Int,
            @SerializedName("rUrl")
            val rUrl: Any,
            @SerializedName("mark")
            val mark: Long,
            @SerializedName("transNames")
            val transNames: List<String>
        ) {
            data class Artist(
                @SerializedName("id")
                val id: Int,
                @SerializedName("name")
                val name: String,
                @SerializedName("picUrl")
                val picUrl: Any,
                @SerializedName("alias")
                val alias: List<Any>,
                @SerializedName("albumSize")
                val albumSize: Int,
                @SerializedName("picId")
                val picId: Int,
                @SerializedName("fansGroup")
                val fansGroup: Any,
                @SerializedName("img1v1Url")
                val img1v1Url: String,
                @SerializedName("img1v1")
                val img1v1: Int,
                @SerializedName("trans")
                val trans: Any
            )

            data class Album(
                @SerializedName("id")
                val id: Int,
                @SerializedName("name")
                val name: String,
                @SerializedName("artist")
                val artist: Artist,
                @SerializedName("publishTime")
                val publishTime: Long,
                @SerializedName("size")
                val size: Int,
                @SerializedName("copyrightId")
                val copyrightId: Int,
                @SerializedName("status")
                val status: Int,
                @SerializedName("picId")
                val picId: Long,
                @SerializedName("mark")
                val mark: Int,
                @SerializedName("transNames")
                val transNames: List<String>
            ) {
                data class Artist(
                    @SerializedName("id")
                    val id: Int,
                    @SerializedName("name")
                    val name: String,
                    @SerializedName("picUrl")
                    val picUrl: Any,
                    @SerializedName("alias")
                    val alias: List<Any>,
                    @SerializedName("albumSize")
                    val albumSize: Int,
                    @SerializedName("picId")
                    val picId: Int,
                    @SerializedName("fansGroup")
                    val fansGroup: Any,
                    @SerializedName("img1v1Url")
                    val img1v1Url: String,
                    @SerializedName("img1v1")
                    val img1v1: Int,
                    @SerializedName("trans")
                    val trans: Any
                )
            }
        }

        data class Playlists(
            @SerializedName("id")
            val id: Long,
            @SerializedName("name")
            val name: String,
            @SerializedName("coverImgUrl")
            val coverImgUrl: String,
            @SerializedName("creator")
            val creator: Any,
            @SerializedName("subscribed")
            val subscribed: Boolean,
            @SerializedName("trackCount")
            val trackCount: Int,
            @SerializedName("userId")
            val userId: Int,
            @SerializedName("playCount")
            val playCount: Int,
            @SerializedName("bookCount")
            val bookCount: Int,
            @SerializedName("specialType")
            val specialType: Int,
            @SerializedName("officialTags")
            val officialTags: Any,
            @SerializedName("action")
            val action: Any,
            @SerializedName("actionType")
            val actionType: Any,
            @SerializedName("recommendText")
            val recommendText: Any,
            @SerializedName("score")
            val score: Any,
            @SerializedName("officialPlaylistTitle")
            val officialPlaylistTitle: Any,
            @SerializedName("playlistType")
            val playlistType: String,
            @SerializedName("description")
            val description: String,
            @SerializedName("highQuality")
            val highQuality: Boolean
        )
    }
}