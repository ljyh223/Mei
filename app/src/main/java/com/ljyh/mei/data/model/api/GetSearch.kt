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
            @SerializedName("transNames")
            val transNames: List<String>,
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
    }

    data class Trp(
        @SerializedName("rules")
        val rules: List<String>
    )
}