package com.ljyh.mei.data.model.weapi
import com.google.gson.annotations.SerializedName
data class Comment(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data1,
    @SerializedName("message")
    val message: String
)

data class Data1(
    @SerializedName("bottomAction")
    val bottomAction: Any,
    @SerializedName("comments")
    val comments: List<CommentX>,
    @SerializedName("commentsTitle")
    val commentsTitle: String,
    @SerializedName("currentComment")
    val currentComment: Any,
    @SerializedName("currentCommentTitle")
    val currentCommentTitle: String,
    @SerializedName("cursor")
    val cursor: String,
    @SerializedName("expandCount")
    val expandCount: Int,
    @SerializedName("hasMore")
    val hasMore: Boolean,
    @SerializedName("likeAnimation")
    val likeAnimation: LikeAnimation,
    @SerializedName("newReplyExpGroupName")
    val newReplyExpGroupName: String,
    @SerializedName("sortType")
    val sortType: Int,
    @SerializedName("sortTypeList")
    val sortTypeList: List<SortType>,
    @SerializedName("style")
    val style: String,
    @SerializedName("totalCount")
    val totalCount: Int
)

data class CommentX(
    @SerializedName("aiCommentLabel")
    val aiCommentLabel: Any,
    @SerializedName("airborneAction")
    val airborneAction: Any,
    @SerializedName("args")
    val args: Any,
    @SerializedName("beReplied")
    val beReplied: Any,
    @SerializedName("bottomTags")
    val bottomTags: List<Any>,
    @SerializedName("commentId")
    val commentId: Long,
    @SerializedName("commentLocationType")
    val commentLocationType: Int,
    @SerializedName("commentVideoVO")
    val commentVideoVO: CommentVideoVO,
    @SerializedName("content")
    val content: String,
    @SerializedName("contentPicExt")
    val contentPicExt: Any,
    @SerializedName("contentPicNosKey")
    val contentPicNosKey: Any,
    @SerializedName("contentPicUrl")
    val contentPicUrl: Any,
    @SerializedName("contentResource")
    val contentResource: Any,
    @SerializedName("decoration")
    val decoration: Decoration,
    @SerializedName("expressionUrl")
    val expressionUrl: Any,
    @SerializedName("extInfo")
    val extInfo: ExtInfo,
    @SerializedName("favorited")
    val favorited: Boolean,
    @SerializedName("grade")
    val grade: Any,
    @SerializedName("hideSerialComments")
    val hideSerialComments: Any,
    @SerializedName("hideSerialTips")
    val hideSerialTips: Any,
    @SerializedName("highlight")
    val highlight: Boolean,
    @SerializedName("ipLocation")
    val ipLocation: IpLocation,
    @SerializedName("likeAnimationMap")
    val likeAnimationMap: LikeAnimationMap,
    @SerializedName("liked")
    val liked: Boolean,
    @SerializedName("likedCount")
    val likedCount: Int,
    @SerializedName("medal")
    val medal: Any,
    @SerializedName("musicianSayAirborne")
    val musicianSayAirborne: Any,
    @SerializedName("needDisplayTime")
    val needDisplayTime: Boolean,
    @SerializedName("outShowComments")
    val outShowComments: List<Any>,
    @SerializedName("owner")
    val owner: Boolean,
    @SerializedName("parentCommentId")
    val parentCommentId: Int,
    @SerializedName("pendantData")
    val pendantData: PendantData,
    @SerializedName("pickInfo")
    val pickInfo: Any,
    @SerializedName("privacy")
    val privacy: Int,
    @SerializedName("repliedMark")
    val repliedMark: Boolean,
    @SerializedName("replyCount")
    val replyCount: Int,
    @SerializedName("resourceSpecialType")
    val resourceSpecialType: Any,
    @SerializedName("reward")
    val reward: Any,
    @SerializedName("richContent")
    val richContent: String,
    @SerializedName("showFloorComment")
    val showFloorComment: ShowFloorComment,
    @SerializedName("source")
    val source: Any,
    @SerializedName("status")
    val status: Int,
    @SerializedName("tag")
    val tag: Tag,
    @SerializedName("tail")
    val tail: Any,
    @SerializedName("threadId")
    val threadId: String,
    @SerializedName("time")
    val time: Long,
    @SerializedName("timeStr")
    val timeStr: String,
    @SerializedName("topicList")
    val topicList: Any,
    @SerializedName("track")
    val track: String,
    @SerializedName("user")
    val user: User,
    @SerializedName("userBizLevels")
    val userBizLevels: Any,
    @SerializedName("userNameplates")
    val userNameplates: Any,
    @SerializedName("userTop")
    val userTop: Boolean,
    @SerializedName("voiceDurationMillSecond")
    val voiceDurationMillSecond: Int,
    @SerializedName("voiceExt")
    val voiceExt: Any,
    @SerializedName("voiceNosKey")
    val voiceNosKey: Any,
    @SerializedName("voiceWhaleId")
    val voiceWhaleId: Any,
    @SerializedName("wordMatchList")
    val wordMatchList: Any
)

data class LikeAnimation(
    @SerializedName("animationConfigMap")
    val animationConfigMap: AnimationConfigMap,
    @SerializedName("version")
    val version: Long
)

