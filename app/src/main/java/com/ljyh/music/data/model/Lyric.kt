package com.ljyh.music.data.model
import com.google.gson.annotations.SerializedName


data class Lyric(
    @SerializedName("code")
    val code: Int,
    @SerializedName("klyric")
    val klyric: Klyric,
    @SerializedName("lrc")
    val lrc: Lrc,
    @SerializedName("qfy")
    val qfy: Boolean,
    @SerializedName("romalrc")
    val romalrc: Romalrc,
    @SerializedName("sfy")
    val sfy: Boolean,
    @SerializedName("sgc")
    val sgc: Boolean,
    @SerializedName("tlyric")
    val tlyric: Tlyric,
    @SerializedName("pureMusic")
    val pureMusic: Boolean?


) {

    companion object {
        const val LYRICS_NOT_FOUND = "LYRICS_NOT_FOUND"
    }
    data class Klyric(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )

    data class Lrc(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )

    data class Romalrc(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )

    data class Tlyric(
        @SerializedName("lyric")
        val lyric: String,
        @SerializedName("version")
        val version: Int
    )
}