package com.ljyh.mei.data.model

import com.google.gson.annotations.SerializedName

data class AlbumDetail(
    @SerializedName("resourceState")
    val resourceState: Boolean,
    @SerializedName("songs")
    val songs: List<PlaylistDetail.Playlist.Track>,
    @SerializedName("code")
    val code: Int,
    @SerializedName("album")
    val album: Album
) {


    data class Album(
        @SerializedName("songs")
        val songs: List<Any>,
        @SerializedName("paid")
        val paid: Boolean,
        @SerializedName("onSale")
        val onSale: Boolean,
        @SerializedName("mark")
        val mark: Int,
        @SerializedName("awardTags")
        val awardTags: Any,
        @SerializedName("displayTags")
        val displayTags: Any,
        @SerializedName("briefDesc")
        val briefDesc: Any,
        @SerializedName("artists")
        val artists: List<Artist>,
        @SerializedName("picId")
        val picId: Long,
        @SerializedName("artist")
        val artist: Artist,
        @SerializedName("copyrightId")
        val copyrightId: Int,
        @SerializedName("publishTime")
        val publishTime: Long,
        @SerializedName("company")
        val company: String,
        @SerializedName("picUrl")
        val picUrl: String,
        @SerializedName("commentThreadId")
        val commentThreadId: String,
        @SerializedName("blurPicUrl")
        val blurPicUrl: String,
        @SerializedName("companyId")
        val companyId: Int,
        @SerializedName("pic")
        val pic: Long,
        @SerializedName("status")
        val status: Int,
        @SerializedName("subType")
        val subType: String,
        @SerializedName("description")
        val description: String,
        @SerializedName("tags")
        val tags: String,
        @SerializedName("alias")
        val alias: List<Any>,
        @SerializedName("name")
        val name: String,
        @SerializedName("id")
        val id: Long,
        @SerializedName("type")
        val type: String,
        @SerializedName("size")
        val size: Int,
        @SerializedName("picId_str")
        val picIdStr: String,
        @SerializedName("info")
        val info: Info
    ) {

        data class Artist(
            @SerializedName("img1v1Id")
            val img1v1Id: Long,
            @SerializedName("topicPerson")
            val topicPerson: Int,
            @SerializedName("musicSize")
            val musicSize: Int,
            @SerializedName("albumSize")
            val albumSize: Int,
            @SerializedName("briefDesc")
            val briefDesc: String,
            @SerializedName("picId")
            val picId: Long,
            @SerializedName("picUrl")
            val picUrl: String,
            @SerializedName("img1v1Url")
            val img1v1Url: String,
            @SerializedName("followed")
            val followed: Boolean,
            @SerializedName("trans")
            val trans: String,
            @SerializedName("alias")
            val alias: List<Any>,
            @SerializedName("name")
            val name: String,
            @SerializedName("id")
            val id: Int,
            @SerializedName("picId_str")
            val picIdStr: String? = null,
            @SerializedName("img1v1Id_str")
            val img1v1IdStr: String
        )

        data class Info(
            @SerializedName("commentThread")
            val commentThread: CommentThread,
            @SerializedName("latestLikedUsers")
            val latestLikedUsers: Any,
            @SerializedName("liked")
            val liked: Boolean,
            @SerializedName("comments")
            val comments: Any,
            @SerializedName("resourceType")
            val resourceType: Int,
            @SerializedName("resourceId")
            val resourceId: Int,
            @SerializedName("commentCount")
            val commentCount: Int,
            @SerializedName("likedCount")
            val likedCount: Int,
            @SerializedName("shareCount")
            val shareCount: Int,
            @SerializedName("threadId")
            val threadId: String
        ) {
            data class CommentThread(
                @SerializedName("id")
                val id: String,
                @SerializedName("resourceInfo")
                val resourceInfo: ResourceInfo,
                @SerializedName("resourceType")
                val resourceType: Int,
                @SerializedName("commentCount")
                val commentCount: Int,
                @SerializedName("likedCount")
                val likedCount: Int,
                @SerializedName("shareCount")
                val shareCount: Int,
                @SerializedName("hotCount")
                val hotCount: Int,
                @SerializedName("latestLikedUsers")
                val latestLikedUsers: Any,
                @SerializedName("resourceId")
                val resourceId: Int,
                @SerializedName("resourceOwnerId")
                val resourceOwnerId: Int,
                @SerializedName("resourceTitle")
                val resourceTitle: String
            ) {
                data class ResourceInfo(
                    @SerializedName("id")
                    val id: Int,
                    @SerializedName("userId")
                    val userId: Int,
                    @SerializedName("name")
                    val name: String,
                    @SerializedName("imgUrl")
                    val imgUrl: String,
                    @SerializedName("creator")
                    val creator: Any,
                    @SerializedName("encodedId")
                    val encodedId: Any,
                    @SerializedName("subTitle")
                    val subTitle: Any,
                    @SerializedName("webUrl")
                    val webUrl: Any
                )
            }
        }
    }
}

data class MiniAlbumDetail(
    val name: String,
    val id: Long,
    val picUrl: String,
    val description: String,
    val artist: List<Artist>,
    val publishTime: Long,
    val collected: Boolean = false
){
    data class Artist(
        val name: String,
        val id: Int
    )
}

fun AlbumDetail.toMiniAlbumDetail(): MiniAlbumDetail {
    return MiniAlbumDetail(
        name = album.name,
        id = album.id,
        picUrl = album.picUrl,
        description = album.description,
        publishTime = album.publishTime,
        artist = album.artists.map {
            MiniAlbumDetail.Artist(
                name = it.name,
                id = it.id
            )
        }
    )
}