package com.ljyh.music.data.model
import com.google.gson.annotations.SerializedName


data class HomePage(
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
        @SerializedName("blockCodeOrderList")
        val blockCodeOrderList: Any,
        @SerializedName("blockUUIDs")
        val blockUUIDs: Any,
        @SerializedName("blocks")
        val blocks: List<Block>,
        @SerializedName("cursor")
        val cursor: String,
        @SerializedName("demote")
        val demote: Boolean,
        @SerializedName("exposedResource")
        val exposedResource: String,
        @SerializedName("guideToast")
        val guideToast: GuideToast,
        @SerializedName("hasMore")
        val hasMore: Boolean,
        @SerializedName("internalTest")
        val internalTest: Any,
        @SerializedName("pageConfig")
        val pageConfig: PageConfig,
        @SerializedName("titles")
        val titles: List<Any>
    ) {
        data class Block(
            @SerializedName("action")
            val action: String,
            @SerializedName("actionType")
            val actionType: String,
            @SerializedName("blockCode")
            val blockCode: String,
            @SerializedName("blockDemote")
            val blockDemote: Boolean,
            @SerializedName("blockStyle")
            val blockStyle: Int,
            @SerializedName("canClose")
            val canClose: Boolean,
            @SerializedName("canFeedback")
            val canFeedback: Boolean,
            @SerializedName("creatives")
            val creatives: List<Creative>,
            @SerializedName("crossPlatformConfig")
            val crossPlatformConfig: CrossPlatformConfig,
            @SerializedName("dislikeShowType")
            val dislikeShowType: Int,
            @SerializedName("extInfo")
            val extInfo: ExtInfo,
            @SerializedName("resourceIdList")
            val resourceIdList: List<String>,
            @SerializedName("showType")
            val showType: String,
            @SerializedName("sort")
            val sort: Int,
            @SerializedName("uiElement")
            val uiElement: UiElement
        ) {
            data class Creative(
                @SerializedName("action")
                val action: String,
                @SerializedName("actionType")
                val actionType: String,
                @SerializedName("alg")
                val alg: String,
                @SerializedName("creativeExtInfoVO")
                val creativeExtInfoVO: CreativeExtInfoVO,
                @SerializedName("creativeId")
                val creativeId: String,
                @SerializedName("creativeType")
                val creativeType: String,
                @SerializedName("logInfo")
                val logInfo: String,
                @SerializedName("position")
                val position: Int,
                @SerializedName("resources")
                val resources: List<Resource>,
                @SerializedName("uiElement")
                val uiElement: UiElement
            ) {
                data class CreativeExtInfoVO(
                    @SerializedName("cursor")
                    val cursor: String
                )

                data class Resource(
                    @SerializedName("action")
                    val action: String,
                    @SerializedName("actionType")
                    val actionType: String,
                    @SerializedName("alg")
                    val alg: String,
                    @SerializedName("ctrp")
                    val ctrp: String,
                    @SerializedName("likedCount")
                    val likedCount: Any,
                    @SerializedName("logInfo")
                    val logInfo: String,
                    @SerializedName("playParams")
                    val playParams: PlayParams,
                    @SerializedName("position")
                    val position: Int,
                    @SerializedName("replyCount")
                    val replyCount: Any,
                    @SerializedName("resourceContentList")
                    val resourceContentList: Any,
                    @SerializedName("resourceExtInfo")
                    val resourceExtInfo: ResourceExtInfo,
                    @SerializedName("resourceId")
                    val resourceId: String,
                    @SerializedName("resourceState")
                    val resourceState: Any,
                    @SerializedName("resourceType")
                    val resourceType: String,
                    @SerializedName("resourceUrl")
                    val resourceUrl: String,
                    @SerializedName("uiElement")
                    val uiElement: UiElement,
                    @SerializedName("valid")
                    val valid: Boolean
                ) {
                    data class PlayParams(
                        @SerializedName("playerType")
                        val playerType: String,
                        @SerializedName("resourceIds")
                        val resourceIds: List<String>,
                        @SerializedName("showUI")
                        val showUI: Boolean
                    )

                    data class ResourceExtInfo(
                        @SerializedName("alias")
                        val alias: String,
                        @SerializedName("artists")
                        val artists: List<Artist>,
                        @SerializedName("commentSimpleData")
                        val commentSimpleData: CommentSimpleData,
                        @SerializedName("djProgram")
                        val djProgram: DjProgram,
                        @SerializedName("hasListened")
                        val hasListened: Boolean,
                        @SerializedName("highQuality")
                        val highQuality: Boolean,
                        @SerializedName("playCount")
                        val playCount: Long,
                        @SerializedName("song")
                        val song: Song,
                        @SerializedName("songData")
                        val songData: SongData,
                        @SerializedName("songDatas")
                        val songDatas: List<SongData>,
                        @SerializedName("songPrivilege")
                        val songPrivilege: SongPrivilege,
                        @SerializedName("specialType")
                        val specialType: Int,
                        @SerializedName("voiceRelatedSongName")
                        val voiceRelatedSongName: String,
                        @SerializedName("voiceRelatedSongNames")
                        val voiceRelatedSongNames: List<String>,
                        @SerializedName("voiceRelatedSongSize")
                        val voiceRelatedSongSize: Int
                    ) {
                        data class Artist(
                            @SerializedName("albumSize")
                            val albumSize: Int,
                            @SerializedName("alias")
                            val alias: List<Any>,
                            @SerializedName("briefDesc")
                            val briefDesc: String,
                            @SerializedName("id")
                            val id: Long,
                            @SerializedName("img1v1Id")
                            val img1v1Id: Long,
                            @SerializedName("img1v1Url")
                            val img1v1Url: String,
                            @SerializedName("musicSize")
                            val musicSize: Int,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("picId")
                            val picId: Long,
                            @SerializedName("picUrl")
                            val picUrl: String,
                            @SerializedName("topicPerson")
                            val topicPerson: Int,
                            @SerializedName("trans")
                            val trans: String
                        )

                        data class CommentSimpleData(
                            @SerializedName("commentId")
                            val commentId: Long,
                            @SerializedName("content")
                            val content: String,
                            @SerializedName("threadId")
                            val threadId: String,
                            @SerializedName("userId")
                            val userId: Long,
                            @SerializedName("userName")
                            val userName: String
                        )

                        data class DjProgram(
                            @SerializedName("adjustedPlayCount")
                            val adjustedPlayCount: Int,
                            @SerializedName("auditStatus")
                            val auditStatus: Int,
                            @SerializedName("bdAuditStatus")
                            val bdAuditStatus: Int,
                            @SerializedName("blurCoverUrl")
                            val blurCoverUrl: Any,
                            @SerializedName("buyed")
                            val buyed: Boolean,
                            @SerializedName("canReward")
                            val canReward: Boolean,
                            @SerializedName("category")
                            val category: String,
                            @SerializedName("categoryId")
                            val categoryId: Long,
                            @SerializedName("channels")
                            val channels: List<String>,
                            @SerializedName("commentCount")
                            val commentCount: Int,
                            @SerializedName("commentThreadId")
                            val commentThreadId: String,
                            @SerializedName("coverId")
                            val coverId: Long,
                            @SerializedName("coverUrl")
                            val coverUrl: String,
                            @SerializedName("createEventId")
                            val createEventId: Long,
                            @SerializedName("createTime")
                            val createTime: Long,
                            @SerializedName("description")
                            val description: String,
                            @SerializedName("disPlayStatus")
                            val disPlayStatus: String,
                            @SerializedName("dj")
                            val dj: Dj,
                            @SerializedName("djPlayRecordVo")
                            val djPlayRecordVo: Any,
                            @SerializedName("duration")
                            val duration: Int,
                            @SerializedName("h5Links")
                            val h5Links: List<Any>,
                            @SerializedName("id")
                            val id: Long,
                            @SerializedName("likedCount")
                            val likedCount: Int,
                            @SerializedName("listenerCount")
                            val listenerCount: Int,
                            @SerializedName("mainSong")
                            val mainSong: MainSong,
                            @SerializedName("mainTrackId")
                            val mainTrackId: Long,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("privacy")
                            val privacy: Boolean,
                            @SerializedName("programDesc")
                            val programDesc: Any,
                            @SerializedName("programFeeType")
                            val programFeeType: Int,
                            @SerializedName("pubStatus")
                            val pubStatus: Int,
                            @SerializedName("publish")
                            val publish: Boolean,
                            @SerializedName("radio")
                            val radio: Radio,
                            @SerializedName("reward")
                            val reward: Boolean,
                            @SerializedName("scheduledPublishTime")
                            val scheduledPublishTime: Long,
                            @SerializedName("secondCategory")
                            val secondCategory: String,
                            @SerializedName("secondCategoryId")
                            val secondCategoryId: Long,
                            @SerializedName("serialNum")
                            val serialNum: Int,
                            @SerializedName("shareCount")
                            val shareCount: Int,
                            @SerializedName("shortName")
                            val shortName: String,
                            @SerializedName("songs")
                            val songs: Any,
                            @SerializedName("subscribedCount")
                            val subscribedCount: Int,
                            @SerializedName("titbitImages")
                            val titbitImages: Any,
                            @SerializedName("titbits")
                            val titbits: Any,
                            @SerializedName("trackCount")
                            val trackCount: Int,
                            @SerializedName("updateTime")
                            val updateTime: Long,
                            @SerializedName("userId")
                            val userId: Long
                        ) {
                            data class Dj(
                                @SerializedName("accountStatus")
                                val accountStatus: Int,
                                @SerializedName("anchor")
                                val anchor: Boolean,
                                @SerializedName("authStatus")
                                val authStatus: Int,
                                @SerializedName("authenticationTypes")
                                val authenticationTypes: Int,
                                @SerializedName("authority")
                                val authority: Int,
                                @SerializedName("avatarDetail")
                                val avatarDetail: Any,
                                @SerializedName("avatarImgId")
                                val avatarImgId: Long,
                                @SerializedName("avatarImgIdStr")
                                val avatarImgIdStr: String,
                                @SerializedName("avatarUrl")
                                val avatarUrl: String,
                                @SerializedName("backgroundImgId")
                                val backgroundImgId: Long,
                                @SerializedName("backgroundImgIdStr")
                                val backgroundImgIdStr: String,
                                @SerializedName("backgroundUrl")
                                val backgroundUrl: String,
                                @SerializedName("birthday")
                                val birthday: Long,
                                @SerializedName("brand")
                                val brand: String,
                                @SerializedName("city")
                                val city: Int,
                                @SerializedName("defaultAvatar")
                                val defaultAvatar: Boolean,
                                @SerializedName("description")
                                val description: String,
                                @SerializedName("detailDescription")
                                val detailDescription: String,
                                @SerializedName("djStatus")
                                val djStatus: Int,
                                @SerializedName("expertTags")
                                val expertTags: Any,
                                @SerializedName("experts")
                                val experts: Any,
                                @SerializedName("followed")
                                val followed: Boolean,
                                @SerializedName("gender")
                                val gender: Int,
                                @SerializedName("mutual")
                                val mutual: Boolean,
                                @SerializedName("nickname")
                                val nickname: String,
                                @SerializedName("province")
                                val province: Int,
                                @SerializedName("remarkName")
                                val remarkName: Any,
                                @SerializedName("signature")
                                val signature: String,
                                @SerializedName("userId")
                                val userId: Long,
                                @SerializedName("userType")
                                val userType: Int,
                                @SerializedName("vipType")
                                val vipType: Int
                            )

                            data class MainSong(
                                @SerializedName("album")
                                val album: Album,
                                @SerializedName("alias")
                                val alias: List<Any>,
                                @SerializedName("artists")
                                val artists: List<Artist>,
                                @SerializedName("audition")
                                val audition: Any,
                                @SerializedName("bMusic")
                                val bMusic: BMusic,
                                @SerializedName("commentThreadId")
                                val commentThreadId: String,
                                @SerializedName("copyFrom")
                                val copyFrom: String,
                                @SerializedName("copyright")
                                val copyright: Int,
                                @SerializedName("copyrightId")
                                val copyrightId: Long,
                                @SerializedName("crbt")
                                val crbt: Any,
                                @SerializedName("dayPlays")
                                val dayPlays: Int,
                                @SerializedName("disc")
                                val disc: String,
                                @SerializedName("duration")
                                val duration: Int,
                                @SerializedName("extProperties")
                                val extProperties: Any,
                                @SerializedName("fee")
                                val fee: Int,
                                @SerializedName("ftype")
                                val ftype: Int,
                                @SerializedName("hMusic")
                                val hMusic: HMusic,
                                @SerializedName("hearTime")
                                val hearTime: Int,
                                @SerializedName("hrMusic")
                                val hrMusic: Any,
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("lMusic")
                                val lMusic: LMusic,
                                @SerializedName("mMusic")
                                val mMusic: MMusic,
                                @SerializedName("mark")
                                val mark: Int,
                                @SerializedName("mp3Url")
                                val mp3Url: Any,
                                @SerializedName("mvid")
                                val mvId: Long,
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
                                @SerializedName("playedNum")
                                val playedNum: Int,
                                @SerializedName("popularity")
                                val popularity: Int,
                                @SerializedName("position")
                                val position: Int,
                                @SerializedName("ringtone")
                                val ringtone: String,
                                @SerializedName("rtUrl")
                                val rtUrl: Any,
                                @SerializedName("rtUrls")
                                val rtUrls: List<Any>,
                                @SerializedName("rtype")
                                val rtype: Int,
                                @SerializedName("rurl")
                                val rurl: Any,
                                @SerializedName("score")
                                val score: Int,
                                @SerializedName("sign")
                                val sign: Any,
                                @SerializedName("single")
                                val single: Int,
                                @SerializedName("sqMusic")
                                val sqMusic: Any,
                                @SerializedName("starred")
                                val starred: Boolean,
                                @SerializedName("starredNum")
                                val starredNum: Int,
                                @SerializedName("status")
                                val status: Int,
                                @SerializedName("transName")
                                val transName: Any,
                                @SerializedName("xInfo")
                                val xInfo: Any
                            ) {
                                data class Album(
                                    @SerializedName("alias")
                                    val alias: List<Any>,
                                    @SerializedName("artist")
                                    val artist: Artist,
                                    @SerializedName("artists")
                                    val artists: List<Artist>,
                                    @SerializedName("blurPicUrl")
                                    val blurPicUrl: String,
                                    @SerializedName("briefDesc")
                                    val briefDesc: String,
                                    @SerializedName("commentThreadId")
                                    val commentThreadId: String,
                                    @SerializedName("company")
                                    val company: Any,
                                    @SerializedName("companyId")
                                    val companyId: Long,
                                    @SerializedName("copyrightId")
                                    val copyrightId: Long,
                                    @SerializedName("description")
                                    val description: String,
                                    @SerializedName("dolbyMark")
                                    val dolbyMark: Int,
                                    @SerializedName("extProperties")
                                    val extProperties: ExtProperties,
                                    @SerializedName("gapless")
                                    val gapless: Int,
                                    @SerializedName("id")
                                    val Id: Long,
                                    @SerializedName("mark")
                                    val mark: Int,
                                    @SerializedName("name")
                                    val name: String,
                                    @SerializedName("onSale")
                                    val onSale: Boolean,
                                    @SerializedName("pic")
                                    val pic: Long,
                                    @SerializedName("picId")
                                    val picId: Long,
                                    @SerializedName("picUrl")
                                    val picUrl: String,
                                    @SerializedName("publishTime")
                                    val publishTime: Long,
                                    @SerializedName("size")
                                    val size: Int,
                                    @SerializedName("songs")
                                    val songs: List<Any>,
                                    @SerializedName("status")
                                    val status: Int,
                                    @SerializedName("subType")
                                    val subType: Any,
                                    @SerializedName("tags")
                                    val tags: String,
                                    @SerializedName("transName")
                                    val transName: Any,
                                    @SerializedName("type")
                                    val type: Any,
                                    @SerializedName("xInfo")
                                    val xInfo: XInfo
                                ) {
                                    data class Artist(
                                        @SerializedName("albumSize")
                                        val albumSize: Int,
                                        @SerializedName("alias")
                                        val alias: List<Any>,
                                        @SerializedName("briefDesc")
                                        val briefDesc: String,
                                        @SerializedName("extProperties")
                                        val extProperties: Any,
                                        @SerializedName("id")
                                        val Id: Long,
                                        @SerializedName("img1v1Id")
                                        val img1v1Id: Long,
                                        @SerializedName("img1v1Url")
                                        val img1v1Url: String,
                                        @SerializedName("musicSize")
                                        val musicSize: Int,
                                        @SerializedName("name")
                                        val name: String,
                                        @SerializedName("picId")
                                        val picId: Long,
                                        @SerializedName("picUrl")
                                        val picUrl: String,
                                        @SerializedName("topicPerson")
                                        val topicPerson: Int,
                                        @SerializedName("trans")
                                        val trans: String,
                                        @SerializedName("xInfo")
                                        val xInfo: Any
                                    )

                                    data class ExtProperties(
                                        @SerializedName("picId_str")
                                        val picIdStr: String
                                    )

                                    data class XInfo(
                                        @SerializedName("picId_str")
                                        val picIdStr: String
                                    )
                                }

                                data class Artist(
                                    @SerializedName("albumSize")
                                    val albumSize: Int,
                                    @SerializedName("alias")
                                    val alias: List<Any>,
                                    @SerializedName("briefDesc")
                                    val briefDesc: String,
                                    @SerializedName("extProperties")
                                    val extProperties: Any,
                                    @SerializedName("id")
                                    val Id: Long,
                                    @SerializedName("img1v1Id")
                                    val img1v1Id: Long,
                                    @SerializedName("img1v1Url")
                                    val img1v1Url: String,
                                    @SerializedName("musicSize")
                                    val musicSize: Int,
                                    @SerializedName("name")
                                    val name: String,
                                    @SerializedName("picId")
                                    val picId: Long,
                                    @SerializedName("picUrl")
                                    val picUrl: String,
                                    @SerializedName("topicPerson")
                                    val topicPerson: Int,
                                    @SerializedName("trans")
                                    val trans: String,
                                    @SerializedName("xInfo")
                                    val xInfo: Any
                                )

                                data class BMusic(
                                    @SerializedName("bitrate")
                                    val bitrate: Int,
                                    @SerializedName("dfsId")
                                    val dfsId: Long,
                                    @SerializedName("extProperties")
                                    val extProperties: Any,
                                    @SerializedName("extension")
                                    val extension: String,
                                    @SerializedName("id")
                                    val id: Long,
                                    @SerializedName("name")
                                    val name: Any,
                                    @SerializedName("playTime")
                                    val playTime: Int,
                                    @SerializedName("size")
                                    val size: Int,
                                    @SerializedName("sr")
                                    val sr: Int,
                                    @SerializedName("volumeDelta")
                                    val volumeDelta: Double,
                                    @SerializedName("xInfo")
                                    val xInfo: Any
                                )

                                data class HMusic(
                                    @SerializedName("bitrate")
                                    val bitrate: Int,
                                    @SerializedName("dfsId")
                                    val dfsId: Long,
                                    @SerializedName("extProperties")
                                    val extProperties: Any,
                                    @SerializedName("extension")
                                    val extension: String,
                                    @SerializedName("id")
                                    val id: Long,
                                    @SerializedName("name")
                                    val name: Any,
                                    @SerializedName("playTime")
                                    val playTime: Int,
                                    @SerializedName("size")
                                    val size: Int,
                                    @SerializedName("sr")
                                    val sr: Int,
                                    @SerializedName("volumeDelta")
                                    val volumeDelta: Double,
                                    @SerializedName("xInfo")
                                    val xInfo: Any
                                )

                                data class LMusic(
                                    @SerializedName("bitrate")
                                    val bitrate: Int,
                                    @SerializedName("dfsId")
                                    val dfsId: Long,
                                    @SerializedName("extProperties")
                                    val extProperties: Any,
                                    @SerializedName("extension")
                                    val extension: String,
                                    @SerializedName("id")
                                    val id: Long,
                                    @SerializedName("name")
                                    val name: Any,
                                    @SerializedName("playTime")
                                    val playTime: Int,
                                    @SerializedName("size")
                                    val size: Int,
                                    @SerializedName("sr")
                                    val sr: Int,
                                    @SerializedName("volumeDelta")
                                    val volumeDelta: Double,
                                    @SerializedName("xInfo")
                                    val xInfo: Any
                                )

                                data class MMusic(
                                    @SerializedName("bitrate")
                                    val bitrate: Int,
                                    @SerializedName("dfsId")
                                    val dfsId: Long,
                                    @SerializedName("extProperties")
                                    val extProperties: Any,
                                    @SerializedName("extension")
                                    val extension: String,
                                    @SerializedName("id")
                                    val id: Long,
                                    @SerializedName("name")
                                    val name: Any,
                                    @SerializedName("playTime")
                                    val playTime: Int,
                                    @SerializedName("size")
                                    val size: Int,
                                    @SerializedName("sr")
                                    val sr: Int,
                                    @SerializedName("volumeDelta")
                                    val volumeDelta: Double,
                                    @SerializedName("xInfo")
                                    val xInfo: Any
                                )
                            }

                            data class Radio(
                                @SerializedName("buyed")
                                val buyed: Boolean,
                                @SerializedName("category")
                                val category: String,
                                @SerializedName("categoryId")
                                val categoryId: Long,
                                @SerializedName("composeVideo")
                                val composeVideo: Boolean,
                                @SerializedName("createTime")
                                val createTime: Long,
                                @SerializedName("desc")
                                val desc: String,
                                @SerializedName("descPicList")
                                val descPicList: List<DescPic>,
                                @SerializedName("discountPrice")
                                val discountPrice: Any,
                                @SerializedName("dj")
                                val dj: Any,
                                @SerializedName("dynamic")
                                val `dynamic`: Boolean,
                                @SerializedName("feeScope")
                                val feeScope: Int,
                                @SerializedName("finished")
                                val finished: Boolean,
                                @SerializedName("hightQuality")
                                val hightQuality: Boolean,
                                @SerializedName("icon")
                                val icon: Any,
                                @SerializedName("id")
                                val Id: Long,
                                @SerializedName("intervenePicId")
                                val intervenePicId: Long,
                                @SerializedName("intervenePicUrl")
                                val intervenePicUrl: String,
                                @SerializedName("lastProgramCreateTime")
                                val lastProgramCreateTime: Long,
                                @SerializedName("lastProgramId")
                                val lastProgramId: Long,
                                @SerializedName("lastProgramName")
                                val lastProgramName: Any,
                                @SerializedName("liveInfo")
                                val liveInfo: Any,
                                @SerializedName("manualTagsDTO")
                                val manualTagsDTO: Any,
                                @SerializedName("name")
                                val name: String,
                                @SerializedName("originalPrice")
                                val originalPrice: Int,
                                @SerializedName("picId")
                                val picId: Long,
                                @SerializedName("picUrl")
                                val picUrl: String,
                                @SerializedName("playCount")
                                val playCount: Long,
                                @SerializedName("price")
                                val price: Int,
                                @SerializedName("privacy")
                                val privacy: Boolean,
                                @SerializedName("programCount")
                                val programCount: Int,
                                @SerializedName("purchaseCount")
                                val purchaseCount: Int,
                                @SerializedName("radioFeeType")
                                val radioFeeType: Int,
                                @SerializedName("rcmdText")
                                val rcmdText: String,
                                @SerializedName("scoreInfoDTO")
                                val scoreInfoDTO: Any,
                                @SerializedName("secondCategory")
                                val secondCategory: String,
                                @SerializedName("secondCategoryId")
                                val secondCategoryId: Long,
                                @SerializedName("shortName")
                                val shortName: Any,
                                @SerializedName("subCount")
                                val subCount: Int,
                                @SerializedName("subed")
                                val subed: Boolean,
                                @SerializedName("taskId")
                                val taskId: Long,
                                @SerializedName("underShelf")
                                val underShelf: Boolean,
                                @SerializedName("videos")
                                val videos: Any,
                                @SerializedName("whiteList")
                                val whiteList: Boolean
                            ) {
                                data class DescPic(
                                    @SerializedName("content")
                                    val content: String,
                                    @SerializedName("height")
                                    val height: Any,
                                    @SerializedName("id")
                                    val Id: Long,
                                    @SerializedName("imageContentURLInvalid")
                                    val imageContentURLInvalid: Boolean,
                                    @SerializedName("type")
                                    val type: Int,
                                    @SerializedName("width")
                                    val width: Any
                                )
                            }
                        }

                        data class Song(
                            @SerializedName("a")
                            val a: Any,
                            @SerializedName("al")
                            val al: Al,
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
                            @SerializedName("pop")
                            val pop: Int,
                            @SerializedName("pst")
                            val pst: Int,
                            @SerializedName("publishTime")
                            val publishTime: Long,
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



                        data class SongData(
                            @SerializedName("album")
                            val album: Album,
                            @SerializedName("alias")
                            val alias: List<String>,
                            @SerializedName("artists")
                            val artists: List<Artist>,
                            @SerializedName("audition")
                            val audition: Any,
                            @SerializedName("bMusic")
                            val bMusic: BMusic,
                            @SerializedName("commentThreadId")
                            val commentThreadId: String,
                            @SerializedName("copyFrom")
                            val copyFrom: String,
                            @SerializedName("copyright")
                            val copyright: Int,
                            @SerializedName("copyrightId")
                            val copyrightId: Long,
                            @SerializedName("crbt")
                            val crbt: Any,
                            @SerializedName("dayPlays")
                            val dayPlays: Int,
                            @SerializedName("disc")
                            val disc: String,
                            @SerializedName("duration")
                            val duration: Int,
                            @SerializedName("extProperties")
                            val extProperties: ExtProperties,
                            @SerializedName("fee")
                            val fee: Int,
                            @SerializedName("ftype")
                            val ftype: Int,
                            @SerializedName("hMusic")
                            val hMusic: HMusic,
                            @SerializedName("hearTime")
                            val hearTime: Int,
                            @SerializedName("hrMusic")
                            val hrMusic: HrMusic,
                            @SerializedName("id")
                            val Id: Long,
                            @SerializedName("lMusic")
                            val lMusic: LMusic,
                            @SerializedName("mMusic")
                            val mMusic: MMusic,
                            @SerializedName("mark")
                            val mark: Int,
                            @SerializedName("mp3Url")
                            val mp3Url: Any,
                            @SerializedName("mvid")
                            val mvId: Long,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("no")
                            val no: Int,
                            @SerializedName("noCopyrightRcmd")
                            val noCopyrightRcmd: Any,
                            @SerializedName("originCoverType")
                            val originCoverType: Int,
                            @SerializedName("originSongSimpleData")
                            val originSongSimpleData: OriginSongSimpleData,
                            @SerializedName("playedNum")
                            val playedNum: Int,
                            @SerializedName("popularity")
                            val popularity: Int,
                            @SerializedName("position")
                            val position: Int,
                            @SerializedName("ringtone")
                            val ringtone: String,
                            @SerializedName("rtUrl")
                            val rtUrl: Any,
                            @SerializedName("rtUrls")
                            val rtUrls: List<Any>,
                            @SerializedName("rtype")
                            val rtype: Int,
                            @SerializedName("rurl")
                            val rurl: Any,
                            @SerializedName("score")
                            val score: Int,
                            @SerializedName("sign")
                            val sign: Any,
                            @SerializedName("single")
                            val single: Int,
                            @SerializedName("sqMusic")
                            val sqMusic: SqMusic,
                            @SerializedName("starred")
                            val starred: Boolean,
                            @SerializedName("starredNum")
                            val starredNum: Int,
                            @SerializedName("status")
                            val status: Int,
                            @SerializedName("transName")
                            val transName: String,
                            @SerializedName("xInfo")
                            val xInfo: XInfo
                        ) {
                            data class Album(
                                @SerializedName("alias")
                                val alias: List<String>,
                                @SerializedName("artist")
                                val artist: Artist,
                                @SerializedName("artists")
                                val artists: List<Artist>,
                                @SerializedName("blurPicUrl")
                                val blurPicUrl: String,
                                @SerializedName("briefDesc")
                                val briefDesc: String,
                                @SerializedName("commentThreadId")
                                val commentThreadId: String,
                                @SerializedName("company")
                                val company: String,
                                @SerializedName("companyId")
                                val companyId: Long,
                                @SerializedName("copyrightId")
                                val copyrightId: Long,
                                @SerializedName("description")
                                val description: String,
                                @SerializedName("dolbyMark")
                                val dolbyMark: Int,
                                @SerializedName("extProperties")
                                val extProperties: ExtProperties,
                                @SerializedName("gapless")
                                val gapless: Int,
                                @SerializedName("id")
                                val Id: Long,
                                @SerializedName("mark")
                                val mark: Int,
                                @SerializedName("name")
                                val name: String,
                                @SerializedName("onSale")
                                val onSale: Boolean,
                                @SerializedName("pic")
                                val pic: Long,
                                @SerializedName("picId")
                                val picId: Long,
                                @SerializedName("picUrl")
                                val picUrl: String,
                                @SerializedName("publishTime")
                                val publishTime: Long,
                                @SerializedName("size")
                                val size: Int,
                                @SerializedName("songs")
                                val songs: List<Any>,
                                @SerializedName("status")
                                val status: Int,
                                @SerializedName("subType")
                                val subType: String,
                                @SerializedName("tags")
                                val tags: String,
                                @SerializedName("transName")
                                val transName: String,
                                @SerializedName("type")
                                val type: String,
                                @SerializedName("xInfo")
                                val xInfo: XInfo
                            ) {


                                data class Artist(
                                    @SerializedName("albumSize")
                                    val albumSize: Int,
                                    @SerializedName("alias")
                                    val alias: List<Any>,
                                    @SerializedName("briefDesc")
                                    val briefDesc: String,
                                    @SerializedName("extProperties")
                                    val extProperties: Any,
                                    @SerializedName("id")
                                    val Id: Long,
                                    @SerializedName("img1v1Id")
                                    val img1v1Id: Long,
                                    @SerializedName("img1v1Url")
                                    val img1v1Url: String,
                                    @SerializedName("musicSize")
                                    val musicSize: Int,
                                    @SerializedName("name")
                                    val name: String,
                                    @SerializedName("picId")
                                    val picId: Long,
                                    @SerializedName("picUrl")
                                    val picUrl: String,
                                    @SerializedName("topicPerson")
                                    val topicPerson: Int,
                                    @SerializedName("trans")
                                    val trans: String,
                                    @SerializedName("xInfo")
                                    val xInfo: Any
                                )

                                data class ExtProperties(
                                    @SerializedName("picId_str")
                                    val picIdStr: String,
                                    @SerializedName("transNames")
                                    val transNames: List<String>
                                )

                                data class XInfo(
                                    @SerializedName("picId_str")
                                    val picIdStr: String,
                                    @SerializedName("transNames")
                                    val transNames: List<String>
                                )
                            }

                            data class Artist(
                                @SerializedName("albumSize")
                                val albumSize: Int,
                                @SerializedName("alias")
                                val alias: List<Any>,
                                @SerializedName("briefDesc")
                                val briefDesc: String,
                                @SerializedName("extProperties")
                                val extProperties: Any,
                                @SerializedName("id")
                                val Id: Long,
                                @SerializedName("img1v1Id")
                                val img1v1Id: Long,
                                @SerializedName("img1v1Url")
                                val img1v1Url: String,
                                @SerializedName("musicSize")
                                val musicSize: Int,
                                @SerializedName("name")
                                val name: String,
                                @SerializedName("picId")
                                val picId: Long,
                                @SerializedName("picUrl")
                                val picUrl: String,
                                @SerializedName("topicPerson")
                                val topicPerson: Int,
                                @SerializedName("trans")
                                val trans: String,
                                @SerializedName("xInfo")
                                val xInfo: Any
                            )

                            data class BMusic(
                                @SerializedName("bitrate")
                                val bitrate: Int,
                                @SerializedName("dfsId")
                                val dfsId: Long,
                                @SerializedName("extProperties")
                                val extProperties: Any,
                                @SerializedName("extension")
                                val extension: String,
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("name")
                                val name: Any,
                                @SerializedName("playTime")
                                val playTime: Int,
                                @SerializedName("size")
                                val size: Int,
                                @SerializedName("sr")
                                val sr: Int,
                                @SerializedName("volumeDelta")
                                val volumeDelta: Double,
                                @SerializedName("xInfo")
                                val xInfo: Any
                            )

                            data class ExtProperties(
                                @SerializedName("pc")
                                val pc: Pc,
                                @SerializedName("transNames")
                                val transNames: List<String>
                            ) {
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
                            }

                            data class HMusic(
                                @SerializedName("bitrate")
                                val bitrate: Int,
                                @SerializedName("dfsId")
                                val dfsId: Long,
                                @SerializedName("extProperties")
                                val extProperties: Any,
                                @SerializedName("extension")
                                val extension: String,
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("name")
                                val name: Any,
                                @SerializedName("playTime")
                                val playTime: Int,
                                @SerializedName("size")
                                val size: Int,
                                @SerializedName("sr")
                                val sr: Int,
                                @SerializedName("volumeDelta")
                                val volumeDelta: Double,
                                @SerializedName("xInfo")
                                val xInfo: Any
                            )

                            data class HrMusic(
                                @SerializedName("bitrate")
                                val bitrate: Int,
                                @SerializedName("dfsId")
                                val dfsId: Long,
                                @SerializedName("extProperties")
                                val extProperties: Any,
                                @SerializedName("extension")
                                val extension: String,
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("name")
                                val name: Any,
                                @SerializedName("playTime")
                                val playTime: Int,
                                @SerializedName("size")
                                val size: Int,
                                @SerializedName("sr")
                                val sr: Int,
                                @SerializedName("volumeDelta")
                                val volumeDelta: Double,
                                @SerializedName("xInfo")
                                val xInfo: Any
                            )

                            data class LMusic(
                                @SerializedName("bitrate")
                                val bitrate: Int,
                                @SerializedName("dfsId")
                                val dfsId: Long,
                                @SerializedName("extProperties")
                                val extProperties: Any,
                                @SerializedName("extension")
                                val extension: String,
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("name")
                                val name: Any,
                                @SerializedName("playTime")
                                val playTime: Int,
                                @SerializedName("size")
                                val size: Int,
                                @SerializedName("sr")
                                val sr: Int,
                                @SerializedName("volumeDelta")
                                val volumeDelta: Double,
                                @SerializedName("xInfo")
                                val xInfo: Any
                            )

                            data class MMusic(
                                @SerializedName("bitrate")
                                val bitrate: Int,
                                @SerializedName("dfsId")
                                val dfsId: Long,
                                @SerializedName("extProperties")
                                val extProperties: Any,
                                @SerializedName("extension")
                                val extension: String,
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("name")
                                val name: Any,
                                @SerializedName("playTime")
                                val playTime: Int,
                                @SerializedName("size")
                                val size: Int,
                                @SerializedName("sr")
                                val sr: Int,
                                @SerializedName("volumeDelta")
                                val volumeDelta: Double,
                                @SerializedName("xInfo")
                                val xInfo: Any
                            )

                            data class OriginSongSimpleData(
                                @SerializedName("albumMeta")
                                val albumMeta: AlbumMeta,
                                @SerializedName("artists")
                                val artists: List<Artist>,
                                @SerializedName("name")
                                val name: String,
                                @SerializedName("songId")
                                val songId: Long
                            ) {
                                data class AlbumMeta(
                                    @SerializedName("extProperties")
                                    val extProperties: Any,
                                    @SerializedName("id")
                                    val Id: Long,
                                    @SerializedName("name")
                                    val name: String,
                                    @SerializedName("xInfo")
                                    val xInfo: Any
                                )

                                data class Artist(
                                    @SerializedName("extProperties")
                                    val extProperties: Any,
                                    @SerializedName("id")
                                    val Id: Long,
                                    @SerializedName("name")
                                    val name: String,
                                    @SerializedName("xInfo")
                                    val xInfo: Any
                                )
                            }

                            data class SqMusic(
                                @SerializedName("bitrate")
                                val bitrate: Int,
                                @SerializedName("dfsId")
                                val dfsId: Long,
                                @SerializedName("extProperties")
                                val extProperties: Any,
                                @SerializedName("extension")
                                val extension: String,
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("name")
                                val name: Any,
                                @SerializedName("playTime")
                                val playTime: Int,
                                @SerializedName("size")
                                val size: Int,
                                @SerializedName("sr")
                                val sr: Int,
                                @SerializedName("volumeDelta")
                                val volumeDelta: Double,
                                @SerializedName("xInfo")
                                val xInfo: Any
                            )

                            data class XInfo(
                                @SerializedName("pc")
                                val pc: Pc,
                                @SerializedName("transNames")
                                val transNames: List<String>
                            ) {
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
                            }
                        }

                        data class SongPrivilege(
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
                            val pc: Any,
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
                        }
                    }

                    data class UiElement(
                        @SerializedName("description")
                        val description: String,
                        @SerializedName("image")
                        val image: Image,
                        @SerializedName("labelBgColor")
                        val labelBgColor: String,
                        @SerializedName("labelBorderColor")
                        val labelBorderColor: String,
                        @SerializedName("labelColor")
                        val labelColor: String,
                        @SerializedName("labelTexts")
                        val labelTexts: List<String>,
                        @SerializedName("labelType")
                        val labelType: String,
                        @SerializedName("mainTitle")
                        val mainTitle: MainTitle,
                        @SerializedName("rcmdShowType")
                        val rcmdShowType: String,
                        @SerializedName("subTitle")
                        val subTitle: SubTitle
                    ) {
                        data class Image(
                            @SerializedName("action")
                            val action: String,
                            @SerializedName("actionType")
                            val actionType: String,
                            @SerializedName("imageUrl")
                            val imageUrl: String,
                            @SerializedName("imageUrl2")
                            val imageUrl2: String,
                            @SerializedName("purePicture")
                            val purePicture: Boolean,
                            @SerializedName("title")
                            val title: String
                        )

                        data class MainTitle(
                            @SerializedName("canShowTitleLogo")
                            val canShowTitleLogo: Boolean,
                            @SerializedName("title")
                            val title: String
                        )

                        data class SubTitle(
                            @SerializedName("canShowTitleLogo")
                            val canShowTitleLogo: Boolean,
                            @SerializedName("title")
                            val title: String,
                            @SerializedName("titleId")
                            val titleId: String,
                            @SerializedName("titleType")
                            val titleType: String
                        )
                    }
                }

                data class UiElement(
                    @SerializedName("image")
                    val image: Image,
                    @SerializedName("labelTexts")
                    val labelTexts: List<String>,
                    @SerializedName("mainTitle")
                    val mainTitle: MainTitle,
                    @SerializedName("rcmdShowType")
                    val rcmdShowType: String,
                    @SerializedName("subTitle")
                    val subTitle: SubTitle
                ) {
                    data class Image(
                        @SerializedName("imageUrl")
                        val imageUrl: String,
                        @SerializedName("purePicture")
                        val purePicture: Boolean
                    )

                    data class MainTitle(
                        @SerializedName("canShowTitleLogo")
                        val canShowTitleLogo: Boolean,
                        @SerializedName("title")
                        val title: String
                    )

                    data class SubTitle(
                        @SerializedName("canShowTitleLogo")
                        val canShowTitleLogo: Boolean,
                        @SerializedName("title")
                        val title: String
                    )
                }
            }

            data class CrossPlatformConfig(
                @SerializedName("containerType")
                val containerType: String,
                @SerializedName("rnContent")
                val rnContent: RnContent
            ) {
                data class RnContent(
                    @SerializedName("component")
                    val component: String,
                    @SerializedName("engineId")
                    val engineId: String,
                    @SerializedName("estimatedHeight")
                    val estimatedHeight: Int,
                    @SerializedName("estimatedRatio")
                    val estimatedRatio: String,
                    @SerializedName("moduleName")
                    val moduleName: String,
                    @SerializedName("params")
                    val params: Params
                ) {
                    class Params
                }
            }

            data class ExtInfo(
                @SerializedName("banners")
                val banners: List<Banner>
            ) {
                data class Banner(
                    @SerializedName("adLocation")
                    val adLocation: Any,
                    @SerializedName("adSource")
                    val adSource: Any,
                    @SerializedName("adid")
                    val adid: Any,
                    @SerializedName("backgroundColor")
                    val backgroundColor: Any,
                    @SerializedName("backgroundImageUrl")
                    val backgroundImageUrl: Any,
                    @SerializedName("bannerBizType")
                    val bannerBizType: String,
                    @SerializedName("encodeId")
                    val encodeId: Any,
                    @SerializedName("exclusive")
                    val exclusive: Boolean,
                    @SerializedName("extMonitor")
                    val extMonitor: Any,
                    @SerializedName("extMonitorInfo")
                    val extMonitorInfo: Any,
                    @SerializedName("imageUrl")
                    val imageUrl: String,
                    @SerializedName("monitorBlackList")
                    val monitorBlackList: Any,
                    @SerializedName("monitorClick")
                    val monitorClick: Any,
                    @SerializedName("monitorClickList")
                    val monitorClickList: Any,
                    @SerializedName("monitorImpress")
                    val monitorImpress: Any,
                    @SerializedName("monitorImpressList")
                    val monitorImpressList: Any,
                    @SerializedName("monitorType")
                    val monitorType: Any,
                    @SerializedName("scm")
                    val scm: String,
                    @SerializedName("targetId")
                    val targetId: Long,
                    @SerializedName("targetType")
                    val targetType: Int,
                    @SerializedName("titleColor")
                    val titleColor: String,
                    @SerializedName("typeTitle")
                    val typeTitle: String,
                    @SerializedName("url")
                    val url: String
                )
            }

            data class UiElement(
                @SerializedName("button")
                val button: Button,
                @SerializedName("canRefresh")
                val canRefresh: Boolean,
                @SerializedName("mainTitle")
                val mainTitle: MainTitle,
                @SerializedName("rcmdShowType")
                val rcmdShowType: String,
                @SerializedName("subTitle")
                val subTitle: SubTitle
            ) {
                data class Button(
                    @SerializedName("action")
                    val action: String,
                    @SerializedName("actionType")
                    val actionType: String,
                    @SerializedName("biData")
                    val biData: Any,
                    @SerializedName("iconUrl")
                    val iconUrl: Any,
                    @SerializedName("text")
                    val text: String
                )

                data class MainTitle(
                    @SerializedName("action")
                    val action: String,
                    @SerializedName("actionType")
                    val actionType: String,
                    @SerializedName("canShowTitleLogo")
                    val canShowTitleLogo: Boolean,
                    @SerializedName("title")
                    val title: String
                )

                data class SubTitle(
                    @SerializedName("canShowTitleLogo")
                    val canShowTitleLogo: Boolean,
                    @SerializedName("title")
                    val title: String
                )
            }
        }

        data class GuideToast(
            @SerializedName("hasGuideToast")
            val hasGuideToast: Boolean,
            @SerializedName("toastList")
            val toastList: List<Any>
        )

        data class PageConfig(
            @SerializedName("abtest")
            val abtest: List<String>,
            @SerializedName("fullscreen")
            val fullscreen: Boolean,
            @SerializedName("homepageMode")
            val homepageMode: String,
            @SerializedName("nodataToast")
            val nodataToast: String,
            @SerializedName("orderInfo")
            val orderInfo: String,
            @SerializedName("refreshInterval")
            val refreshInterval: Int,
            @SerializedName("refreshToast")
            val refreshToast: String,
            @SerializedName("showModeEntry")
            val showModeEntry: Boolean,
            @SerializedName("songLabelMarkLimit")
            val songLabelMarkLimit: Int,
            @SerializedName("songLabelMarkPriority")
            val songLabelMarkPriority: List<String>,
            @SerializedName("title")
            val title: Any
        )
    }

    data class Trp(
        @SerializedName("rules")
        val rules: List<String>
    )
}