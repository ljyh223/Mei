package com.ljyh.mei.data.model

import com.google.gson.annotations.SerializedName
import com.ljyh.mei.ui.model.Album

data class UserAlbumList(
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("count")
    val count: Int,
    @SerializedName("hasMore")
    val hasMore: Boolean,
    @SerializedName("paidCount")
    val paidCount: Int,
    @SerializedName("code")
    val code: Int
) {
    data class Data(
        @SerializedName("subTime")
        val subTime: Long,
        @SerializedName("msg")
        val msg: List<Any>,
        @SerializedName("artists")
        val artists: List<Artist>,
        @SerializedName("picId")
        val picId: Long,
        @SerializedName("picUrl")
        val picUrl: String,
        @SerializedName("alias")
        val alias: List<String>,
        @SerializedName("name")
        val name: String,
        @SerializedName("id")
        val id: Long,
        @SerializedName("size")
        val size: Int,
        @SerializedName("transNames")
        val transNames: List<Any>
    ) {
        data class Artist(
            @SerializedName("img1v1Id")
            val img1v1Id: Long,
            @SerializedName("topicPerson")
            val topicPerson: Int,
            @SerializedName("picId")
            val picId: Int,
            @SerializedName("briefDesc")
            val briefDesc: String,
            @SerializedName("musicSize")
            val musicSize: Int,
            @SerializedName("albumSize")
            val albumSize: Int,
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


fun UserAlbumList.Data.toAlbum(): Album {
    return Album(
        id = id,
        title = name,
        cover = picUrl,
        size = size,
        artist = artists.map {
            Album.Artist(
                id = it.id,
                name = it.name
            )
        })
}