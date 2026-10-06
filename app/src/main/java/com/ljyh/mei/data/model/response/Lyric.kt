package com.ljyh.mei.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Netease lyric payload fields used by lyric rendering, download, and pure-music handling. */
@Serializable
data class Lyric(
    @SerialName("code") val code: Int = 0,
    @SerialName("klyric") val klyric: Klyric = Klyric(),
    @SerialName("lrc") val lrc: Lrc = Lrc(),
    @SerialName("qfy") val qfy: Boolean = false,
    @SerialName("romalrc") val romalrc: Romalrc? = null,
    @SerialName("sfy") val sfy: Boolean = false,
    @SerialName("sgc") val sgc: Boolean = false,
    @SerialName("tlyric") val tlyric: Tlyric? = null,
    @SerialName("ytlrc") val ytlrc: Tlyric? = null,
    @SerialName("yrc") val yrc: Yrc? = null,
    @SerialName("pureMusic") val pureMusic: Boolean? = null,
) {
    @Serializable
    data class Klyric(
        @SerialName("lyric") val lyric: String = "",
        @SerialName("version") val version: Int = 0,
    )

    @Serializable
    data class Lrc(
        @SerialName("lyric") val lyric: String = "",
        @SerialName("version") val version: Int = 0,
    )

    @Serializable
    data class Romalrc(
        @SerialName("lyric") val lyric: String = "",
        @SerialName("version") val version: Int = 0,
    )

    @Serializable
    data class Tlyric(
        @SerialName("lyric") val lyric: String = "",
        @SerialName("version") val version: Int = 0,
    )

    @Serializable
    data class Ytlrc(
        @SerialName("lyric") val lyric: String = "",
        @SerialName("version") val version: Int = 0,
    )

    @Serializable
    data class Yrc(
        @SerialName("lyric") val lyric: String = "",
        @SerialName("version") val version: Int = 0,
    )
}
