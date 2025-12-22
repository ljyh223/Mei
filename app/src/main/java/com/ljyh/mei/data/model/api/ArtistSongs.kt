package com.ljyh.mei.data.model.api
import com.google.gson.annotations.SerializedName


data class GetArtistSong(
    val limit: Int = 50,
    val offset: Int = 0,
    val total: Boolean = true,
)



data class ArtistSong(
    @SerializedName("artist")
    val artist: Artist,
    @SerializedName("hotSongs")
    val hotSongs: List<HotSong>,
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
        @SerializedName("publishTime")
        val publishTime: Long,
        @SerializedName("accountId")
        val accountId: Long,
        @SerializedName("picId_str")
        val picIdStr: String,
        @SerializedName("transNames")
        val transNames: List<String>,
        @SerializedName("img1v1Id_str")
        val img1v1IdStr: String,
        @SerializedName("mvSize")
        val mvSize: Int
    )

    data class HotSong(
        @SerializedName("rtUrls")
        val rtUrls: List<Any>,
        @SerializedName("ar")
        val ar: List<Ar>,
        @SerializedName("al")
        val al: Al,
        @SerializedName("st")
        val st: Int,
        @SerializedName("noCopyrightRcmd")
        val noCopyrightRcmd: Any,
        @SerializedName("songJumpInfo")
        val songJumpInfo: Any,
        @SerializedName("djId")
        val djId: Int,
        @SerializedName("fee")
        val fee: Int,
        @SerializedName("no")
        val no: Int,
        @SerializedName("mv")
        val mv: Int,
        @SerializedName("cd")
        val cd: String,
        @SerializedName("t")
        val t: Int,
        @SerializedName("v")
        val v: Int,
        @SerializedName("rtype")
        val rtype: Int,
        @SerializedName("rurl")
        val rurl: Any,
        @SerializedName("pst")
        val pst: Int,
        @SerializedName("alia")
        val alia: List<String>,
        @SerializedName("pop")
        val pop: Int,
        @SerializedName("rt")
        val rt: String,
        @SerializedName("mst")
        val mst: Int,
        @SerializedName("cp")
        val cp: Int,
        @SerializedName("crbt")
        val crbt: Any,
        @SerializedName("cf")
        val cf: String,
        @SerializedName("dt")
        val dt: Long,
        @SerializedName("h")
        val h: H,
        @SerializedName("sq")
        val sq: Sq,
        @SerializedName("hr")
        val hr: Hr,
        @SerializedName("l")
        val l: L,
        @SerializedName("rtUrl")
        val rtUrl: Any,
        @SerializedName("ftype")
        val ftype: Int,
        @SerializedName("a")
        val a: Any,
        @SerializedName("m")
        val m: M,
        @SerializedName("name")
        val name: String,
        @SerializedName("id")
        val id: Long,
        @SerializedName("videoInfo")
        val videoInfo: VideoInfo,
        @SerializedName("privilege")
        val privilege: Privilege,
        @SerializedName("eq")
        val eq: String
    ) {
        data class Ar(
            @SerializedName("id")
            val id: Long,
            @SerializedName("name")
            val name: String,
            @SerializedName("tns")
            val tns: List<String>,
            @SerializedName("alia")
            val alia: List<String>
        )

        data class Al(
            @SerializedName("id")
            val id: Long,
            @SerializedName("name")
            val name: String,
            @SerializedName("pic_str")
            val picStr: String,
            @SerializedName("pic")
            val pic: Long,
            @SerializedName("tns")
            val tns: List<String>
        )

        data class H(
            @SerializedName("br")
            val br: Int,
            @SerializedName("fid")
            val fid: Int,
            @SerializedName("size")
            val size: Int,
            @SerializedName("vd")
            val vd: Int,
            @SerializedName("sr")
            val sr: Int
        )

        data class Sq(
            @SerializedName("br")
            val br: Int,
            @SerializedName("fid")
            val fid: Int,
            @SerializedName("size")
            val size: Int,
            @SerializedName("vd")
            val vd: Int,
            @SerializedName("sr")
            val sr: Int
        )

        data class Hr(
            @SerializedName("br")
            val br: Int,
            @SerializedName("fid")
            val fid: Int,
            @SerializedName("size")
            val size: Int,
            @SerializedName("vd")
            val vd: Int,
            @SerializedName("sr")
            val sr: Int
        )

        data class L(
            @SerializedName("br")
            val br: Int,
            @SerializedName("fid")
            val fid: Int,
            @SerializedName("size")
            val size: Int,
            @SerializedName("vd")
            val vd: Int,
            @SerializedName("sr")
            val sr: Int
        )

        data class M(
            @SerializedName("br")
            val br: Int,
            @SerializedName("fid")
            val fid: Int,
            @SerializedName("size")
            val size: Int,
            @SerializedName("vd")
            val vd: Int,
            @SerializedName("sr")
            val sr: Int
        )

        data class VideoInfo(
            @SerializedName("moreThanOne")
            val moreThanOne: Boolean,
            @SerializedName("video")
            val video: Video
        ) {
            data class Video(
                @SerializedName("vid")
                val vid: String,
                @SerializedName("type")
                val type: Int,
                @SerializedName("title")
                val title: String,
                @SerializedName("playTime")
                val playTime: Int,
                @SerializedName("coverUrl")
                val coverUrl: String,
                @SerializedName("publishTime")
                val publishTime: Long,
                @SerializedName("artists")
                val artists: Any
            )
        }

        data class Privilege(
            @SerializedName("id")
            val id: Long,
            @SerializedName("fee")
            val fee: Int,
            @SerializedName("payed")
            val payed: Int,
            @SerializedName("st")
            val st: Int,
            @SerializedName("pl")
            val pl: Int,
            @SerializedName("dl")
            val dl: Int,
            @SerializedName("sp")
            val sp: Int,
            @SerializedName("cp")
            val cp: Int,
            @SerializedName("subp")
            val subp: Int,
            @SerializedName("cs")
            val cs: Boolean,
            @SerializedName("maxbr")
            val maxbr: Int,
            @SerializedName("fl")
            val fl: Int,
            @SerializedName("toast")
            val toast: Boolean,
            @SerializedName("flag")
            val flag: Int,
            @SerializedName("preSell")
            val preSell: Boolean,
            @SerializedName("playMaxbr")
            val playMaxbr: Int,
            @SerializedName("downloadMaxbr")
            val downloadMaxbr: Int,
            @SerializedName("maxBrLevel")
            val maxBrLevel: String,
            @SerializedName("playMaxBrLevel")
            val playMaxBrLevel: String,
            @SerializedName("downloadMaxBrLevel")
            val downloadMaxBrLevel: String,
            @SerializedName("plLevel")
            val plLevel: String,
            @SerializedName("dlLevel")
            val dlLevel: String,
            @SerializedName("flLevel")
            val flLevel: String,
            @SerializedName("rscl")
            val rscl: Any,
            @SerializedName("freeTrialPrivilege")
            val freeTrialPrivilege: FreeTrialPrivilege,
            @SerializedName("rightSource")
            val rightSource: Int,
            @SerializedName("chargeInfoList")
            val chargeInfoList: List<ChargeInfo>,
            @SerializedName("code")
            val code: Int,
            @SerializedName("message")
            val message: Any,
            @SerializedName("plLevels")
            val plLevels: Any,
            @SerializedName("dlLevels")
            val dlLevels: Any,
            @SerializedName("ignoreCache")
            val ignoreCache: Any,
            @SerializedName("bd")
            val bd: Any
        ) {
            data class FreeTrialPrivilege(
                @SerializedName("resConsumable")
                val resConsumable: Boolean,
                @SerializedName("userConsumable")
                val userConsumable: Boolean,
                @SerializedName("listenType")
                val listenType: Any,
                @SerializedName("cannotListenReason")
                val cannotListenReason: Any,
                @SerializedName("playReason")
                val playReason: Any,
                @SerializedName("freeLimitTagType")
                val freeLimitTagType: Any
            )

            data class ChargeInfo(
                @SerializedName("rate")
                val rate: Int,
                @SerializedName("chargeUrl")
                val chargeUrl: Any,
                @SerializedName("chargeMessage")
                val chargeMessage: Any,
                @SerializedName("chargeType")
                val chargeType: Int
            )
        }
    }
}


