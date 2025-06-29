package com.ljyh.mei.data.model.qq.u
import com.google.gson.annotations.SerializedName








data class GetLyricData(
    @SerializedName("comm")
    val comm: Comm = Comm(),
    @SerializedName("music.musichallSong.PlayLyricInfo.GetPlayLyricInfo")
    val getPlayLyricInfo: GetPlayLyricInfo
){
    data class Comm(
        @SerializedName("_channelid")
        val channelid: String = "",
        @SerializedName("_os_version")
        val osVersion: String = "6.2.9200-2",
        @SerializedName("authst")
        val authst: String = "",
        @SerializedName("ct")
        val ct: String = "19",
        @SerializedName("cv")
        val cv: String = "1942",
        @SerializedName("patch")
        val patch: String = "118",
        @SerializedName("psrf_access_token_expiresAt")
        val psrfAccessTokenExpiresAt: Int = 0,
        @SerializedName("psrf_qqaccess_token")
        val psrfQqaccessToken: String = "",
        @SerializedName("psrf_qqopenid")
        val psrfQqopenid: String = "",
        @SerializedName("psrf_qqunionid")
        val psrfQqunionid: String = "",
        @SerializedName("tmeAppID")
        val tmeAppID: String = "qqmusic",
        @SerializedName("tmeLoginType")
        val tmeLoginType: Int = 0,
        @SerializedName("uin")
        val uin: String = "",
        @SerializedName("wid")
        val wid: String = ""
    )


    data class GetPlayLyricInfo(
        @SerializedName("method")
        val method: String = "GetPlayLyricInfo",
        @SerializedName("module")
        val module: String = "music.musichallSong.PlayLyricInfo",
        @SerializedName("param")
        val `param`: GetLyric
    ){
        data class GetLyric(
            @SerializedName("albumName")
            val albumName: String ,
            @SerializedName("crypt")
            val crypt: Int = 1,
            @SerializedName("ct")
            val ct: Int = 19,
            @SerializedName("cv")
            val cv: Int = 1942,
            @SerializedName("interval")
            val interval: Int,
            @SerializedName("lrc_t")
            val lrcT: Int = 0,
            @SerializedName("qrc")
            val qrc: Int = 1,
            @SerializedName("qrc_t")
            val qrcT: Int = 0,
            @SerializedName("roma")
            val roma: Int = 1,
            @SerializedName("roma_t")
            val romaT: Int = 0,
            @SerializedName("singerName")
            val singerName: String,
            @SerializedName("songID")
            val songID: Int,
            @SerializedName("songName")
            val songName: String,
            @SerializedName("trans")
            val trans: Int = 1,
            @SerializedName("trans_t")
            val transT: Int = 0,
            @SerializedName("type")
            val type: Int = 0
        )
    }


}