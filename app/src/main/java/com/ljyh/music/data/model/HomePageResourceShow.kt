package com.ljyh.music.data.model
import com.google.gson.annotations.SerializedName
import com.ljyh.music.di.SpecialKey


data class HomePageResourceShow(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data
) {
    data class Data(
        @SerializedName("blocks")
        val blocks: List<Block>,
        @SerializedName("guideToast")
        val guideToast: Any,
        @SerializedName("cursor")
        val cursor: Int,
        @SerializedName("blockCodeOrderList")
        val blockCodeOrderList: Any,
        @SerializedName("requestBlockOrder")
        val requestBlockOrder: Any,
        @SerializedName("callbackParameters")
        val callbackParameters: String,
        @SerializedName("hasMore")
        val hasMore: Boolean,
        @SerializedName("demote")
        val demote: Boolean,
        @SerializedName("libraList")
        val libraList: Any,
        @SerializedName("hasDoubleFlow")
        val hasDoubleFlow: Boolean,
        @SerializedName("extMap")
        val extMap: Any,
        @SerializedName("dinformation")
        val dinformation: Any
    ) {
        data class Block(
            @SerializedName("trp_type")
            val trpType: Any,
            @SerializedName("trp_id")
            val trpId: Any,
            @SerializedName("scene")
            val scene: Any,
            @SerializedName("positionCode")
            val positionCode: String,
            @SerializedName("c_flowGroupId")
            val cFlowGroupId: Any,
            @SerializedName("channelCode")
            val channelCode: String,
            @SerializedName("channelCodeSubId")
            val channelCodeSubId: Any,
            @SerializedName("c_planGroupId")
            val cPlanGroupId: Any,
            @SerializedName("c_planId")
            val cPlanId: Any,
            @SerializedName("c_templateId")
            val cTemplateId: Any,
            @SerializedName("c_bizTags")
            val cBizTags: Any,
            @SerializedName("c_resourceType")
            val cResourceType: Any,
            @SerializedName("c_resourceId")
            val cResourceId: Any,
            @SerializedName("c_creativeReachId")
            val cCreativeReachId: Any,
            @SerializedName("originalData")
            val originalData: Any,
            @SerializedName("clientData")
            val clientData: Any,
            @SerializedName("summary")
            val summary: Any,
            @SerializedName("log")
            val log: Log,
            @SerializedName("positionInteractInfo")
            val positionInteractInfo: Any,
            @SerializedName("moduleType")
            val moduleType: Any,
            @SerializedName("crossPlatformConfig")
            val crossPlatformConfig: CrossPlatformConfig,
            @SerializedName("nativeConfig")
            val nativeConfig: Any,
            @SerializedName("dslData")
            val dslData: DslData,
            @SerializedName("rnData")
            val rnData: Any,
            @SerializedName("nativeData")
            val nativeData: Any,
            @SerializedName("extMap")
            val extMap: ExtMap,
            @SerializedName("callbackParametersMap")
            val callbackParametersMap: Any,
            @SerializedName("bizCode")
            val bizCode: String,
            @SerializedName("showTitle")
            val showTitle: Boolean,
            @SerializedName("needClientCover")
            val needClientCover: Boolean,
            @SerializedName("feedbackType")
            val feedbackType: Any,
            @SerializedName("likePosition")
            val likePosition: Int,
            @SerializedName("logId")
            val logId: Any,
            @SerializedName("logMap")
            val logMap: LogMap,
            @SerializedName("code")
            val code: Any,
            @SerializedName("constructLogId")
            val constructLogId: String
        ) {
            data class Log(
                @SerializedName("s_ctrp")
                val sCtrp: String
            )

            data class CrossPlatformConfig(
                @SerializedName("containerType")
                val containerType: String,
                @SerializedName("alertConfig")
                val alertConfig: AlertConfig,
                @SerializedName("rnContent")
                val rnContent: Any,
                @SerializedName("dslContent")
                val dslContent: DslContent
            ) {
                data class AlertConfig(
                    @SerializedName("alertType")
                    val alertType: Any,
                    @SerializedName("needCloseBtn")
                    val needCloseBtn: Boolean,
                    @SerializedName("hPadding")
                    val hPadding: Int,
                    @SerializedName("widthExpand")
                    val widthExpand: Boolean,
                    @SerializedName("autoDismissDuration")
                    val autoDismissDuration: Any,
                    @SerializedName("hpadding")
                    val hpadding: Int
                )

                data class DslContent(
                    @SerializedName("lunaforcmSence")
                    val lunaforcmSence: String,
                    @SerializedName("lunaforcmTempletId")
                    val lunaforcmTempletId: String,
                    @SerializedName("lunaforcmLoadType")
                    val lunaforcmLoadType: String,
                    @SerializedName("lunaforcmTempletContent")
                    val lunaforcmTempletContent: LunaforcmTempletContent
                ) {
                    data class LunaforcmTempletContent(
                        @SerializedName("dataSources")
                        val dataSources: Any,
                        @SerializedName("dslRootId")
                        val dslRootId: String,
                        @SerializedName("minSupportVersion")
                        val minSupportVersion: Any,
                        @SerializedName("blockId")
                        val blockId: Int,
                        @SerializedName("blockName")
                        val blockName: String,
                        @SerializedName("blockType")
                        val blockType: String,
                        @SerializedName("publishTime")
                        val publishTime: String,
                        @SerializedName("templateConfig")
                        val templateConfig: String,
                        @SerializedName("sceneName")
                        val sceneName: String,
                        @SerializedName("needScaleForPad")
                        val needScaleForPad: Boolean,
                        @SerializedName("dslMapMd5")
                        val dslMapMd5: Any,
                        @SerializedName("merge")
                        val merge: Any,
                        @SerializedName("headlessJson")
                        val headlessJson: Any
                    )
                }
            }

            data class DslData(
                @SerializedName("blockResource")
                val blockResource: BlockResource,
                @SerializedName("code")
                val code: Int,
                @SerializedName("djPrograms")
                val djPrograms: List<DjProgram>,
                @SerializedName("cursor")
                val cursor: String,
                @SerializedName("blockCode")
                val blockCode: String,
                @SerializedName("blockName")
                val blockName: String,
                @SerializedName("itemIds")
                val itemIds: List<Long>,
                @SerializedName("client")
                val client: String,
                @SerializedName("header")
                val header: Header,
                @SerializedName("track")
                val track: Track,
                @SerializedName("items")
                val items: List<Item>,
                @SerializedName("extInfo")
                val extInfo: ExtInfo,

                @SerializedName(SpecialKey.Rank)
                val rank: Rank,
                @SerializedName(SpecialKey.HomeCommon)
                val homeCommon: HomeCommon,
            ) {
                data class Rank(
                    @SerializedName("showMore")
                    val showMore: Boolean,
                    @SerializedName("resources")
                    val resources: List<Resource>,
                    @SerializedName("title")
                    val title: String
                ) {
                    data class Resource(
                        @SerializedName("playCount")
                        val playCount: String,
                        @SerializedName("playCountOpacity")
                        val playCountOpacity: Int,
                        @SerializedName("resourceId")
                        val resourceId: String,
                        @SerializedName("coverImg")
                        val coverImg: String,
                        @SerializedName("subTitle")
                        val subTitle: String,
                        @SerializedName("action")
                        val action: String,
                        @SerializedName("playBtnOpacity")
                        val playBtnOpacity: Int,
                        @SerializedName("playBtn")
                        val playBtn: String,
                        @SerializedName("title")
                        val title: String,
                        @SerializedName("alg")
                        val alg: String,
                        @SerializedName("resourceType")
                        val resourceType: String
                    )
                }

                data class BlockResource(
                    @SerializedName("subTitle")
                    val subTitle: String,
                    @SerializedName("showMore")
                    val showMore: Boolean,
                    @SerializedName("resources")
                    val resources: List<Resource>,
                    @SerializedName("title")
                    val title: String,
                    @SerializedName("alg")
                    val alg: String,
                    @SerializedName("action")
                    val action: Any,
                    @SerializedName("iconUrl")
                    val iconUrl: Any,
                    @SerializedName("minLimitSize")
                    val minLimitSize: Int,
                    @SerializedName("button")
                    val button: Any,
                    @SerializedName("playAll")
                    val playAll: Any
                ) {
                    data class Resource(
                        @SerializedName("playBtnData")
                        val playBtnData: Any,
                        @SerializedName("resourceId")
                        val resourceId: String,
                        @SerializedName("coverImg")
                        val coverImg: String,
                        @SerializedName("subTitle")
                        val subTitle: String,
                        @SerializedName("iconDesc")
                        val iconDesc: IconDesc,
                        @SerializedName("lunaItemType")
                        val lunaItemType: String,
                        @SerializedName("action")
                        val action: String,
                        @SerializedName("title")
                        val title: String,
                        @SerializedName("alg")
                        val alg: String,
                        @SerializedName("extInfo")
                        val extInfo: ExtInfo,
                        @SerializedName("resourceType")
                        val resourceType: String,
                        @SerializedName("tagId")
                        val tagId: String,
                        @SerializedName("categoryId")
                        val categoryId: String,
                        @SerializedName("resourceStates")
                        val resourceStates: String,
                        @SerializedName("playOrpheus")
                        val playOrpheus: Any,
                        @SerializedName("coverImgType")
                        val coverImgType: Any,
                        @SerializedName("logInfo")
                        val logInfo: Any,
                        @SerializedName("resourceInteractInfo")
                        val resourceInteractInfo: ResourceInteractInfo,
                        @SerializedName("tags")
                        val tags: List<Any>,
                        @SerializedName("tagImgUrl")
                        val tagImgUrl: Any,
                        @SerializedName("labelText")
                        val labelText: Any,
                        @SerializedName("resourceExtInfo")
                        val resourceExtInfo: ResourceExtInfo,
                        @SerializedName("extMap")
                        val extMap: Any,
                        @SerializedName("singleLineTitle")
                        val singleLineTitle:String,
                    ) {
                        data class PlayBtnData(
                            @SerializedName("pauseType")
                            val pauseType: String,
                            @SerializedName("resourceId")
                            val resourceId: String,
                            @SerializedName("playOrpheus")
                            val playOrpheus: String,
                            @SerializedName("playType")
                            val playType: String,
                            @SerializedName("detailUrl")
                            val detailUrl: String,
                            @SerializedName("playActionType")
                            val playActionType: String,
                            @SerializedName("resourceType")
                            val resourceType: String
                        )

                        data class ResourceExtInfo(
                            @SerializedName("artists")
                            val artists: List<Artist>,
                            @SerializedName("coverText")
                            val coverText: List<String>
                        ) {
                            data class Artist(
                                @SerializedName("id")
                                val id: Int,
                                @SerializedName("imgUrl")
                                val imgUrl: String,
                                @SerializedName("name")
                                val name: String,
                                @SerializedName("nickName")
                                val nickName: String
                            )
                        }

                        data class IconDesc(
                            @SerializedName("image")
                            val image: String,
                            @SerializedName("blur_bottom")
                            val blurBottom: String,
                            @SerializedName("blur_top")
                            val blurTop: String,
                            @SerializedName("blur_right")
                            val blurRight: String,
                            @SerializedName("blur_show")
                            val blurShow: Boolean,
                            @SerializedName("blur_left")
                            val blurLeft: String
                        )

                        data class ExtInfo(
                            @SerializedName("miniIconGroup")
                            val miniIconGroup: String,
                            @SerializedName("useMiniIcon")
                            val useMiniIcon: Boolean,
                            @SerializedName("topGradient")
                            val topGradient: String
                        )

                        data class ResourceInteractInfo(
                            @SerializedName("playCount")
                            val playCount: String,
                            @SerializedName("likedCount")
                            val likedCount: Any,
                            @SerializedName("replyCount")
                            val replyCount: Any
                        )
                    }
                }

                data class DjProgram(
                    @SerializedName("scheduledPublishTime")
                    val scheduledPublishTime: Long,
                    @SerializedName("bdAuditStatus")
                    val bdAuditStatus: Int,
                    @SerializedName("programDesc")
                    val programDesc: List<ProgramDesc>,
                    @SerializedName("dj")
                    val dj: Dj,
                    @SerializedName("description")
                    val description: String,
                    @SerializedName("privacy")
                    val privacy: Boolean,
                    @SerializedName("classicRelationSong")
                    val classicRelationSong: ClassicRelationSong,
                    @SerializedName("specialTags")
                    val specialTags: List<String>,
                    @SerializedName("radio")
                    val radio: Radio,
                    @SerializedName("programFeeType")
                    val programFeeType: Int,
                    @SerializedName("duration")
                    val duration: Int,
                    @SerializedName("listenerCount")
                    val listenerCount: Int,
                    @SerializedName("trackCount")
                    val trackCount: Int,
                    @SerializedName("coverId")
                    val coverId: Long,
                    @SerializedName("blurCoverUrl")
                    val blurCoverUrl: String,
                    @SerializedName("id")
                    val id: Long,
                    @SerializedName("reward")
                    val reward: Boolean,
                    @SerializedName("pubStatus")
                    val pubStatus: Int,
                    @SerializedName("createEventId")
                    val createEventId: Int,
                    @SerializedName("serialNum")
                    val serialNum: Long,
                    @SerializedName("commentThreadId")
                    val commentThreadId: String,
                    @SerializedName("updateTime")
                    val updateTime: Long,
                    @SerializedName("mainTrackId")
                    val mainTrackId: Long,
                    @SerializedName("userId")
                    val userId: Long,
                    @SerializedName("coverUrl")
                    val coverUrl: String,
                    @SerializedName("channels")
                    val channels: List<String>,
                    @SerializedName("h5Links")
                    val h5Links: List<Any>,
                    @SerializedName("createTime")
                    val createTime: Long,
                    @SerializedName("canReward")
                    val canReward: Boolean,
                    @SerializedName("publish")
                    val publish: Boolean,
                    @SerializedName("name")
                    val name: String,
                    @SerializedName("buyed")
                    val buyed: Boolean,
                    @SerializedName("subscribedCount")
                    val subscribedCount: Int,
                    @SerializedName("auditStatus")
                    val auditStatus: Int,
                    @SerializedName("adjustedPlayCount")
                    val adjustedPlayCount: Int,
                    @SerializedName("secondCategoryId")
                    val secondCategoryId: Int,
                    @SerializedName("categoryId")
                    val categoryId: Int,
                    @SerializedName("disPlayStatus")
                    val disPlayStatus: String,
                    @SerializedName("secondCategory")
                    val secondCategory: String,
                    @SerializedName("mainSong")
                    val mainSong: MainSong,
                    @SerializedName("shortName")
                    val shortName: String,
                    @SerializedName("category")
                    val category: String
                ) {
                    data class ProgramDesc(
                        @SerializedName("id")
                        val id: Int
                    )

                    data class Dj(
                        @SerializedName("birthday")
                        val birthday: Long,
                        @SerializedName("detailDescription")
                        val detailDescription: String,
                        @SerializedName("backgroundUrl")
                        val backgroundUrl: String,
                        @SerializedName("gender")
                        val gender: Int,
                        @SerializedName("city")
                        val city: Int,
                        @SerializedName("signature")
                        val signature: String,
                        @SerializedName("description")
                        val description: String,
                        @SerializedName("accountStatus")
                        val accountStatus: Int,
                        @SerializedName("avatarImgId")
                        val avatarImgId: Long,
                        @SerializedName("defaultAvatar")
                        val defaultAvatar: Boolean,
                        @SerializedName("avatarImgIdStr")
                        val avatarImgIdStr: String,
                        @SerializedName("backgroundImgIdStr")
                        val backgroundImgIdStr: String,
                        @SerializedName("province")
                        val province: Int,
                        @SerializedName("nickname")
                        val nickname: String,
                        @SerializedName("djStatus")
                        val djStatus: Int,
                        @SerializedName("avatarUrl")
                        val avatarUrl: String,
                        @SerializedName("authStatus")
                        val authStatus: Int,
                        @SerializedName("vipType")
                        val vipType: Int,
                        @SerializedName("followed")
                        val followed: Boolean,
                        @SerializedName("userId")
                        val userId: Long,
                        @SerializedName("authenticationTypes")
                        val authenticationTypes: Int,
                        @SerializedName("mutual")
                        val mutual: Boolean,
                        @SerializedName("authority")
                        val authority: Int,
                        @SerializedName("anchor")
                        val anchor: Boolean,
                        @SerializedName("userType")
                        val userType: Int,
                        @SerializedName("backgroundImgId")
                        val backgroundImgId: Long
                    )

                    data class ClassicRelationSong(
                        @SerializedName("voiceName")
                        val voiceName: String,
                        @SerializedName("redirectText")
                        val redirectText: String,
                        @SerializedName("hotScore")
                        val hotScore: Int,
                        @SerializedName("bubbleClickUrl")
                        val bubbleClickUrl: String,
                        @SerializedName("mainTitleIcon")
                        val mainTitleIcon: MainTitleIcon,
                        @SerializedName("titleImage")
                        val titleImage: TitleImage,
                        @SerializedName("subTitle")
                        val subTitle: String,
                        @SerializedName("mainTitle")
                        val mainTitle: String,
                        @SerializedName("musicStyles")
                        val musicStyles: List<Int>,
                        @SerializedName("commentClickUrl")
                        val commentClickUrl: String,
                        @SerializedName("songDataList")
                        val songDataList: List<SongData>,
                        @SerializedName("shortName")
                        val shortName: String,
                        @SerializedName("bubbleTipText")
                        val bubbleTipText: String,
                        @SerializedName("voiceCollectionUrl")
                        val voiceCollectionUrl: String
                    ) {
                        data class MainTitleIcon(
                            @SerializedName("width")
                            val width: Int,
                            @SerializedName("url")
                            val url: String,
                            @SerializedName("height")
                            val height: Int
                        )

                        data class TitleImage(
                            @SerializedName("width")
                            val width: Int,
                            @SerializedName("url")
                            val url: String,
                            @SerializedName("height")
                            val height: Int
                        )

                        data class SongData(
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("id")
                            val id: Long
                        )
                    }

                    data class Radio(
                        @SerializedName("lastProgramId")
                        val lastProgramId: Long,
                        @SerializedName("secondCategory")
                        val secondCategory: String,
                        @SerializedName("originalPrice")
                        val originalPrice: Int,
                        @SerializedName("purchaseCount")
                        val purchaseCount: Int,
                        @SerializedName("radioFeeType")
                        val radioFeeType: Int,
                        @SerializedName("feeScope")
                        val feeScope: Int,
                        @SerializedName("lastProgramCreateTime")
                        val lastProgramCreateTime: Long,
                        @SerializedName("privacy")
                        val privacy: Boolean,
                        @SerializedName("intervenePicId")
                        val intervenePicId: Long,
                        @SerializedName("subCount")
                        val subCount: Int,
                        @SerializedName("picUrl")
                        val picUrl: String,
                        @SerializedName("specialType")
                        val specialType: Int,
                        @SerializedName("subed")
                        val subed: Boolean,
                        @SerializedName("price")
                        val price: Int,
                        @SerializedName("danmakuCount")
                        val danmakuCount: Int,
                        @SerializedName("dynamic")
                        val `dynamic`: Boolean,
                        @SerializedName("id")
                        val id: Int,
                        @SerializedName("picId")
                        val picId: Long,
                        @SerializedName("participateUidList")
                        val participateUidList: List<Any>,
                        @SerializedName("userDeleted")
                        val userDeleted: Boolean,
                        @SerializedName("intervenePicUrl")
                        val intervenePicUrl: String,
                        @SerializedName("finished")
                        val finished: Boolean,
                        @SerializedName("programOrder")
                        val programOrder: Int,
                        @SerializedName("hightQuality")
                        val hightQuality: Boolean,
                        @SerializedName("composeVideo")
                        val composeVideo: Boolean,
                        @SerializedName("descPicList")
                        val descPicList: List<DescPic>,
                        @SerializedName("playCount")
                        val playCount: Int,
                        @SerializedName("deleted")
                        val deleted: Boolean,
                        @SerializedName("createTime")
                        val createTime: Long,
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("buyed")
                        val buyed: Boolean,
                        @SerializedName("whiteList")
                        val whiteList: Boolean,
                        @SerializedName("programCount")
                        val programCount: Int,
                        @SerializedName("underShelf")
                        val underShelf: Boolean,
                        @SerializedName("category")
                        val category: String,
                        @SerializedName("secondCategoryId")
                        val secondCategoryId: Int,
                        @SerializedName("onlySelfSee")
                        val onlySelfSee: Boolean,
                        @SerializedName("categoryId")
                        val categoryId: Int,
                        @SerializedName("taskId")
                        val taskId: Int,
                        @SerializedName("desc")
                        val desc: String,
                        @SerializedName("original")
                        val original: String,
                        @SerializedName("rcmdText")
                        val rcmdText: String
                    ) {
                        data class DescPic(
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("type")
                            val type: Int,
                            @SerializedName("imageContentURLInvalid")
                            val imageContentURLInvalid: Boolean,
                            @SerializedName("content")
                            val content: String,
                            @SerializedName("nestedData")
                            val nestedData: NestedData
                        ) {
                            data class NestedData(
                                @SerializedName("textList")
                                val textList: List<Text>
                            ) {
                                data class Text(
                                    @SerializedName("text")
                                    val text: String,
                                    @SerializedName("attributes")
                                    val attributes: Attributes
                                ) {
                                    data class Attributes(
                                        @SerializedName("bold")
                                        val bold: Boolean
                                    )
                                }
                            }
                        }
                    }

                    data class MainSong(
                        @SerializedName("no")
                        val no: Int,
                        @SerializedName("copyright")
                        val copyright: Int,
                        @SerializedName("dayPlays")
                        val dayPlays: Int,
                        @SerializedName("fee")
                        val fee: Int,
                        @SerializedName("mMusic")
                        val mMusic: MMusic,
                        @SerializedName("bMusic")
                        val bMusic: BMusic,
                        @SerializedName("duration")
                        val duration: Int,
                        @SerializedName("score")
                        val score: Int,
                        @SerializedName("starred")
                        val starred: Boolean,
                        @SerializedName("artists")
                        val artists: List<Artist>,
                        @SerializedName("rtUrls")
                        val rtUrls: List<Any>,
                        @SerializedName("popularity")
                        val popularity: Int,
                        @SerializedName("playedNum")
                        val playedNum: Int,
                        @SerializedName("hearTime")
                        val hearTime: Int,
                        @SerializedName("starredNum")
                        val starredNum: Int,
                        @SerializedName("alias")
                        val alias: List<Any>,
                        @SerializedName("id")
                        val id: Long,
                        @SerializedName("lMusic")
                        val lMusic: LMusic,
                        @SerializedName("album")
                        val album: Album,
                        @SerializedName("originCoverType")
                        val originCoverType: Int,
                        @SerializedName("commentThreadId")
                        val commentThreadId: String,
                        @SerializedName("ringtone")
                        val ringtone: String,
                        @SerializedName("copyFrom")
                        val copyFrom: String,
                        @SerializedName("single")
                        val single: Int,
                        @SerializedName("ftype")
                        val ftype: Int,
                        @SerializedName("copyrightId")
                        val copyrightId: Int,
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("disc")
                        val disc: String,
                        @SerializedName("position")
                        val position: Int,
                        @SerializedName("mark")
                        val mark: Int,
                        @SerializedName("status")
                        val status: Int,
                        @SerializedName("hMusic")
                        val hMusic: HMusic
                    ) {
                        data class MMusic(
                            @SerializedName("extension")
                            val extension: String,
                            @SerializedName("size")
                            val size: Int,
                            @SerializedName("volumeDelta")
                            val volumeDelta: Int,
                            @SerializedName("bitrate")
                            val bitrate: Int,
                            @SerializedName("playTime")
                            val playTime: Int,
                            @SerializedName("id")
                            val id: Long,
                            @SerializedName("dfsId")
                            val dfsId: Int,
                            @SerializedName("sr")
                            val sr: Int
                        )

                        data class BMusic(
                            @SerializedName("extension")
                            val extension: String,
                            @SerializedName("size")
                            val size: Int,
                            @SerializedName("volumeDelta")
                            val volumeDelta: Int,
                            @SerializedName("bitrate")
                            val bitrate: Int,
                            @SerializedName("playTime")
                            val playTime: Int,
                            @SerializedName("id")
                            val id: Long,
                            @SerializedName("dfsId")
                            val dfsId: Int,
                            @SerializedName("sr")
                            val sr: Int
                        )

                        data class Artist(
                            @SerializedName("img1v1Url")
                            val img1v1Url: String,
                            @SerializedName("picUrl")
                            val picUrl: String,
                            @SerializedName("topicPerson")
                            val topicPerson: Int,
                            @SerializedName("briefDesc")
                            val briefDesc: String,
                            @SerializedName("musicSize")
                            val musicSize: Int,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("alias")
                            val alias: List<Any>,
                            @SerializedName("img1v1Id")
                            val img1v1Id: Int,
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("albumSize")
                            val albumSize: Int,
                            @SerializedName("picId")
                            val picId: Int,
                            @SerializedName("trans")
                            val trans: String
                        )

                        data class LMusic(
                            @SerializedName("extension")
                            val extension: String,
                            @SerializedName("size")
                            val size: Int,
                            @SerializedName("volumeDelta")
                            val volumeDelta: Int,
                            @SerializedName("bitrate")
                            val bitrate: Int,
                            @SerializedName("playTime")
                            val playTime: Int,
                            @SerializedName("id")
                            val id: Long,
                            @SerializedName("dfsId")
                            val dfsId: Int,
                            @SerializedName("sr")
                            val sr: Int
                        )

                        data class Album(
                            @SerializedName("publishTime")
                            val publishTime: Int,
                            @SerializedName("dolbyMark")
                            val dolbyMark: Int,
                            @SerializedName("artist")
                            val artist: Artist,
                            @SerializedName("description")
                            val description: String,
                            @SerializedName("commentThreadId")
                            val commentThreadId: String,
                            @SerializedName("pic")
                            val pic: Long,
                            @SerializedName("gapless")
                            val gapless: Int,
                            @SerializedName("tags")
                            val tags: String,
                            @SerializedName("picUrl")
                            val picUrl: String,
                            @SerializedName("companyId")
                            val companyId: Int,
                            @SerializedName("size")
                            val size: Int,
                            @SerializedName("briefDesc")
                            val briefDesc: String,
                            @SerializedName("artists")
                            val artists: List<Artist>,
                            @SerializedName("copyrightId")
                            val copyrightId: Int,
                            @SerializedName("songs")
                            val songs: List<Any>,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("alias")
                            val alias: List<Any>,
                            @SerializedName("onSale")
                            val onSale: Boolean,
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("picId")
                            val picId: Long,
                            @SerializedName("mark")
                            val mark: Int,
                            @SerializedName("status")
                            val status: Int,
                            @SerializedName("extProperties")
                            val extProperties: ExtProperties,
                            @SerializedName("blurPicUrl")
                            val blurPicUrl: String,
                            @SerializedName("xInfo")
                            val xInfo: XInfo
                        ) {
                            data class Artist(
                                @SerializedName("img1v1Url")
                                val img1v1Url: String,
                                @SerializedName("picUrl")
                                val picUrl: String,
                                @SerializedName("topicPerson")
                                val topicPerson: Int,
                                @SerializedName("briefDesc")
                                val briefDesc: String,
                                @SerializedName("musicSize")
                                val musicSize: Int,
                                @SerializedName("name")
                                val name: String,
                                @SerializedName("alias")
                                val alias: List<Any>,
                                @SerializedName("img1v1Id")
                                val img1v1Id: Int,
                                @SerializedName("id")
                                val id: Int,
                                @SerializedName("albumSize")
                                val albumSize: Int,
                                @SerializedName("picId")
                                val picId: Int,
                                @SerializedName("trans")
                                val trans: String
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

                        data class HMusic(
                            @SerializedName("extension")
                            val extension: String,
                            @SerializedName("size")
                            val size: Int,
                            @SerializedName("volumeDelta")
                            val volumeDelta: Int,
                            @SerializedName("bitrate")
                            val bitrate: Int,
                            @SerializedName("playTime")
                            val playTime: Int,
                            @SerializedName("id")
                            val id: Long,
                            @SerializedName("dfsId")
                            val dfsId: Int,
                            @SerializedName("sr")
                            val sr: Int
                        )
                    }
                }

                data class Header(
                    @SerializedName("showMore")
                    val showMore: Boolean,
                    @SerializedName("action")
                    val action: String,
                    @SerializedName("title")
                    val title: String
                )

                data class Track(
                    @SerializedName("s_cid")
                    val sCid: String,
                    @SerializedName("s_ctype")
                    val sCtype: String,
                    @SerializedName("itemIds")
                    val itemIds: List<Long>,
                    @SerializedName("title")
                    val title: String,
                    @SerializedName("s_ctrp")
                    val sCtrp: String
                )

                data class Item(
                    @SerializedName("items")
                    val items: List<Item>
                ) {
                    data class Item(
                        @SerializedName("coverUrl")
                        val coverUrl: String,
                        @SerializedName("resourceId")
                        val resourceId: Long,
                        @SerializedName("subTitle")
                        val subTitle: String,
                        @SerializedName("isClassicSong")
                        val isClassicSong: Boolean,
                        @SerializedName("index")
                        val index: Int,
                        @SerializedName("title")
                        val title: String,
                        @SerializedName("track")
                        val track: Track,
                        @SerializedName("classicSongDesc")
                        val classicSongDesc: String,
                        @SerializedName("radioName")
                        val radioName: String,
                        @SerializedName("resourceType")
                        val resourceType: String,
                        @SerializedName("label")
                        val label: Label
                    ) {
                        data class Track(
                            @SerializedName("s_position")
                            val sPosition: Int,
                            @SerializedName("s_cid")
                            val sCid: Long,
                            @SerializedName("s_ctype")
                            val sCtype: String,
                            @SerializedName("s_calg")
                            val sCalg: String,
                            @SerializedName("text")
                            val text: String,
                            @SerializedName("s_ctrp")
                            val sCtrp: String
                        )

                        data class Label(
                            @SerializedName("labelSource")
                            val labelSource: String,
                            @SerializedName("labelSize")
                            val labelSize: String,
                            @SerializedName("text")
                            val text: String
                        )
                    }
                }

                data class ExtInfo(
                    @SerializedName("2065473022")
                    val x2065473022: X2065473022,
                    @SerializedName("2488727108")
                    val x2488727108: X2488727108,
                    @SerializedName("2508696560")
                    val x2508696560: X2508696560,
                    @SerializedName("2518145597")
                    val x2518145597: X2518145597,
                    @SerializedName("2522578775")
                    val x2522578775: X2522578775,
                    @SerializedName("2524310254")
                    val x2524310254: X2524310254,
                    @SerializedName("2524523882")
                    val x2524523882: X2524523882,
                    @SerializedName("2526969458")
                    val x2526969458: X2526969458,
                    @SerializedName("2537211653")
                    val x2537211653: X2537211653,
                    @SerializedName("2539114511")
                    val x2539114511: X2539114511,
                    @SerializedName("2539414505")
                    val x2539414505: X2539414505,
                    @SerializedName("3062599074")
                    val x3062599074: X3062599074
                ) {
                    data class X2065473022(
                        @SerializedName("coverImageInfo")
                        val coverImageInfo: String,
                        @SerializedName("title")
                        val title: String
                    )

                    data class X2488727108(
                        @SerializedName("coverImageInfo")
                        val coverImageInfo: String,
                        @SerializedName("title")
                        val title: String
                    )

                    data class X2508696560(
                        @SerializedName("coverImageInfo")
                        val coverImageInfo: String,
                        @SerializedName("title")
                        val title: String
                    )

                    data class X2518145597(
                        @SerializedName("coverImageInfo")
                        val coverImageInfo: String,
                        @SerializedName("title")
                        val title: String
                    )

                    data class X2522578775(
                        @SerializedName("coverImageInfo")
                        val coverImageInfo: String,
                        @SerializedName("title")
                        val title: String
                    )

                    data class X2524310254(
                        @SerializedName("coverImageInfo")
                        val coverImageInfo: String,
                        @SerializedName("title")
                        val title: String
                    )

                    data class X2524523882(
                        @SerializedName("coverImageInfo")
                        val coverImageInfo: String,
                        @SerializedName("title")
                        val title: String
                    )

                    data class X2526969458(
                        @SerializedName("coverImageInfo")
                        val coverImageInfo: String,
                        @SerializedName("title")
                        val title: String
                    )

                    data class X2537211653(
                        @SerializedName("coverImageInfo")
                        val coverImageInfo: String,
                        @SerializedName("title")
                        val title: String
                    )

                    data class X2539114511(
                        @SerializedName("coverImageInfo")
                        val coverImageInfo: String,
                        @SerializedName("title")
                        val title: String
                    )

                    data class X2539414505(
                        @SerializedName("coverImageInfo")
                        val coverImageInfo: String,
                        @SerializedName("title")
                        val title: String
                    )

                    data class X3062599074(
                        @SerializedName("coverImageInfo")
                        val coverImageInfo: String,
                        @SerializedName("title")
                        val title: String
                    )
                }

                data class HomeCommon(
                    @SerializedName("ctype")
                    val ctype: String,
                    @SerializedName("header")
                    val header: Header,
                    @SerializedName("oid")
                    val oid: String,
                    @SerializedName("content")
                    val content: Content
                ) {
                    data class Header(
                        @SerializedName("showMore")
                        val showMore: Boolean,
                        @SerializedName("title")
                        val title: String
                    )

                    data class Content(
                        @SerializedName("oid")
                        val oid: String,
                        @SerializedName("items")
                        val items: List<Item>
                    ) {
                        data class Item(
                            @SerializedName("items")
                            val items: List<Item>
                        ) {
                            data class Item(
                                @SerializedName("likeDisplay")
                                val likeDisplay: String,
                                @SerializedName("resourceId")
                                val resourceId: String,
                                @SerializedName("tagDisplay")
                                val tagDisplay: String,
                                @SerializedName("like")
                                val like: Int,
                                @SerializedName("singleSongCtrp")
                                val singleSongCtrp: String,
                                @SerializedName("recReasonDisplay")
                                val recReasonDisplay: String,
                                @SerializedName("oid")
                                val oid: String,
                                @SerializedName("title")
                                val title: String,
                                @SerializedName("coverUrl")
                                val coverUrl: String,
                                @SerializedName("recReason")
                                val recReason: String,
                                @SerializedName("artistName")
                                val artistName: String,
                                @SerializedName("position")
                                val position: Int,
                                @SerializedName("tag")
                                val tag: String,
                                @SerializedName("playBtn")
                                val playBtn: PlayBtn,
                                @SerializedName("alg")
                                val alg: String,
                                @SerializedName("clickAction")
                                val clickAction: ClickAction,
                                @SerializedName("resourceType")
                                val resourceType: String
                            ) {
                                data class PlayBtn(
                                    @SerializedName("pauseType")
                                    val pauseType: String,
                                    @SerializedName("resourceId")
                                    val resourceId: String,
                                    @SerializedName("playAction")
                                    val playAction: PlayAction,
                                    @SerializedName("playType")
                                    val playType: String,
                                    @SerializedName("tintColor")
                                    val tintColor: String,
                                    @SerializedName("detailUrl")
                                    val detailUrl: String
                                ) {
                                    data class PlayAction(
                                        @SerializedName("songIndex")
                                        val songIndex: Int,
                                        @SerializedName("songIds")
                                        val songIds: List<String>,
                                        @SerializedName("algs")
                                        val algs: List<String>,
                                        @SerializedName("playParams")
                                        val playParams: PlayParams
                                    ) {
                                        data class PlayParams(
                                            @SerializedName("sourceResourceName")
                                            val sourceResourceName: String,
                                            @SerializedName("trialSceneMode")
                                            val trialSceneMode: Int,
                                            @SerializedName("sourceDes")
                                            val sourceDes: String,
                                            @SerializedName("playerType")
                                            val playerType: String,
                                            @SerializedName("playingShowUI")
                                            val playingShowUI: Boolean,
                                            @SerializedName("showUI")
                                            val showUI: Boolean,
                                            @SerializedName("sourceModuleName")
                                            val sourceModuleName: String
                                        )
                                    }
                                }

                                data class ClickAction(
                                    @SerializedName("msg")
                                    val msg: Msg,
                                    @SerializedName("type")
                                    val type: String
                                ) {
                                    data class Msg(
                                        @SerializedName("method")
                                        val method: String,
                                        @SerializedName("module")
                                        val module: String,
                                        @SerializedName("params")
                                        val params: Params
                                    ) {
                                        data class Params(
                                            @SerializedName("songIndex")
                                            val songIndex: Int,
                                            @SerializedName("songIds")
                                            val songIds: List<String>,
                                            @SerializedName("algs")
                                            val algs: List<String>,
                                            @SerializedName("playParams")
                                            val playParams: PlayParams
                                        ) {
                                            data class PlayParams(
                                                @SerializedName("sourceResourceName")
                                                val sourceResourceName: String,
                                                @SerializedName("trialSceneMode")
                                                val trialSceneMode: Int,
                                                @SerializedName("sourceDes")
                                                val sourceDes: String,
                                                @SerializedName("playerType")
                                                val playerType: String,
                                                @SerializedName("playingShowUI")
                                                val playingShowUI: Boolean,
                                                @SerializedName("showUI")
                                                val showUI: Boolean,
                                                @SerializedName("sourceModuleName")
                                                val sourceModuleName: String
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            data class ExtMap(
                @SerializedName("demote")
                val demote: String,
                @SerializedName("cacheable")
                val cacheable: String,
                @SerializedName("clientCacheExpireTime")
                val clientCacheExpireTime: String
            )

            data class LogMap(
                @SerializedName("cc")
                val cc: String,
                @SerializedName("pc")
                val pc: String,
                @SerializedName("fgid")
                val fgid: String
            )
        }
    }
}