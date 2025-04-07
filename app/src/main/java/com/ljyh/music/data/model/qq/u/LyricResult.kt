package com.ljyh.music.data.model.qq.u
import com.google.gson.annotations.SerializedName




data class LyricResult(
    @SerializedName("code")
    val code: Int,
    @SerializedName("ts")
    val ts: Long,
    @SerializedName("start_ts")
    val startTs: Long,
    @SerializedName("traceid")
    val traceid: String,
    @SerializedName("music.musichallSong.PlayLyricInfo.GetPlayLyricInfo")
    val musicMusichallSongPlayLyricInfoGetPlayLyricInfo: MusicMusichallSongPlayLyricInfoGetPlayLyricInfo
) {
    data class MusicMusichallSongPlayLyricInfoGetPlayLyricInfo(
        @SerializedName("code")
        val code: Int,
        @SerializedName("data")
        val `data`: Data
    ) {
        data class Data(
            @SerializedName("songID")
            val songID: Int,
            @SerializedName("songName")
            val songName: String,
            @SerializedName("songType")
            val songType: Int,
            @SerializedName("singerName")
            val singerName: String,
            @SerializedName("qrc")
            val qrc: Int,
            @SerializedName("crypt")
            val crypt: Int,
            @SerializedName("lyric")
            val lyric: String,
            @SerializedName("trans")
            val trans: String,
            @SerializedName("roma")
            val roma: String,
            @SerializedName("lrc_t")
            val lrcT: Int,
            @SerializedName("qrc_t")
            val qrcT: Int,
            @SerializedName("trans_t")
            val transT: Int,
            @SerializedName("roma_t")
            val romaT: Int,
            @SerializedName("lyric_style")
            val lyricStyle: Int,
            @SerializedName("classical")
            val classical: Int,
            @SerializedName("introduceTitle")
            val introduceTitle: String,
            @SerializedName("introduceText")
            val introduceText: List<IntroduceText>,
            @SerializedName("vecSongID")
            val vecSongID: Any?,
            @SerializedName("track")
            val track: Track?,
            @SerializedName("startTs")
            val startTs: Int,
            @SerializedName("transSource")
            val transSource: Int,
            @SerializedName("hasContributor")
            val hasContributor: Boolean,
            @SerializedName("hasTransContributor")
            val hasTransContributor: Boolean
        ) {
            data class IntroduceText(
                @SerializedName("title")
                val title: String,
                @SerializedName("content")
                val content: String
            )

            data class Track(
                @SerializedName("id")
                val id: Int,
                @SerializedName("type")
                val type: Int,
                @SerializedName("mid")
                val mid: String,
                @SerializedName("name")
                val name: String,
                @SerializedName("title")
                val title: String,
                @SerializedName("subtitle")
                val subtitle: String,
                @SerializedName("singer")
                val singer: Any,
                @SerializedName("album")
                val album: Album,
                @SerializedName("mv")
                val mv: Mv,
                @SerializedName("interval")
                val interval: Int,
                @SerializedName("isonly")
                val isonly: Int,
                @SerializedName("language")
                val language: Int,
                @SerializedName("genre")
                val genre: Int,
                @SerializedName("index_cd")
                val indexCd: Int,
                @SerializedName("index_album")
                val indexAlbum: Int,
                @SerializedName("time_public")
                val timePublic: String,
                @SerializedName("status")
                val status: Int,
                @SerializedName("fnote")
                val fnote: Int,
                @SerializedName("file")
                val `file`: File,
                @SerializedName("pay")
                val pay: Pay,
                @SerializedName("action")
                val action: Action,
                @SerializedName("ksong")
                val ksong: Ksong,
                @SerializedName("volume")
                val volume: Volume,
                @SerializedName("label")
                val label: String,
                @SerializedName("url")
                val url: String,
                @SerializedName("bpm")
                val bpm: Int,
                @SerializedName("version")
                val version: Int,
                @SerializedName("trace")
                val trace: String,
                @SerializedName("data_type")
                val dataType: Int,
                @SerializedName("modify_stamp")
                val modifyStamp: Int,
                @SerializedName("pingpong")
                val pingpong: String,
                @SerializedName("aid")
                val aid: Int,
                @SerializedName("ppurl")
                val ppurl: String,
                @SerializedName("tid")
                val tid: Int,
                @SerializedName("ov")
                val ov: Int,
                @SerializedName("sa")
                val sa: Int,
                @SerializedName("es")
                val es: String,
                @SerializedName("vs")
                val vs: Any,
                @SerializedName("vi")
                val vi: Any,
                @SerializedName("ktag")
                val ktag: String,
                @SerializedName("vf")
                val vf: Any,
                @SerializedName("va")
                val va: Any
            ) {
                data class Album(
                    @SerializedName("id")
                    val id: Int,
                    @SerializedName("mid")
                    val mid: String,
                    @SerializedName("name")
                    val name: String,
                    @SerializedName("title")
                    val title: String,
                    @SerializedName("subtitle")
                    val subtitle: String,
                    @SerializedName("time_public")
                    val timePublic: String,
                    @SerializedName("pmid")
                    val pmid: String
                )

                data class Mv(
                    @SerializedName("id")
                    val id: Int,
                    @SerializedName("vid")
                    val vid: String,
                    @SerializedName("name")
                    val name: String,
                    @SerializedName("title")
                    val title: String,
                    @SerializedName("vt")
                    val vt: Int
                )

                data class File(
                    @SerializedName("media_mid")
                    val mediaMid: String,
                    @SerializedName("size_24aac")
                    val size24aac: Int,
                    @SerializedName("size_48aac")
                    val size48aac: Int,
                    @SerializedName("size_96aac")
                    val size96aac: Int,
                    @SerializedName("size_192ogg")
                    val size192ogg: Int,
                    @SerializedName("size_192aac")
                    val size192aac: Int,
                    @SerializedName("size_128mp3")
                    val size128mp3: Int,
                    @SerializedName("size_320mp3")
                    val size320mp3: Int,
                    @SerializedName("size_ape")
                    val sizeApe: Int,
                    @SerializedName("size_flac")
                    val sizeFlac: Int,
                    @SerializedName("size_dts")
                    val sizeDts: Int,
                    @SerializedName("size_try")
                    val sizeTry: Int,
                    @SerializedName("try_begin")
                    val tryBegin: Int,
                    @SerializedName("try_end")
                    val tryEnd: Int,
                    @SerializedName("url")
                    val url: String,
                    @SerializedName("size_hires")
                    val sizeHires: Int,
                    @SerializedName("hires_sample")
                    val hiresSample: Int,
                    @SerializedName("hires_bitdepth")
                    val hiresBitdepth: Int,
                    @SerializedName("b_30s")
                    val b30s: Int,
                    @SerializedName("e_30s")
                    val e30s: Int,
                    @SerializedName("size_96ogg")
                    val size96ogg: Int,
                    @SerializedName("size_360ra")
                    val size360ra: Any,
                    @SerializedName("size_dolby")
                    val sizeDolby: Int,
                    @SerializedName("size_new")
                    val sizeNew: Any
                )

                data class Pay(
                    @SerializedName("pay_month")
                    val payMonth: Int,
                    @SerializedName("price_track")
                    val priceTrack: Int,
                    @SerializedName("price_album")
                    val priceAlbum: Int,
                    @SerializedName("pay_play")
                    val payPlay: Int,
                    @SerializedName("pay_down")
                    val payDown: Int,
                    @SerializedName("pay_status")
                    val payStatus: Int,
                    @SerializedName("time_free")
                    val timeFree: Int
                )

                data class Action(
                    @SerializedName("switch")
                    val switch: Int,
                    @SerializedName("msgid")
                    val msgid: Int,
                    @SerializedName("alert")
                    val alert: Int,
                    @SerializedName("icons")
                    val icons: Int,
                    @SerializedName("msgshare")
                    val msgshare: Int,
                    @SerializedName("msgfav")
                    val msgfav: Int,
                    @SerializedName("msgdown")
                    val msgdown: Int,
                    @SerializedName("msgpay")
                    val msgpay: Int,
                    @SerializedName("switch2")
                    val switch2: Any,
                    @SerializedName("icon2")
                    val icon2: Int
                )

                data class Ksong(
                    @SerializedName("id")
                    val id: Int,
                    @SerializedName("mid")
                    val mid: String
                )

                data class Volume(
                    @SerializedName("gain")
                    val gain: Int,
                    @SerializedName("peak")
                    val peak: Int,
                    @SerializedName("lra")
                    val lra: Int
                )
            }
        }
    }
}


val emptyData= LyricResult.MusicMusichallSongPlayLyricInfoGetPlayLyricInfo.Data(
    songID = 0,
    songName = "",
    songType = 0,
    singerName = "",
    qrc = 0,
    crypt = 0,
    lyric = "",
    trans = "",
    roma = "",
    lrcT = 0,
    qrcT = 0,
    transT = 0,
    romaT = 0,
    lyricStyle = 0,
    classical = 0,
    introduceTitle = "",
    introduceText = emptyList(),
    vecSongID = null, // 或者根据实际需求设置为其他默认值
    track = null,
    startTs = 0,
    transSource = 0,
    hasContributor = false,
    hasTransContributor = false
)