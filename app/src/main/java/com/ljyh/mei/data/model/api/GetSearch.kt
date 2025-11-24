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
        val songs: List<Song>?,
        @SerializedName("artists")
        val artists: List<Artist>?,
        @SerializedName("playlists")
        val playlists: List<Playlist>?,
        @SerializedName("albums")
        val albums: List<Album>?
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
            @SerializedName("transNames")
            val transNames: List<String>?,
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
            val status: Int
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
                val alia: List<String>
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


        data class Artist(
            @SerializedName("id")
            val id: Long,
            @SerializedName("name")
            val name: String,
            @SerializedName("picUrl")
            val picUrl: String,
            @SerializedName("alias")
            val alias: List<String>,
            @SerializedName("albumSize")
            val albumSize: Int,
            @SerializedName("picId")
            val picId: Long,
            @SerializedName("fansGroup")
            val fansGroup: Any,
            @SerializedName("img1v1Url")
            val img1v1Url: String,
            @SerializedName("img1v1")
            val img1v1: Long,
            @SerializedName("mvSize")
            val mvSize: Int,
            @SerializedName("followed")
            val followed: Boolean,
            @SerializedName("alia")
            val alia: List<String>,
            @SerializedName("trans")
            val trans: String,
            @SerializedName("accountId")
            val accountId: Long,
            @SerializedName("transNames")
            val transNames: List<String>
        )


        data class Album(
            @SerializedName("name")
            val name: String,
            @SerializedName("id")
            val id: Long,
            @SerializedName("idStr")
            val idStr: String,
            @SerializedName("type")
            val type: String,
            @SerializedName("size")
            val size: Int,
            @SerializedName("picId")
            val picId: Long,
            @SerializedName("blurPicUrl")
            val blurPicUrl: String,
            @SerializedName("companyId")
            val companyId: Int,
            @SerializedName("pic")
            val pic: Long,
            @SerializedName("picUrl")
            val picUrl: String,
            @SerializedName("publishTime")
            val publishTime: Long,
            @SerializedName("description")
            val description: String,
            @SerializedName("tags")
            val tags: String,
            @SerializedName("company")
            val company: String,
            @SerializedName("briefDesc")
            val briefDesc: String,
            @SerializedName("artist")
            val artist: Artist,
            @SerializedName("songs")
            val songs: List<Any>,
            @SerializedName("alias")
            val alias: List<String>,
            @SerializedName("status")
            val status: Int,
            @SerializedName("copyrightId")
            val copyrightId: Int,
            @SerializedName("commentThreadId")
            val commentThreadId: String,
            @SerializedName("artists")
            val artists: List<Artist>,
            @SerializedName("onSale")
            val onSale: Boolean,
            @SerializedName("picId_str")
            val picIdStr: String,
            @SerializedName("isSub")
            val isSub: Boolean,
            @SerializedName("transNames")
            val transNames: List<String>
        ) {

            data class Artist(
                @SerializedName("name")
                val name: String,
                @SerializedName("id")
                val id: Int,
                @SerializedName("picId")
                val picId: Int,
                @SerializedName("img1v1Id")
                val img1v1Id: Int,
                @SerializedName("briefDesc")
                val briefDesc: String,
                @SerializedName("picUrl")
                val picUrl: String,
                @SerializedName("img1v1Url")
                val img1v1Url: String,
                @SerializedName("albumSize")
                val albumSize: Int,
                @SerializedName("alias")
                val alias: List<Any>,
                @SerializedName("trans")
                val trans: String,
                @SerializedName("musicSize")
                val musicSize: Int,
                @SerializedName("topicPerson")
                val topicPerson: Int
            )
        }


        data class Playlist(
            @SerializedName("id")
            val id: Long,
            @SerializedName("name")
            val name: String,
            @SerializedName("coverImgUrl")
            val coverImgUrl: String,
            @SerializedName("creator")
            val creator: Creator,
            @SerializedName("subscribed")
            val subscribed: Boolean,
            @SerializedName("trackCount")
            val trackCount: Int,
            @SerializedName("userId")
            val userId: Long,
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
        ) {
            data class Creator(
                @SerializedName("nickname")
                val nickname: String,
                @SerializedName("userId")
                val userId: Long,
                @SerializedName("userType")
                val userType: Int,
                @SerializedName("avatarUrl")
                val avatarUrl: Any,
                @SerializedName("authStatus")
                val authStatus: Int,
                @SerializedName("expertTags")
                val expertTags: Any,
                @SerializedName("experts")
                val experts: Any
            )
        }
    }

    data class Trp(
        @SerializedName("rules")
        val rules: List<String>
    )
}
