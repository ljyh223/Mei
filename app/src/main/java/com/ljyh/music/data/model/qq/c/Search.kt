package com.ljyh.music.data.model.qq.c
import com.google.gson.annotations.SerializedName


data class Search(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("subcode")
    val subcode: Int
) {
    data class Data(
        @SerializedName("album")
        val album: Album,
        @SerializedName("mv")
        val mv: Mv,
        @SerializedName("singer")
        val singer: Singer,
        @SerializedName("song")
        val song: Song
    ) {
        data class Album(
            @SerializedName("count")
            val count: Int,
            @SerializedName("itemlist")
            val itemlist: List<Any>,
            @SerializedName("name")
            val name: String,
            @SerializedName("order")
            val order: Int,
            @SerializedName("type")
            val type: Int
        )

        data class Mv(
            @SerializedName("count")
            val count: Int,
            @SerializedName("itemlist")
            val itemlist: List<Any>,
            @SerializedName("name")
            val name: String,
            @SerializedName("order")
            val order: Int,
            @SerializedName("type")
            val type: Int
        )

        data class Singer(
            @SerializedName("count")
            val count: Int,
            @SerializedName("itemlist")
            val itemlist: List<Any>,
            @SerializedName("name")
            val name: String,
            @SerializedName("order")
            val order: Int,
            @SerializedName("type")
            val type: Int
        )

        data class Song(
            @SerializedName("count")
            val count: Int,
            @SerializedName("itemlist")
            val itemlist: List<Itemlist>,
            @SerializedName("name")
            val name: String,
            @SerializedName("order")
            val order: Int,
            @SerializedName("type")
            val type: Int
        ) {
            data class Itemlist(
                @SerializedName("docid")
                val docid: String,
                @SerializedName("id")
                val id: String,
                @SerializedName("mid")
                val mid: String,
                @SerializedName("name")
                val name: String,
                @SerializedName("singer")
                val singer: String
            )
        }
    }
}