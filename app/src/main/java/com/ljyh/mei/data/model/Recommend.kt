package com.ljyh.mei.data.model
import com.google.gson.annotations.SerializedName


data class Recommend(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data
) {
    data class Data(
        @SerializedName("dailySongs")
        val dailySongs: List<DailySong>,
        @SerializedName("demote")
        val demote: Boolean,
        @SerializedName("mvResourceInfos")
        val mvResourceInfos: Any,
        @SerializedName("orderSongs")
        val orderSongs: List<Any>,
        @SerializedName("recommendReasons")
        val recommendReasons: List<RecommendReason>
    ) {
        data class DailySong(
            @SerializedName("a")
            val a: Any,
            @SerializedName("al")
            val al: Al,
            @SerializedName("alg")
            val alg: String,
            @SerializedName("alia")
            val alia: List<String>,
            @SerializedName("ar")
            val ar: List<Ar>,
            @SerializedName("cd")
            val cd: String,
            @SerializedName("cf")
            val cf: String,
            @SerializedName("copyright")
            val copyright: Int,
            @SerializedName("cp")
            val cp: Int,
            @SerializedName("crbt")
            val crbt: Any,
            @SerializedName("djId")
            val djId: Long,
            @SerializedName("dt")
            val dt: Int,
            @SerializedName("entertainmentTags")
            val entertainmentTags: Any,
            @SerializedName("fee")
            val fee: Int,
            @SerializedName("ftype")
            val ftype: Int,
            @SerializedName("h")
            val h: H,
            @SerializedName("hr")
            val hr: Hr,
            @SerializedName("id")
            val id: Long,
            @SerializedName("l")
            val l: L,
            @SerializedName("m")
            val m: M,
            @SerializedName("mark")
            val mark: Long,
            @SerializedName("mst")
            val mst: Int,
            @SerializedName("mv")
            val mv: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("no")
            val no: Int,
            @SerializedName("noCopyrightRcmd")
            val noCopyrightRcmd: Any,
            @SerializedName("originCoverType")
            val originCoverType: Int,
            @SerializedName("originSongSimpleData")
            val originSongSimpleData: Any,
            @SerializedName("pc")
            val pc: Pc,
            @SerializedName("pop")
            val pop: Int,
            @SerializedName("privilege")
            val privilege: Privilege,
            @SerializedName("pst")
            val pst: Int,
            @SerializedName("publishTime")
            val publishTime: Long,
            @SerializedName("reason")
            val reason: String,
            @SerializedName("recommendReason")
            val recommendReason: String,
            @SerializedName("resourceState")
            val resourceState: Boolean,
            @SerializedName("rt")
            val rt: String,
            @SerializedName("rtUrl")
            val rtUrl: Any,
            @SerializedName("rtUrls")
            val rtUrls: List<Any>,
            @SerializedName("rtype")
            val rtype: Int,
            @SerializedName("rurl")
            val rurl: Any,
            @SerializedName("s_id")
            val sId: Long,
            @SerializedName("single")
            val single: Int,
            @SerializedName("songJumpInfo")
            val songJumpInfo: Any,
            @SerializedName("sq")
            val sq: Sq,
            @SerializedName("st")
            val st: Int,
            @SerializedName("t")
            val t: Int,
            @SerializedName("tagPicList")
            val tagPicList: Any,
            @SerializedName("tns")
            val tns: List<String>,
            @SerializedName("v")
            val v: Int,
            @SerializedName("version")
            val version: Int
        ) {
            data class Al(
                @SerializedName("id")
                val Id: Long,
                @SerializedName("name")
                val name: String,
                @SerializedName("pic")
                val pic: Long,
                @SerializedName("pic_str")
                val picStr: String,
                @SerializedName("picUrl")
                val picUrl: String,
                @SerializedName("tns")
                val tns: List<String>
            )

            data class Ar(
                @SerializedName("alias")
                val alias: List<Any>,
                @SerializedName("id")
                val Id: Long,
                @SerializedName("name")
                val name: String,
                @SerializedName("tns")
                val tns: List<Any>
            )

            data class H(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fId: Long,
                @SerializedName("size")
                val size: Int,
                @SerializedName("sr")
                val sr: Int,
                @SerializedName("vd")
                val vd: Int
            )

            data class Hr(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fId: Long,
                @SerializedName("size")
                val size: Int,
                @SerializedName("sr")
                val sr: Int,
                @SerializedName("vd")
                val vd: Int
            )

            data class L(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fId: Long,
                @SerializedName("size")
                val size: Int,
                @SerializedName("sr")
                val sr: Int,
                @SerializedName("vd")
                val vd: Int
            )

            data class M(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fId: Long,
                @SerializedName("size")
                val size: Int,
                @SerializedName("sr")
                val sr: Int,
                @SerializedName("vd")
                val vd: Int
            )

            data class Pc(
                @SerializedName("alb")
                val alb: String,
                @SerializedName("ar")
                val ar: String,
                @SerializedName("br")
                val br: Int,
                @SerializedName("cid")
                val cid: String,
                @SerializedName("fn")
                val fn: String,
                @SerializedName("nickname")
                val nickname: String,
                @SerializedName("sn")
                val sn: String,
                @SerializedName("uid")
                val uid: Long,
                @SerializedName("version")
                val version: Int
            )

            data class Privilege(
                @SerializedName("chargeInfoList")
                val chargeInfoList: List<ChargeInfo>,
                @SerializedName("code")
                val code: Int,
                @SerializedName("cp")
                val cp: Int,
                @SerializedName("cs")
                val cs: Boolean,
                @SerializedName("dl")
                val dl: Int,
                @SerializedName("dlLevel")
                val dlLevel: String,
                @SerializedName("downloadMaxBrLevel")
                val downloadMaxBrLevel: String,
                @SerializedName("downloadMaxbr")
                val downloadMaxbr: Int,
                @SerializedName("fee")
                val fee: Int,
                @SerializedName("fl")
                val fl: Int,
                @SerializedName("flLevel")
                val flLevel: String,
                @SerializedName("flag")
                val flag: Int,
                @SerializedName("freeTrialPrivilege")
                val freeTrialPrivilege: FreeTrialPrivilege,
                @SerializedName("id")
                val id: Long,
                @SerializedName("maxBrLevel")
                val maxBrLevel: String,
                @SerializedName("maxbr")
                val maxbr: Int,
                @SerializedName("message")
                val message: Any,
                @SerializedName("paidBigBang")
                val paidBigBang: Boolean,
                @SerializedName("payed")
                val payed: Int,
                @SerializedName("pc")
                val pc: Pc,
                @SerializedName("pl")
                val pl: Int,
                @SerializedName("plLevel")
                val plLevel: String,
                @SerializedName("playMaxBrLevel")
                val playMaxBrLevel: String,
                @SerializedName("playMaxbr")
                val playMaxbr: Int,
                @SerializedName("preSell")
                val preSell: Boolean,
                @SerializedName("realPayed")
                val realPayed: Int,
                @SerializedName("rightSource")
                val rightSource: Int,
                @SerializedName("rscl")
                val rscl: Any,
                @SerializedName("sp")
                val sp: Int,
                @SerializedName("st")
                val st: Int,
                @SerializedName("subp")
                val subp: Int,
                @SerializedName("toast")
                val toast: Boolean
            ) {
                data class ChargeInfo(
                    @SerializedName("chargeMessage")
                    val chargeMessage: Any,
                    @SerializedName("chargeType")
                    val chargeType: Int,
                    @SerializedName("chargeUrl")
                    val chargeUrl: Any,
                    @SerializedName("rate")
                    val rate: Int
                )

                data class FreeTrialPrivilege(
                    @SerializedName("cannotListenReason")
                    val cannotListenReason: Any,
                    @SerializedName("freeLimitTagType")
                    val freeLimitTagType: Any,
                    @SerializedName("listenType")
                    val listenType: Any,
                    @SerializedName("playReason")
                    val playReason: Any,
                    @SerializedName("resConsumable")
                    val resConsumable: Boolean,
                    @SerializedName("userConsumable")
                    val userConsumable: Boolean
                )

                data class Pc(
                    @SerializedName("addTime")
                    val addTime: Long,
                    @SerializedName("album")
                    val album: String,
                    @SerializedName("artist")
                    val artist: String,
                    @SerializedName("bitrate")
                    val bitrate: Int,
                    @SerializedName("convertLyric")
                    val convertLyric: Int,
                    @SerializedName("cover")
                    val cover: Int,
                    @SerializedName("cue")
                    val cue: Int,
                    @SerializedName("fileName")
                    val fileName: String,
                    @SerializedName("fileSize")
                    val fileSize: Int,
                    @SerializedName("id")
                    val id: Long,
                    @SerializedName("lrcType")
                    val lrcType: String,
                    @SerializedName("lyric")
                    val lyric: Int,
                    @SerializedName("md5")
                    val md5: String,
                    @SerializedName("originalAudioSongId")
                    val originalAudioSongId: Long,
                    @SerializedName("song")
                    val song: String,
                    @SerializedName("songDfsId")
                    val songDfsId: Long,
                    @SerializedName("songId")
                    val songId: Long,
                    @SerializedName("status")
                    val status: Int,
                    @SerializedName("userId")
                    val userId: Long,
                    @SerializedName("version")
                    val version: Int
                )
            }

            data class Sq(
                @SerializedName("br")
                val br: Int,
                @SerializedName("fid")
                val fId: Long,
                @SerializedName("size")
                val size: Int,
                @SerializedName("sr")
                val sr: Int,
                @SerializedName("vd")
                val vd: Int
            )
        }

        data class RecommendReason(
            @SerializedName("reason")
            val reason: String,
            @SerializedName("reasonId")
            val reasonId: String,
            @SerializedName("songId")
            val songId: Long,
            @SerializedName("targetUrl")
            val targetUrl: Any
        )
    }
}