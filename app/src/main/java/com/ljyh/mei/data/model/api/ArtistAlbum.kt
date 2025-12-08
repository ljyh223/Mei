package com.ljyh.mei.data.model.api

import com.google.gson.annotations.SerializedName


data class GetArtistAlbum(
    val limit: Int = 50,
    val offset: Int = 0,
    val total: Boolean = true,
)

data class ArtistAlbum(
    @SerializedName("artist")
    val artist: Artist,
    @SerializedName("hotAlbums")
    val hotAlbums: List<HotAlbum>,
    @SerializedName("more")
    val more: Boolean,
    @SerializedName("code")
    val code: Int
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
        val picIdStr: String,
        @SerializedName("transNames")
        val transNames: List<String>,
        @SerializedName("img1v1Id_str")
        val img1v1IdStr: String
    )

    data class HotAlbum(
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
        val briefDesc: String,
        @SerializedName("company")
        val company: String,
        @SerializedName("publishTime")
        val publishTime: Long,
        @SerializedName("artists")
        val artists: List<Artist>,
        @SerializedName("copyrightId")
        val copyrightId: Int,
        @SerializedName("picId")
        val picId: Long,
        @SerializedName("artist")
        val artist: Artist,
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
        val id: Int,
        @SerializedName("type")
        val type: String,
        @SerializedName("size")
        val size: Int,
        @SerializedName("picId_str")
        val picIdStr: String
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
            val id: Long,
            @SerializedName("img1v1Id_str")
            val img1v1IdStr: String
        )


    }
}