data class SortType(
    @SerializedName("sortType")
    val sortType: Int,
    @SerializedName("sortTypeName")
    val sortTypeName: String,
    @SerializedName("target")
    val target: String
)

data class CommentVideoVO(
    @SerializedName("allowCreation")
    val allowCreation: Boolean,
    @SerializedName("creationOrpheusUrl")
    val creationOrpheusUrl: Any,
    @SerializedName("forbidCreationText")
    val forbidCreationText: String,
    @SerializedName("playOrpheusUrl")
    val playOrpheusUrl: Any,
    @SerializedName("showCreationEntrance")
    val showCreationEntrance: Boolean,
    @SerializedName("videoCount")
    val videoCount: Int
)

data class Decoration(
    @SerializedName("repliedByAuthorCount")
    val repliedByAuthorCount: Int
)

data class ExtInfo(
    @SerializedName("forwardEvent")
    val forwardEvent: Int,
    @SerializedName("source")
    val source: Source
)

data class IpLocation(
    @SerializedName("ip")
    val ip: Any,
    @SerializedName("location")
    val location: String,
    @SerializedName("userId")
    val userId: Long
)

class LikeAnimationMap

data class PendantData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("imageUrl")
    val imageUrl: String
)

data class ShowFloorComment(
    @SerializedName("comments")
    val comments: Any,
    @SerializedName("replyCount")
    val replyCount: Int,
    @SerializedName("showReplyCount")
    val showReplyCount: Boolean,
    @SerializedName("target")
    val target: Any,
    @SerializedName("topCommentIds")
    val topCommentIds: Any
)

data class Tag(
    @SerializedName("contentDatas")
    val contentDatas: List<Any>,
    @SerializedName("contentPicDatas")
    val contentPicDatas: List<Any>,
    @SerializedName("datas")
    val datas: List<Any>,
    @SerializedName("extDatas")
    val extDatas: List<Any>,
    @SerializedName("relatedCommentIds")
    val relatedCommentIds: Any
)

data class User(
    @SerializedName("anonym")
    val anonym: Int,
    @SerializedName("authStatus")
    val authStatus: Int,
    @SerializedName("avatarDetail")
    val avatarDetail: AvatarDetail,
    @SerializedName("avatarUrl")
    val avatarUrl: String,
    @SerializedName("commonIdentity")
    val commonIdentity: Any,
    @SerializedName("encryptUserId")
    val encryptUserId: String,
    @SerializedName("expertTags")
    val expertTags: Any,
    @SerializedName("experts")
    val experts: Any,
    @SerializedName("followed")
    val followed: Boolean,
    @SerializedName("isHug")
    val isHug: Boolean,
    @SerializedName("liveInfo")
    val liveInfo: Any,
    @SerializedName("locationInfo")
    val locationInfo: Any,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("relationTag")
    val relationTag: Any,
    @SerializedName("remarkName")
    val remarkName: Any,
    @SerializedName("socialUserId")
    val socialUserId: Any,
    @SerializedName("target")
    val target: Any,
    @SerializedName("userId")
    val userId: Long,
    @SerializedName("userType")
    val userType: Int,
    @SerializedName("vipRights")
    val vipRights: VipRights?,
    @SerializedName("vipType")
    val vipType: Int
)

data class Source(
    @SerializedName("iconUrl")
    val iconUrl: Any,
    @SerializedName("id")
    val id: Any,
    @SerializedName("keys")
    val keys: Any,
    @SerializedName("orpheus")
    val orpheus: Any,
    @SerializedName("text")
    val text: Any,
    @SerializedName("type")
    val type: Int
)



data class VipRights(
    @SerializedName("associator")
    val associator: Associator,
    @SerializedName("extInfo")
    val extInfo: Any,
    @SerializedName("memberLogo")
    val memberLogo: Any,
    @SerializedName("musicPackage")
    val musicPackage: MusicPackage,
    @SerializedName("redVipAnnualCount")
    val redVipAnnualCount: Int,
    @SerializedName("redVipLevel")
    val redVipLevel: Int,
    @SerializedName("redplus")
    val redplus: Redplus,
    @SerializedName("relationType")
    val relationType: Int
)

data class Associator(
    @SerializedName("iconUrl")
    val iconUrl: String,
    @SerializedName("rights")
    val rights: Boolean,
    @SerializedName("vipCode")
    val vipCode: Int
)

data class MusicPackage(
    @SerializedName("iconUrl")
    val iconUrl: String,
    @SerializedName("rights")
    val rights: Boolean,
    @SerializedName("vipCode")
    val vipCode: Int
)

data class Redplus(
    @SerializedName("iconUrl")
    val iconUrl: String,
    @SerializedName("rights")
    val rights: Boolean,
    @SerializedName("vipCode")
    val vipCode: Int
)

data class AnimationConfigMap(
    @SerializedName("COMMENT_AREA")
    val cOMMENTAREA: List<Any>,
    @SerializedName("EVENT_FEED")
    val eVENTFEED: List<Any>,
    @SerializedName("INPUT")
    val iNPUT: List<Any>,
    @SerializedName("MOMENT")
    val mOMENT: List<Any>
)



