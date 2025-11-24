package com.ljyh.mei.data.model.eapi
import com.google.gson.annotations.SerializedName


data class GetSearch(
    @SerializedName("keyword")
    val keyword: String,
    @SerializedName("scene")
    val scene: String="normal",
    @SerializedName("needCorrect")
    val needCorrect: String="true",
    @SerializedName("channel")
    val channel: String="typing",
    @SerializedName("bizQueryInfo")
    val bizQueryInfo: String="",
    @SerializedName("e_r")
    val eR: Boolean= true
)




data class SearchResult(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("trp")
    val trp: Trp
) {
    data class Data(
        @SerializedName("traceId")
        val traceId: String,
        @SerializedName("cursor")
        val cursor: Cursor,
        @SerializedName("blocks")
        val blocks: List<Block>,
        @SerializedName("preJumpUrl")
        val preJumpUrl: Any,
        @SerializedName("sceneTransmissionInfo")
        val sceneTransmissionInfo: SceneTransmissionInfo,
        @SerializedName("demote")
        val demote: Boolean,
        @SerializedName("localCache")
        val localCache: Boolean
    ) {
        data class Cursor(
            @SerializedName("page")
            val page: Int,
            @SerializedName("ignoreBlocks")
            val ignoreBlocks: List<String>,
            @SerializedName("traceId")
            val traceId: String
        )

        data class Block(
            @SerializedName("blockCode")
            val blockCode: String,
            @SerializedName("resources")
            val resources: List<Resource>,
            @SerializedName("hotResultLinkResourceData")
            val hotResultLinkResourceData: Any,
            @SerializedName("autoPlayIds")
            val autoPlayIds: Any,
            @SerializedName("autoPlayAlgs")
            val autoPlayAlgs: Any,
            @SerializedName("more")
            val more: More,
            @SerializedName("tagSelectSongTagInfoDetails")
            val tagSelectSongTagInfoDetails: Any,
            @SerializedName("hlWords")
            val hlWords: List<String>,
            @SerializedName("activityTextLabel")
            val activityTextLabel: Any,
            @SerializedName("extInfo")
            val extInfo: ExtInfo,
            @SerializedName("zoneID")
            val zoneID: Any,
            @SerializedName("demote")
            val demote: Boolean
        ) {
            data class Resource(
                @SerializedName("blockCode")
                val blockCode: String,
                @SerializedName("resourceName")
                val resourceName: String,
                @SerializedName("resourceType")
                val resourceType: String,
                @SerializedName("resourceId")
                val resourceId: String,
                @SerializedName("baseInfo")
                val baseInfo: BaseInfo,
                @SerializedName("extInfo")
                val extInfo: ExtInfo,
                @SerializedName("action")
                val action: String,
                @SerializedName("actionType")
                val actionType: String,
                @SerializedName("type")
                val type: String,
                @SerializedName("alg")
                val alg: String,
                @SerializedName("foldId")
                val foldId: String,
                @SerializedName("algInfo")
                val algInfo: String
            ) {
                data class BaseInfo(
                    @SerializedName("artistDTO")
                    val artistDTO: ArtistDTO,
                    @SerializedName("albumData")
                    val albumData: AlbumData,
                    @SerializedName("activityDTO")
                    val activityDTO: ActivityDTO,
                    @SerializedName("simpleSongData")
                    val simpleSongData: SimpleSongData,
                    @SerializedName("metaData")
                    val metaData: List<String>,
                    @SerializedName("pubPlaylistData")
                    val pubPlaylistData: PubPlaylistData,
                    @SerializedName("pubDJRadioData")
                    val pubDJRadioData: PubDJRadioData,
                    @SerializedName("pubDJProgramData")
                    val pubDJProgramData: PubDJProgramData,
                    @SerializedName("pubUserProfileData")
                    val pubUserProfileData: PubUserProfileData
                ) {
                    data class ArtistDTO(
                        @SerializedName("id")
                        val id: Int,
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("picUrl")
                        val picUrl: String,
                        @SerializedName("musicSize")
                        val musicSize: Int,
                        @SerializedName("albumSize")
                        val albumSize: Int,
                        @SerializedName("briefDesc")
                        val briefDesc: String,
                        @SerializedName("alias")
                        val alias: List<String>,
                        @SerializedName("transNames")
                        val transNames: List<String>,
                        @SerializedName("trans")
                        val trans: String,
                        @SerializedName("img1v1Url")
                        val img1v1Url: String
                    )

                    data class AlbumData(
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("id")
                        val id: Int,
                        @SerializedName("type")
                        val type: String,
                        @SerializedName("size")
                        val size: Int,
                        @SerializedName("picId")
                        val picId: Long,
                        @SerializedName("blurPicUrl")
                        val blurPicUrl: String,
                        @SerializedName("companyId")
                        val companyId: Int,
                        @SerializedName("pic")
                        val pic: Long,
                        @SerializedName("picUrl")
                        val picUrl: String,
                        @SerializedName("publishTime")
                        val publishTime: Long,
                        @SerializedName("description")
                        val description: String,
                        @SerializedName("tags")
                        val tags: String,
                        @SerializedName("company")
                        val company: String,
                        @SerializedName("briefDesc")
                        val briefDesc: String,
                        @SerializedName("artist")
                        val artist: Artist,
                        @SerializedName("songs")
                        val songs: List<Any>,
                        @SerializedName("alias")
                        val alias: List<String>,
                        @SerializedName("status")
                        val status: Int,
                        @SerializedName("copyrightId")
                        val copyrightId: Int,
                        @SerializedName("commentThreadId")
                        val commentThreadId: String,
                        @SerializedName("artists")
                        val artists: List<Artist>,
                        @SerializedName("subType")
                        val subType: String,
                        @SerializedName("transName")
                        val transName: Any,
                        @SerializedName("onSale")
                        val onSale: Boolean,
                        @SerializedName("mark")
                        val mark: Int,
                        @SerializedName("gapless")
                        val gapless: Int,
                        @SerializedName("dolbyMark")
                        val dolbyMark: Int,
                        @SerializedName("picId_str")
                        val picIdStr: String
                    ) {


                        data class Artist(
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("picId")
                            val picId: Int,
                            @SerializedName("img1v1Id")
                            val img1v1Id: Int,
                            @SerializedName("briefDesc")
                            val briefDesc: String,
                            @SerializedName("picUrl")
                            val picUrl: String,
                            @SerializedName("img1v1Url")
                            val img1v1Url: String,
                            @SerializedName("albumSize")
                            val albumSize: Int,
                            @SerializedName("alias")
                            val alias: List<Any>,
                            @SerializedName("trans")
                            val trans: String,
                            @SerializedName("musicSize")
                            val musicSize: Int,
                            @SerializedName("topicPerson")
                            val topicPerson: Int
                        )
                    }

                    data class ActivityDTO(
                        @SerializedName("content")
                        val content: String,
                        @SerializedName("resourceUrl")
                        val resourceUrl: String,
                        @SerializedName("picUrl")
                        val picUrl: String,
                        @SerializedName("subTitle")
                        val subTitle: Any
                    )

                    data class SimpleSongData(
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("mainTitle")
                        val mainTitle: String,
                        @SerializedName("additionalTitle")
                        val additionalTitle: String,
                        @SerializedName("id")
                        val id: Int,
                        @SerializedName("pst")
                        val pst: Int,
                        @SerializedName("t")
                        val t: Int,
                        @SerializedName("ar")
                        val ar: List<Ar>,
                        @SerializedName("alia")
                        val alia: List<String>,
                        @SerializedName("pop")
                        val pop: Int,
                        @SerializedName("st")
                        val st: Int,
                        @SerializedName("rt")
                        val rt: String,
                        @SerializedName("fee")
                        val fee: Int,
                        @SerializedName("v")
                        val v: Int,
                        @SerializedName("crbt")
                        val crbt: Any,
                        @SerializedName("cf")
                        val cf: String,
                        @SerializedName("al")
                        val al: Al,
                        @SerializedName("dt")
                        val dt: Int,
                        @SerializedName("h")
                        val h: H,
                        @SerializedName("m")
                        val m: M,
                        @SerializedName("l")
                        val l: L,
                        @SerializedName("sq")
                        val sq: Sq,
                        @SerializedName("hr")
                        val hr: Hr,
                        @SerializedName("a")
                        val a: Any,
                        @SerializedName("cd")
                        val cd: String,
                        @SerializedName("no")
                        val no: Int,
                        @SerializedName("rtUrl")
                        val rtUrl: Any,
                        @SerializedName("ftype")
                        val ftype: Int,
                        @SerializedName("rtUrls")
                        val rtUrls: List<Any>,
                        @SerializedName("djId")
                        val djId: Int,
                        @SerializedName("copyright")
                        val copyright: Int,
                        @SerializedName("s_id")
                        val sId: Int,
                        @SerializedName("mark")
                        val mark: Long,
                        @SerializedName("originCoverType")
                        val originCoverType: Int,
                        @SerializedName("originSongSimpleData")
                        val originSongSimpleData: Any,
                        @SerializedName("tagPicList")
                        val tagPicList: Any,
                        @SerializedName("resourceState")
                        val resourceState: Boolean,
                        @SerializedName("version")
                        val version: Int,
                        @SerializedName("songJumpInfo")
                        val songJumpInfo: Any,
                        @SerializedName("entertainmentTags")
                        val entertainmentTags: Any,
                        @SerializedName("awardTags")
                        val awardTags: Any,
                        @SerializedName("displayTags")
                        val displayTags: Any,
                        @SerializedName("single")
                        val single: Int,
                        @SerializedName("noCopyrightRcmd")
                        val noCopyrightRcmd: Any,
                        @SerializedName("rtype")
                        val rtype: Int,
                        @SerializedName("rurl")
                        val rurl: Any,
                        @SerializedName("mst")
                        val mst: Int,
                        @SerializedName("cp")
                        val cp: Int,
                        @SerializedName("mv")
                        val mv: Int,
                        @SerializedName("publishTime")
                        val publishTime: Long,
                        @SerializedName("tns")
                        val tns: List<String>,
                        @SerializedName("privilege")
                        val privilege: Privilege,
                        @SerializedName("pc")
                        val pc: Pc
                    ) {
                        data class Ar(
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("tns")
                            val tns: List<Any>,
                            @SerializedName("alias")
                            val alias: List<Any>
                        )

                        data class Al(
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("picUrl")
                            val picUrl: String,
                            @SerializedName("tns")
                            val tns: List<Any>,
                            @SerializedName("pic_str")
                            val picStr: String,
                            @SerializedName("pic")
                            val pic: Long
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

                        data class Privilege(
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("fee")
                            val fee: Int,
                            @SerializedName("payed")
                            val payed: Int,
                            @SerializedName("realPayed")
                            val realPayed: Int,
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
                            @SerializedName("pc")
                            val pc: Pc,
                            @SerializedName("toast")
                            val toast: Boolean,
                            @SerializedName("flag")
                            val flag: Int,
                            @SerializedName("paidBigBang")
                            val paidBigBang: Boolean,
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
                            data class Pc(
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("userId")
                                val userId: Long,
                                @SerializedName("songId")
                                val songId: Int,
                                @SerializedName("md5")
                                val md5: String,
                                @SerializedName("song")
                                val song: String,
                                @SerializedName("artist")
                                val artist: String,
                                @SerializedName("album")
                                val album: String,
                                @SerializedName("bitrate")
                                val bitrate: Int,
                                @SerializedName("fileName")
                                val fileName: String,
                                @SerializedName("songDfsId")
                                val songDfsId: Int,
                                @SerializedName("cover")
                                val cover: Int,
                                @SerializedName("lyric")
                                val lyric: Int,
                                @SerializedName("cue")
                                val cue: Int,
                                @SerializedName("convertLyric")
                                val convertLyric: Int,
                                @SerializedName("version")
                                val version: Int,
                                @SerializedName("addTime")
                                val addTime: Long,
                                @SerializedName("fileSize")
                                val fileSize: Int,
                                @SerializedName("status")
                                val status: Int,
                                @SerializedName("originalAudioSongId")
                                val originalAudioSongId: Int,
                                @SerializedName("lrcType")
                                val lrcType: String
                            )

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

                        data class Pc(
                            @SerializedName("privateCloud")
                            val privateCloud: PrivateCloud,
                            @SerializedName("nickname")
                            val nickname: String,
                            @SerializedName("br")
                            val br: Int,
                            @SerializedName("ar")
                            val ar: String,
                            @SerializedName("alb")
                            val alb: String,
                            @SerializedName("uid")
                            val uid: Long,
                            @SerializedName("sn")
                            val sn: String,
                            @SerializedName("version")
                            val version: Int,
                            @SerializedName("fn")
                            val fn: String,
                            @SerializedName("cid")
                            val cid: String,
                            @SerializedName("cover")
                            val cover: Any
                        ) {
                            data class PrivateCloud(
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("userId")
                                val userId: Long,
                                @SerializedName("songId")
                                val songId: Int,
                                @SerializedName("md5")
                                val md5: String,
                                @SerializedName("song")
                                val song: String,
                                @SerializedName("artist")
                                val artist: String,
                                @SerializedName("album")
                                val album: String,
                                @SerializedName("bitrate")
                                val bitrate: Int,
                                @SerializedName("fileName")
                                val fileName: String,
                                @SerializedName("songDfsId")
                                val songDfsId: Int,
                                @SerializedName("cover")
                                val cover: Int,
                                @SerializedName("lyric")
                                val lyric: Int,
                                @SerializedName("cue")
                                val cue: Int,
                                @SerializedName("convertLyric")
                                val convertLyric: Int,
                                @SerializedName("version")
                                val version: Int,
                                @SerializedName("addTime")
                                val addTime: Long,
                                @SerializedName("fileSize")
                                val fileSize: Int,
                                @SerializedName("status")
                                val status: Int,
                                @SerializedName("originalAudioSongId")
                                val originalAudioSongId: Int,
                                @SerializedName("lrcType")
                                val lrcType: String
                            )
                        }
                    }

                    data class PubPlaylistData(
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("id")
                        val id: Long,
                        @SerializedName("trackNumberUpdateTime")
                        val trackNumberUpdateTime: Long,
                        @SerializedName("status")
                        val status: Int,
                        @SerializedName("userId")
                        val userId: Long,
                        @SerializedName("createTime")
                        val createTime: Long,
                        @SerializedName("updateTime")
                        val updateTime: Long,
                        @SerializedName("subscribedCount")
                        val subscribedCount: Int,
                        @SerializedName("trackCount")
                        val trackCount: Int,
                        @SerializedName("cloudTrackCount")
                        val cloudTrackCount: Int,
                        @SerializedName("coverImgUrl")
                        val coverImgUrl: String,
                        @SerializedName("iconImgUrl")
                        val iconImgUrl: Any,
                        @SerializedName("coverImgId")
                        val coverImgId: Long,
                        @SerializedName("description")
                        val description: String,
                        @SerializedName("tags")
                        val tags: List<String>,
                        @SerializedName("playCount")
                        val playCount: Int,
                        @SerializedName("trackUpdateTime")
                        val trackUpdateTime: Long,
                        @SerializedName("specialType")
                        val specialType: Int,
                        @SerializedName("totalDuration")
                        val totalDuration: Int,
                        @SerializedName("creator")
                        val creator: Creator,
                        @SerializedName("tracks")
                        val tracks: Any,
                        @SerializedName("subscribers")
                        val subscribers: List<Any>,
                        @SerializedName("subscribed")
                        val subscribed: Boolean,
                        @SerializedName("commentThreadId")
                        val commentThreadId: String,
                        @SerializedName("newImported")
                        val newImported: Boolean,
                        @SerializedName("adType")
                        val adType: Int,
                        @SerializedName("highQuality")
                        val highQuality: Boolean,
                        @SerializedName("privacy")
                        val privacy: Int,
                        @SerializedName("ordered")
                        val ordered: Boolean,
                        @SerializedName("anonimous")
                        val anonimous: Boolean,
                        @SerializedName("coverStatus")
                        val coverStatus: Int,
                        @SerializedName("recommendInfo")
                        val recommendInfo: Any,
                        @SerializedName("socialPlaylistCover")
                        val socialPlaylistCover: Any,
                        @SerializedName("recommendText")
                        val recommendText: Any,
                        @SerializedName("coverText")
                        val coverText: Any,
                        @SerializedName("relateResType")
                        val relateResType: Any,
                        @SerializedName("relateResId")
                        val relateResId: Any,
                        @SerializedName("tsSongCount")
                        val tsSongCount: Int,
                        @SerializedName("algType")
                        val algType: Any,
                        @SerializedName("playlistType")
                        val playlistType: String,
                        @SerializedName("uiPlaylistType")
                        val uiPlaylistType: String,
                        @SerializedName("originalCoverId")
                        val originalCoverId: Int,
                        @SerializedName("backgroundImageId")
                        val backgroundImageId: Int,
                        @SerializedName("backgroundImageUrl")
                        val backgroundImageUrl: Any,
                        @SerializedName("topTrackIds")
                        val topTrackIds: Any,
                        @SerializedName("title")
                        val title: Any,
                        @SerializedName("subTitle")
                        val subTitle: Any,
                        @SerializedName("backgroundText")
                        val backgroundText: Any,
                        @SerializedName("coverImgId_str")
                        val coverImgIdStr: String
                    ) {
                        data class Creator(
                            @SerializedName("defaultAvatar")
                            val defaultAvatar: Boolean,
                            @SerializedName("province")
                            val province: Int,
                            @SerializedName("authStatus")
                            val authStatus: Int,
                            @SerializedName("followed")
                            val followed: Boolean,
                            @SerializedName("avatarUrl")
                            val avatarUrl: String,
                            @SerializedName("accountStatus")
                            val accountStatus: Int,
                            @SerializedName("gender")
                            val gender: Int,
                            @SerializedName("city")
                            val city: Int,
                            @SerializedName("birthday")
                            val birthday: Long,
                            @SerializedName("userId")
                            val userId: Long,
                            @SerializedName("userType")
                            val userType: Int,
                            @SerializedName("nickname")
                            val nickname: String,
                            @SerializedName("signature")
                            val signature: String,
                            @SerializedName("description")
                            val description: String,
                            @SerializedName("detailDescription")
                            val detailDescription: String,
                            @SerializedName("avatarImgId")
                            val avatarImgId: Long,
                            @SerializedName("backgroundImgId")
                            val backgroundImgId: Long,
                            @SerializedName("backgroundUrl")
                            val backgroundUrl: String,
                            @SerializedName("authority")
                            val authority: Int,
                            @SerializedName("mutual")
                            val mutual: Boolean,
                            @SerializedName("expertTags")
                            val expertTags: Any,
                            @SerializedName("experts")
                            val experts: Any,
                            @SerializedName("djStatus")
                            val djStatus: Int,
                            @SerializedName("vipType")
                            val vipType: Int,
                            @SerializedName("remarkName")
                            val remarkName: Any,
                            @SerializedName("authenticationTypes")
                            val authenticationTypes: Int,
                            @SerializedName("avatarDetail")
                            val avatarDetail: Any,
                            @SerializedName("avatarImgIdStr")
                            val avatarImgIdStr: String,
                            @SerializedName("backgroundImgIdStr")
                            val backgroundImgIdStr: String,
                            @SerializedName("anchor")
                            val anchor: Boolean
                        )
                    }

                    data class PubDJRadioData(
                        @SerializedName("id")
                        val id: Int,
                        @SerializedName("dj")
                        val dj: Dj,
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("picUrl")
                        val picUrl: String,
                        @SerializedName("desc")
                        val desc: String,
                        @SerializedName("subCount")
                        val subCount: Int,
                        @SerializedName("programCount")
                        val programCount: Int,
                        @SerializedName("createTime")
                        val createTime: Long,
                        @SerializedName("categoryId")
                        val categoryId: Int,
                        @SerializedName("category")
                        val category: String,
                        @SerializedName("secondCategoryId")
                        val secondCategoryId: Int,
                        @SerializedName("secondCategory")
                        val secondCategory: String,
                        @SerializedName("radioFeeType")
                        val radioFeeType: Int,
                        @SerializedName("feeScope")
                        val feeScope: Int,
                        @SerializedName("buyed")
                        val buyed: Boolean,
                        @SerializedName("videos")
                        val videos: Any,
                        @SerializedName("finished")
                        val finished: Boolean,
                        @SerializedName("underShelf")
                        val underShelf: Boolean,
                        @SerializedName("purchaseCount")
                        val purchaseCount: Int,
                        @SerializedName("price")
                        val price: Int,
                        @SerializedName("originalPrice")
                        val originalPrice: Int,
                        @SerializedName("discountPrice")
                        val discountPrice: Any,
                        @SerializedName("lastProgramCreateTime")
                        val lastProgramCreateTime: Long,
                        @SerializedName("lastProgramName")
                        val lastProgramName: Any,
                        @SerializedName("lastProgramId")
                        val lastProgramId: Long,
                        @SerializedName("picId")
                        val picId: Long,
                        @SerializedName("rcmdText")
                        val rcmdText: String,
                        @SerializedName("hightQuality")
                        val hightQuality: Boolean,
                        @SerializedName("whiteList")
                        val whiteList: Boolean,
                        @SerializedName("liveInfo")
                        val liveInfo: Any,
                        @SerializedName("playCount")
                        val playCount: Int,
                        @SerializedName("icon")
                        val icon: Any,
                        @SerializedName("privacy")
                        val privacy: Boolean,
                        @SerializedName("intervenePicUrl")
                        val intervenePicUrl: String,
                        @SerializedName("intervenePicId")
                        val intervenePicId: Long,
                        @SerializedName("dynamic")
                        val `dynamic`: Boolean,
                        @SerializedName("shortName")
                        val shortName: Any,
                        @SerializedName("taskId")
                        val taskId: Int,
                        @SerializedName("manualTagsDTO")
                        val manualTagsDTO: ManualTagsDTO,
                        @SerializedName("scoreInfoDTO")
                        val scoreInfoDTO: Any,
                        @SerializedName("descPicList")
                        val descPicList: List<DescPic>,
                        @SerializedName("subed")
                        val subed: Boolean,
                        @SerializedName("original")
                        val original: String,
                        @SerializedName("replaceResource")
                        val replaceResource: Any,
                        @SerializedName("immersionCover")
                        val immersionCover: Any,
                        @SerializedName("immersionAnimation")
                        val immersionAnimation: Any,
                        @SerializedName("danmakuCount")
                        val danmakuCount: Int,
                        @SerializedName("deleted")
                        val deleted: Boolean,
                        @SerializedName("userDeleted")
                        val userDeleted: Boolean,
                        @SerializedName("onlySelfSee")
                        val onlySelfSee: Boolean,
                        @SerializedName("programOrder")
                        val programOrder: Int,
                        @SerializedName("specialType")
                        val specialType: Int,
                        @SerializedName("participateUidList")
                        val participateUidList: List<Any>,
                        @SerializedName("composeVideo")
                        val composeVideo: Boolean
                    ) {
                        data class Dj(
                            @SerializedName("defaultAvatar")
                            val defaultAvatar: Boolean,
                            @SerializedName("province")
                            val province: Int,
                            @SerializedName("authStatus")
                            val authStatus: Int,
                            @SerializedName("followed")
                            val followed: Boolean,
                            @SerializedName("avatarUrl")
                            val avatarUrl: String,
                            @SerializedName("accountStatus")
                            val accountStatus: Int,
                            @SerializedName("gender")
                            val gender: Int,
                            @SerializedName("city")
                            val city: Int,
                            @SerializedName("birthday")
                            val birthday: Long,
                            @SerializedName("userId")
                            val userId: Int,
                            @SerializedName("userType")
                            val userType: Int,
                            @SerializedName("nickname")
                            val nickname: String,
                            @SerializedName("signature")
                            val signature: String,
                            @SerializedName("description")
                            val description: String,
                            @SerializedName("detailDescription")
                            val detailDescription: String,
                            @SerializedName("avatarImgId")
                            val avatarImgId: Long,
                            @SerializedName("backgroundImgId")
                            val backgroundImgId: Long,
                            @SerializedName("backgroundUrl")
                            val backgroundUrl: String,
                            @SerializedName("authority")
                            val authority: Int,
                            @SerializedName("mutual")
                            val mutual: Boolean,
                            @SerializedName("expertTags")
                            val expertTags: Any,
                            @SerializedName("experts")
                            val experts: Any,
                            @SerializedName("djStatus")
                            val djStatus: Int,
                            @SerializedName("vipType")
                            val vipType: Int,
                            @SerializedName("remarkName")
                            val remarkName: Any,
                            @SerializedName("authenticationTypes")
                            val authenticationTypes: Int,
                            @SerializedName("avatarDetail")
                            val avatarDetail: Any,
                            @SerializedName("avatarImgIdStr")
                            val avatarImgIdStr: String,
                            @SerializedName("backgroundImgIdStr")
                            val backgroundImgIdStr: String,
                            @SerializedName("anchor")
                            val anchor: Boolean
                        )

                        data class ManualTagsDTO(
                            @SerializedName("themeDescTags")
                            val themeDescTags: Any,
                            @SerializedName("contentDescTags")
                            val contentDescTags: ContentDescTags,
                            @SerializedName("hotTags")
                            val hotTags: Any,
                            @SerializedName("brandColumnTags")
                            val brandColumnTags: Any
                        ) {
                            data class ContentDescTags(
                                @SerializedName("id")
                                val id: Int,
                                @SerializedName("tagGroupName")
                                val tagGroupName: Any,
                                @SerializedName("tagList")
                                val tagList: List<Tag>
                            ) {
                                data class Tag(
                                    @SerializedName("id")
                                    val id: Int,
                                    @SerializedName("tagName")
                                    val tagName: String,
                                    @SerializedName("tagImg")
                                    val tagImg: TagImg,
                                    @SerializedName("tagGuiding")
                                    val tagGuiding: Any,
                                    @SerializedName("jumpUrl")
                                    val jumpUrl: String,
                                    @SerializedName("display")
                                    val display: Boolean,
                                    @SerializedName("showPriority")
                                    val showPriority: Int
                                ) {
                                    data class TagImg(
                                        @SerializedName("tagImgUrl")
                                        val tagImgUrl: Any,
                                        @SerializedName("width")
                                        val width: Any,
                                        @SerializedName("height")
                                        val height: Any
                                    )
                                }
                            }
                        }

                        data class DescPic(
                            @SerializedName("type")
                            val type: Int,
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("content")
                            val content: String,
                            @SerializedName("height")
                            val height: Any,
                            @SerializedName("width")
                            val width: Any,
                            @SerializedName("timeStamp")
                            val timeStamp: Any,
                            @SerializedName("nestedData")
                            val nestedData: Any,
                            @SerializedName("imageContentURLInvalid")
                            val imageContentURLInvalid: Boolean
                        )
                    }

                    data class PubDJProgramData(
                        @SerializedName("mainSong")
                        val mainSong: MainSong,
                        @SerializedName("songs")
                        val songs: Any,
                        @SerializedName("dj")
                        val dj: Dj,
                        @SerializedName("participateAnchors")
                        val participateAnchors: Any,
                        @SerializedName("programGuests")
                        val programGuests: Any,
                        @SerializedName("blurCoverUrl")
                        val blurCoverUrl: String,
                        @SerializedName("radio")
                        val radio: Radio,
                        @SerializedName("subscribedCount")
                        val subscribedCount: Int,
                        @SerializedName("reward")
                        val reward: Boolean,
                        @SerializedName("mainTrackId")
                        val mainTrackId: Int,
                        @SerializedName("serialNum")
                        val serialNum: Long,
                        @SerializedName("listenerCount")
                        val listenerCount: Int,
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("id")
                        val id: Long,
                        @SerializedName("createTime")
                        val createTime: Long,
                        @SerializedName("description")
                        val description: String,
                        @SerializedName("userId")
                        val userId: Int,
                        @SerializedName("coverUrl")
                        val coverUrl: String,
                        @SerializedName("commentThreadId")
                        val commentThreadId: String,
                        @SerializedName("channels")
                        val channels: List<String>,
                        @SerializedName("titbits")
                        val titbits: Any,
                        @SerializedName("titbitImages")
                        val titbitImages: Any,
                        @SerializedName("pubStatus")
                        val pubStatus: Int,
                        @SerializedName("trackCount")
                        val trackCount: Int,
                        @SerializedName("bdAuditStatus")
                        val bdAuditStatus: Int,
                        @SerializedName("programFeeType")
                        val programFeeType: Int,
                        @SerializedName("buyed")
                        val buyed: Boolean,
                        @SerializedName("programDesc")
                        val programDesc: Any,
                        @SerializedName("h5Links")
                        val h5Links: List<Any>,
                        @SerializedName("coverId")
                        val coverId: Long,
                        @SerializedName("adjustedPlayCount")
                        val adjustedPlayCount: Int,
                        @SerializedName("canReward")
                        val canReward: Boolean,
                        @SerializedName("auditStatus")
                        val auditStatus: Int,
                        @SerializedName("updateTime")
                        val updateTime: Long,
                        @SerializedName("categoryId")
                        val categoryId: Int,
                        @SerializedName("category")
                        val category: Any,
                        @SerializedName("secondCategoryId")
                        val secondCategoryId: Int,
                        @SerializedName("secondCategory")
                        val secondCategory: Any,
                        @SerializedName("scheduledPublishTime")
                        val scheduledPublishTime: Long,
                        @SerializedName("privacy")
                        val privacy: Boolean,
                        @SerializedName("disPlayStatus")
                        val disPlayStatus: String,
                        @SerializedName("createEventId")
                        val createEventId: Int,
                        @SerializedName("djPlayRecordVo")
                        val djPlayRecordVo: Any,
                        @SerializedName("playRecordOs")
                        val playRecordOs: Any,
                        @SerializedName("shortName")
                        val shortName: Any,
                        @SerializedName("price")
                        val price: Any,
                        @SerializedName("latestFreeTryStartPoint")
                        val latestFreeTryStartPoint: Any,
                        @SerializedName("latestFreeTryEndPoint")
                        val latestFreeTryEndPoint: Any,
                        @SerializedName("algReason")
                        val algReason: Any,
                        @SerializedName("showAlgReason")
                        val showAlgReason: Any,
                        @SerializedName("icon")
                        val icon: Any,
                        @SerializedName("replaceResource")
                        val replaceResource: Any,
                        @SerializedName("songTimeStamps")
                        val songTimeStamps: Any,
                        @SerializedName("classicRelationSong")
                        val classicRelationSong: Any,
                        @SerializedName("specialTags")
                        val specialTags: Any,
                        @SerializedName("playFlag")
                        val playFlag: Any,
                        @SerializedName("specialType")
                        val specialType: Int,
                        @SerializedName("programSource")
                        val programSource: Any,
                        @SerializedName("ttsAddContent")
                        val ttsAddContent: Any,
                        @SerializedName("resourceId")
                        val resourceId: Any,
                        @SerializedName("commonModule")
                        val commonModule: Any,
                        @SerializedName("showNote")
                        val showNote: Any,
                        @SerializedName("publish")
                        val publish: Boolean,
                        @SerializedName("duration")
                        val duration: Int,
                        @SerializedName("shareCount")
                        val shareCount: Int,
                        @SerializedName("subscribed")
                        val subscribed: Boolean,
                        @SerializedName("likedCount")
                        val likedCount: Int,
                        @SerializedName("commentCount")
                        val commentCount: Int
                    ) {
                        data class MainSong(
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("position")
                            val position: Int,
                            @SerializedName("alias")
                            val alias: List<Any>,
                            @SerializedName("status")
                            val status: Int,
                            @SerializedName("fee")
                            val fee: Int,
                            @SerializedName("copyrightId")
                            val copyrightId: Int,
                            @SerializedName("disc")
                            val disc: String,
                            @SerializedName("no")
                            val no: Int,
                            @SerializedName("artists")
                            val artists: List<Artist>,
                            @SerializedName("album")
                            val album: Album,
                            @SerializedName("starred")
                            val starred: Boolean,
                            @SerializedName("popularity")
                            val popularity: Int,
                            @SerializedName("score")
                            val score: Int,
                            @SerializedName("starredNum")
                            val starredNum: Int,
                            @SerializedName("duration")
                            val duration: Int,
                            @SerializedName("playedNum")
                            val playedNum: Int,
                            @SerializedName("dayPlays")
                            val dayPlays: Int,
                            @SerializedName("hearTime")
                            val hearTime: Int,
                            @SerializedName("sqMusic")
                            val sqMusic: Any,
                            @SerializedName("hrMusic")
                            val hrMusic: Any,
                            @SerializedName("ringtone")
                            val ringtone: String,
                            @SerializedName("crbt")
                            val crbt: Any,
                            @SerializedName("audition")
                            val audition: Any,
                            @SerializedName("copyFrom")
                            val copyFrom: String,
                            @SerializedName("commentThreadId")
                            val commentThreadId: String,
                            @SerializedName("rtUrl")
                            val rtUrl: Any,
                            @SerializedName("ftype")
                            val ftype: Int,
                            @SerializedName("rtUrls")
                            val rtUrls: List<Any>,
                            @SerializedName("copyright")
                            val copyright: Int,
                            @SerializedName("transName")
                            val transName: Any,
                            @SerializedName("sign")
                            val sign: Any,
                            @SerializedName("mark")
                            val mark: Int,
                            @SerializedName("originCoverType")
                            val originCoverType: Int,
                            @SerializedName("originSongSimpleData")
                            val originSongSimpleData: Any,
                            @SerializedName("single")
                            val single: Int,
                            @SerializedName("noCopyrightRcmd")
                            val noCopyrightRcmd: Any,
                            @SerializedName("rtype")
                            val rtype: Int,
                            @SerializedName("rurl")
                            val rurl: Any,
                            @SerializedName("mvid")
                            val mvid: Int,
                            @SerializedName("hMusic")
                            val hMusic: HMusic,
                            @SerializedName("mMusic")
                            val mMusic: MMusic,
                            @SerializedName("lMusic")
                            val lMusic: LMusic,
                            @SerializedName("bMusic")
                            val bMusic: BMusic,
                            @SerializedName("mp3Url")
                            val mp3Url: Any
                        ) {
                            data class Artist(
                                @SerializedName("name")
                                val name: String,
                                @SerializedName("id")
                                val id: Int,
                                @SerializedName("picId")
                                val picId: Int,
                                @SerializedName("img1v1Id")
                                val img1v1Id: Int,
                                @SerializedName("briefDesc")
                                val briefDesc: String,
                                @SerializedName("picUrl")
                                val picUrl: String,
                                @SerializedName("img1v1Url")
                                val img1v1Url: String,
                                @SerializedName("albumSize")
                                val albumSize: Int,
                                @SerializedName("alias")
                                val alias: List<Any>,
                                @SerializedName("trans")
                                val trans: String,
                                @SerializedName("musicSize")
                                val musicSize: Int,
                                @SerializedName("topicPerson")
                                val topicPerson: Int
                            )

                            data class Album(
                                @SerializedName("name")
                                val name: String,
                                @SerializedName("id")
                                val id: Int,
                                @SerializedName("type")
                                val type: Any,
                                @SerializedName("size")
                                val size: Int,
                                @SerializedName("picId")
                                val picId: Int,
                                @SerializedName("blurPicUrl")
                                val blurPicUrl: Any,
                                @SerializedName("companyId")
                                val companyId: Int,
                                @SerializedName("pic")
                                val pic: Int,
                                @SerializedName("picUrl")
                                val picUrl: String,
                                @SerializedName("publishTime")
                                val publishTime: Int,
                                @SerializedName("description")
                                val description: String,
                                @SerializedName("tags")
                                val tags: String,
                                @SerializedName("company")
                                val company: Any,
                                @SerializedName("briefDesc")
                                val briefDesc: String,
                                @SerializedName("artist")
                                val artist: Artist,
                                @SerializedName("songs")
                                val songs: List<Any>,
                                @SerializedName("alias")
                                val alias: List<Any>,
                                @SerializedName("status")
                                val status: Int,
                                @SerializedName("copyrightId")
                                val copyrightId: Int,
                                @SerializedName("commentThreadId")
                                val commentThreadId: String,
                                @SerializedName("artists")
                                val artists: List<Artist>,
                                @SerializedName("subType")
                                val subType: Any,
                                @SerializedName("transName")
                                val transName: Any,
                                @SerializedName("onSale")
                                val onSale: Boolean,
                                @SerializedName("mark")
                                val mark: Int,
                                @SerializedName("gapless")
                                val gapless: Int,
                                @SerializedName("dolbyMark")
                                val dolbyMark: Int
                            ) {
                                data class Artist(
                                    @SerializedName("name")
                                    val name: String,
                                    @SerializedName("id")
                                    val id: Int,
                                    @SerializedName("picId")
                                    val picId: Int,
                                    @SerializedName("img1v1Id")
                                    val img1v1Id: Int,
                                    @SerializedName("briefDesc")
                                    val briefDesc: String,
                                    @SerializedName("picUrl")
                                    val picUrl: String,
                                    @SerializedName("img1v1Url")
                                    val img1v1Url: String,
                                    @SerializedName("albumSize")
                                    val albumSize: Int,
                                    @SerializedName("alias")
                                    val alias: List<Any>,
                                    @SerializedName("trans")
                                    val trans: String,
                                    @SerializedName("musicSize")
                                    val musicSize: Int,
                                    @SerializedName("topicPerson")
                                    val topicPerson: Int
                                )


                            }

                            data class HMusic(
                                @SerializedName("name")
                                val name: Any,
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("size")
                                val size: Int,
                                @SerializedName("extension")
                                val extension: String,
                                @SerializedName("sr")
                                val sr: Int,
                                @SerializedName("dfsId")
                                val dfsId: Int,
                                @SerializedName("bitrate")
                                val bitrate: Int,
                                @SerializedName("playTime")
                                val playTime: Int,
                                @SerializedName("volumeDelta")
                                val volumeDelta: Int
                            )

                            data class MMusic(
                                @SerializedName("name")
                                val name: Any,
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("size")
                                val size: Int,
                                @SerializedName("extension")
                                val extension: String,
                                @SerializedName("sr")
                                val sr: Int,
                                @SerializedName("dfsId")
                                val dfsId: Int,
                                @SerializedName("bitrate")
                                val bitrate: Int,
                                @SerializedName("playTime")
                                val playTime: Int,
                                @SerializedName("volumeDelta")
                                val volumeDelta: Int
                            )

                            data class LMusic(
                                @SerializedName("name")
                                val name: Any,
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("size")
                                val size: Int,
                                @SerializedName("extension")
                                val extension: String,
                                @SerializedName("sr")
                                val sr: Int,
                                @SerializedName("dfsId")
                                val dfsId: Int,
                                @SerializedName("bitrate")
                                val bitrate: Int,
                                @SerializedName("playTime")
                                val playTime: Int,
                                @SerializedName("volumeDelta")
                                val volumeDelta: Int
                            )

                            data class BMusic(
                                @SerializedName("name")
                                val name: Any,
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("size")
                                val size: Int,
                                @SerializedName("extension")
                                val extension: String,
                                @SerializedName("sr")
                                val sr: Int,
                                @SerializedName("dfsId")
                                val dfsId: Int,
                                @SerializedName("bitrate")
                                val bitrate: Int,
                                @SerializedName("playTime")
                                val playTime: Int,
                                @SerializedName("volumeDelta")
                                val volumeDelta: Int
                            )
                        }

                        data class Dj(
                            @SerializedName("defaultAvatar")
                            val defaultAvatar: Boolean,
                            @SerializedName("province")
                            val province: Int,
                            @SerializedName("authStatus")
                            val authStatus: Int,
                            @SerializedName("followed")
                            val followed: Boolean,
                            @SerializedName("avatarUrl")
                            val avatarUrl: String,
                            @SerializedName("accountStatus")
                            val accountStatus: Int,
                            @SerializedName("gender")
                            val gender: Int,
                            @SerializedName("city")
                            val city: Int,
                            @SerializedName("birthday")
                            val birthday: Long,
                            @SerializedName("userId")
                            val userId: Int,
                            @SerializedName("userType")
                            val userType: Int,
                            @SerializedName("nickname")
                            val nickname: String,
                            @SerializedName("signature")
                            val signature: String,
                            @SerializedName("description")
                            val description: String,
                            @SerializedName("detailDescription")
                            val detailDescription: String,
                            @SerializedName("avatarImgId")
                            val avatarImgId: Long,
                            @SerializedName("backgroundImgId")
                            val backgroundImgId: Long,
                            @SerializedName("backgroundUrl")
                            val backgroundUrl: String,
                            @SerializedName("authority")
                            val authority: Int,
                            @SerializedName("mutual")
                            val mutual: Boolean,
                            @SerializedName("expertTags")
                            val expertTags: Any,
                            @SerializedName("experts")
                            val experts: Any,
                            @SerializedName("djStatus")
                            val djStatus: Int,
                            @SerializedName("vipType")
                            val vipType: Int,
                            @SerializedName("remarkName")
                            val remarkName: Any,
                            @SerializedName("authenticationTypes")
                            val authenticationTypes: Int,
                            @SerializedName("avatarDetail")
                            val avatarDetail: Any,
                            @SerializedName("avatarImgIdStr")
                            val avatarImgIdStr: String,
                            @SerializedName("backgroundImgIdStr")
                            val backgroundImgIdStr: String,
                            @SerializedName("anchor")
                            val anchor: Boolean,
                            @SerializedName("brand")
                            val brand: String
                        )

                        data class Radio(
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("dj")
                            val dj: Any,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("picUrl")
                            val picUrl: String,
                            @SerializedName("desc")
                            val desc: String,
                            @SerializedName("subCount")
                            val subCount: Int,
                            @SerializedName("programCount")
                            val programCount: Int,
                            @SerializedName("createTime")
                            val createTime: Long,
                            @SerializedName("categoryId")
                            val categoryId: Int,
                            @SerializedName("category")
                            val category: String,
                            @SerializedName("secondCategoryId")
                            val secondCategoryId: Int,
                            @SerializedName("secondCategory")
                            val secondCategory: String,
                            @SerializedName("radioFeeType")
                            val radioFeeType: Int,
                            @SerializedName("feeScope")
                            val feeScope: Int,
                            @SerializedName("buyed")
                            val buyed: Boolean,
                            @SerializedName("videos")
                            val videos: Any,
                            @SerializedName("finished")
                            val finished: Boolean,
                            @SerializedName("underShelf")
                            val underShelf: Boolean,
                            @SerializedName("purchaseCount")
                            val purchaseCount: Int,
                            @SerializedName("price")
                            val price: Int,
                            @SerializedName("originalPrice")
                            val originalPrice: Int,
                            @SerializedName("discountPrice")
                            val discountPrice: Any,
                            @SerializedName("lastProgramCreateTime")
                            val lastProgramCreateTime: Long,
                            @SerializedName("lastProgramName")
                            val lastProgramName: Any,
                            @SerializedName("lastProgramId")
                            val lastProgramId: Long,
                            @SerializedName("picId")
                            val picId: Long,
                            @SerializedName("rcmdText")
                            val rcmdText: String,
                            @SerializedName("hightQuality")
                            val hightQuality: Boolean,
                            @SerializedName("whiteList")
                            val whiteList: Boolean,
                            @SerializedName("liveInfo")
                            val liveInfo: Any,
                            @SerializedName("playCount")
                            val playCount: Int,
                            @SerializedName("icon")
                            val icon: Any,
                            @SerializedName("privacy")
                            val privacy: Boolean,
                            @SerializedName("intervenePicUrl")
                            val intervenePicUrl: String,
                            @SerializedName("intervenePicId")
                            val intervenePicId: Long,
                            @SerializedName("dynamic")
                            val `dynamic`: Boolean,
                            @SerializedName("shortName")
                            val shortName: Any,
                            @SerializedName("taskId")
                            val taskId: Int,
                            @SerializedName("manualTagsDTO")
                            val manualTagsDTO: Any,
                            @SerializedName("scoreInfoDTO")
                            val scoreInfoDTO: Any,
                            @SerializedName("descPicList")
                            val descPicList: Any,
                            @SerializedName("subed")
                            val subed: Boolean,
                            @SerializedName("original")
                            val original: String,
                            @SerializedName("replaceResource")
                            val replaceResource: Any,
                            @SerializedName("immersionCover")
                            val immersionCover: Any,
                            @SerializedName("immersionAnimation")
                            val immersionAnimation: Any,
                            @SerializedName("danmakuCount")
                            val danmakuCount: Int,
                            @SerializedName("deleted")
                            val deleted: Boolean,
                            @SerializedName("userDeleted")
                            val userDeleted: Boolean,
                            @SerializedName("onlySelfSee")
                            val onlySelfSee: Boolean,
                            @SerializedName("programOrder")
                            val programOrder: Int,
                            @SerializedName("specialType")
                            val specialType: Int,
                            @SerializedName("participateUidList")
                            val participateUidList: List<Any>,
                            @SerializedName("composeVideo")
                            val composeVideo: Boolean
                        )
                    }

                    data class PubUserProfileData(
                        @SerializedName("defaultAvatar")
                        val defaultAvatar: Boolean,
                        @SerializedName("province")
                        val province: Int,
                        @SerializedName("authStatus")
                        val authStatus: Int,
                        @SerializedName("followed")
                        val followed: Boolean,
                        @SerializedName("avatarUrl")
                        val avatarUrl: String,
                        @SerializedName("accountStatus")
                        val accountStatus: Int,
                        @SerializedName("gender")
                        val gender: Int,
                        @SerializedName("city")
                        val city: Int,
                        @SerializedName("birthday")
                        val birthday: Int,
                        @SerializedName("userId")
                        val userId: Long,
                        @SerializedName("userType")
                        val userType: Int,
                        @SerializedName("nickname")
                        val nickname: String,
                        @SerializedName("signature")
                        val signature: String,
                        @SerializedName("description")
                        val description: String,
                        @SerializedName("detailDescription")
                        val detailDescription: String,
                        @SerializedName("avatarImgId")
                        val avatarImgId: Long,
                        @SerializedName("backgroundImgId")
                        val backgroundImgId: Long,
                        @SerializedName("backgroundUrl")
                        val backgroundUrl: String,
                        @SerializedName("authority")
                        val authority: Int,
                        @SerializedName("mutual")
                        val mutual: Boolean,
                        @SerializedName("expertTags")
                        val expertTags: Any,
                        @SerializedName("experts")
                        val experts: Any,
                        @SerializedName("djStatus")
                        val djStatus: Int,
                        @SerializedName("vipType")
                        val vipType: Int,
                        @SerializedName("remarkName")
                        val remarkName: Any,
                        @SerializedName("authenticationTypes")
                        val authenticationTypes: Int,
                        @SerializedName("avatarDetail")
                        val avatarDetail: AvatarDetail,
                        @SerializedName("avatarImgIdStr")
                        val avatarImgIdStr: String,
                        @SerializedName("backgroundImgIdStr")
                        val backgroundImgIdStr: String,
                        @SerializedName("anchor")
                        val anchor: Boolean
                    ) {
                        data class AvatarDetail(
                            @SerializedName("userType")
                            val userType: Int,
                            @SerializedName("identityLevel")
                            val identityLevel: Int,
                            @SerializedName("identityIconUrl")
                            val identityIconUrl: String
                        )
                    }
                }

                data class ExtInfo(
                    @SerializedName("accountId")
                    val accountId: Int,
                    @SerializedName("fansSize")
                    val fansSize: Int,
                    @SerializedName("showDesc")
                    val showDesc: String,
                    @SerializedName("iconUrl")
                    val iconUrl: String,
                    @SerializedName("nickname")
                    val nickname: String,
                    @SerializedName("avatarUrl")
                    val avatarUrl: String,
                    @SerializedName("occupation")
                    val occupation: String,
                    @SerializedName("followed")
                    val followed: Boolean,
                    @SerializedName("fansGroup")
                    val fansGroup: Any,
                    @SerializedName("liveInfo")
                    val liveInfo: Any,
                    @SerializedName("liveRightButton")
                    val liveRightButton: Any,
                    @SerializedName("artistAlias")
                    val artistAlias: Any,
                    @SerializedName("appendRecText")
                    val appendRecText: String,
                    @SerializedName("recommendText")
                    val recommendText: String,
                    @SerializedName("officialTags")
                    val officialTags: List<String>,
                    @SerializedName("specialTags")
                    val specialTags: List<Any>,
                    @SerializedName("overrideTitle")
                    val overrideTitle: Any,
                    @SerializedName("overrideSubTitle")
                    val overrideSubTitle: Any,
                    @SerializedName("overrideImageType")
                    val overrideImageType: String,
                    @SerializedName("overrideImageUrl")
                    val overrideImageUrl: String,
                    @SerializedName("algClickableTags")
                    val algClickableTags: List<AlgClickableTag>,
                    @SerializedName("songAlias")
                    val songAlias: String,
                    @SerializedName("artistTns")
                    val artistTns: String,
                    @SerializedName("lyrics")
                    val lyrics: Lyrics,
                    @SerializedName("songCreator")
                    val songCreator: Any,
                    @SerializedName("memberGuidanceInfo")
                    val memberGuidanceInfo: Any,
                    @SerializedName("noCopyRight")
                    val noCopyRight: Boolean,
                    @SerializedName("hasNoCopyrightRcmd")
                    val hasNoCopyrightRcmd: Boolean,
                    @SerializedName("noCopyrightRcmdStyle")
                    val noCopyrightRcmdStyle: Int,
                    @SerializedName("payType")
                    val payType: Any,
                    @SerializedName("albumUrl")
                    val albumUrl: Any,
                    @SerializedName("algAlbumName")
                    val algAlbumName: Any,
                    @SerializedName("resourceHotExplainDTO")
                    val resourceHotExplainDTO: Any,
                    @SerializedName("showVideoTip")
                    val showVideoTip: Boolean,
                    @SerializedName("tsShowFlag")
                    val tsShowFlag: Boolean,
                    @SerializedName("starCount")
                    val starCount: Int,
                    @SerializedName("stared")
                    val stared: Boolean,
                    @SerializedName("scoreDto")
                    val scoreDto: ScoreDto,
                    @SerializedName("playlistType")
                    val playlistType: String,
                    @SerializedName("officialPlaylistTitle")
                    val officialPlaylistTitle: Any,
                    @SerializedName("tagImage")
                    val tagImage: Any,
                    @SerializedName("rightLabelText")
                    val rightLabelText: String,
                    @SerializedName("labels")
                    val labels: List<Label>,
                    @SerializedName("rcmdReason")
                    val rcmdReason: Any,
                    @SerializedName("goldSong")
                    val goldSong: Boolean,
                    @SerializedName("identityName")
                    val identityName: Any,
                    @SerializedName("identityUserType")
                    val identityUserType: Any,
                    @SerializedName("expertCollectNum")
                    val expertCollectNum: Any
                ) {
                    data class AlgClickableTag(
                        @SerializedName("clickable")
                        val clickable: Boolean,
                        @SerializedName("boardId")
                        val boardId: Any,
                        @SerializedName("text")
                        val text: String,
                        @SerializedName("url")
                        val url: String,
                        @SerializedName("reasonId")
                        val reasonId: String,
                        @SerializedName("reasonType")
                        val reasonType: Int,
                        @SerializedName("reasonTag")
                        val reasonTag: String,
                        @SerializedName("sceneTag")
                        val sceneTag: String,
                        @SerializedName("resourceId")
                        val resourceId: String
                    )

                    class Lyrics

                    data class ScoreDto(
                        @SerializedName("score")
                        val score: String,
                        @SerializedName("recommendWord")
                        val recommendWord: String
                    )

                    data class Label(
                        @SerializedName("text")
                        val text: String,
                        @SerializedName("texts")
                        val texts: List<String>,
                        @SerializedName("colour")
                        val colour: String,
                        @SerializedName("showType")
                        val showType: String,
                        @SerializedName("labelSource")
                        val labelSource: String,
                        @SerializedName("tagImage")
                        val tagImage: Any,
                        @SerializedName("activeLink")
                        val activeLink: Any,
                        @SerializedName("activeType")
                        val activeType: Any
                    )
                }
            }

            data class More(
                @SerializedName("action")
                val action: String,
                @SerializedName("actionUrl")
                val actionUrl: Any,
                @SerializedName("actionType")
                val actionType: String,
                @SerializedName("totalHit")
                val totalHit: Any,
                @SerializedName("text")
                val text: String,
                @SerializedName("highText")
                val highText: String,
                @SerializedName("iconUrl")
                val iconUrl: Any
            )

            data class ExtInfo(
                @SerializedName("hideSeparator")
                val hideSeparator: Boolean,
                @SerializedName("overrideTitle")
                val overrideTitle: String,
                @SerializedName("overrideSubTitle")
                val overrideSubTitle: Any,
                @SerializedName("overrideButton")
                val overrideButton: Any
            )
        }

        data class SceneTransmissionInfo(
            @SerializedName("moreSongRcmd")
            val moreSongRcmd: String
        )
    }

    data class Trp(
        @SerializedName("rules")
        val rules: List<String>
    )
}