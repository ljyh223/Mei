package com.ljyh.mei.data.model.api
import com.google.gson.annotations.SerializedName



data class GetArtistDetail(
    val id: String,
)

data class ArtistDetail(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val `data`: Data
) {
    data class Data(
        @SerializedName("videoCount")
        val videoCount: Int,
        @SerializedName("vipRights")
        val vipRights: VipRights,
        @SerializedName("identify")
        val identify: Identify,
        @SerializedName("artist")
        val artist: Artist,
        @SerializedName("blacklist")
        val blacklist: Boolean,
        @SerializedName("preferShow")
        val preferShow: Int,
        @SerializedName("showPriMsg")
        val showPriMsg: Boolean,
        @SerializedName("secondaryExpertIdentiy")
        val secondaryExpertIdentiy: List<SecondaryExpertIdentiy>,
        @SerializedName("eventCount")
        val eventCount: Int,
        @SerializedName("user")
        val user: User
    ) {
        data class VipRights(
            @SerializedName("rightsInfoDetailDtoList")
            val rightsInfoDetailDtoList: List<RightsInfoDetailDto>,
            @SerializedName("oldProtocol")
            val oldProtocol: Boolean,
            @SerializedName("redVipAnnualCount")
            val redVipAnnualCount: Int,
            @SerializedName("redVipLevel")
            val redVipLevel: Int,
            @SerializedName("now")
            val now: Long
        ) {
            data class RightsInfoDetailDto(
                @SerializedName("vipCode")
                val vipCode: Int,
                @SerializedName("expireTime")
                val expireTime: Long,
                @SerializedName("iconUrl")
                val iconUrl: Any,
                @SerializedName("dynamicIconUrl")
                val dynamicIconUrl: Any,
                @SerializedName("vipLevel")
                val vipLevel: Int,
                @SerializedName("signIap")
                val signIap: Boolean,
                @SerializedName("signDeduct")
                val signDeduct: Boolean,
                @SerializedName("signIapDeduct")
                val signIapDeduct: Boolean,
                @SerializedName("sign")
                val sign: Boolean
            )
        }

        data class Identify(
            @SerializedName("imageUrl")
            val imageUrl: String,
            @SerializedName("imageDesc")
            val imageDesc: String,
            @SerializedName("actionUrl")
            val actionUrl: String
        )

        data class Artist(
            @SerializedName("id")
            val id: Int,
            @SerializedName("cover")
            val cover: String,
            @SerializedName("avatar")
            val avatar: String,
            @SerializedName("name")
            val name: String,
            @SerializedName("transNames")
            val transNames: List<String>,
            @SerializedName("alias")
            val alias: List<Any>,
            @SerializedName("identities")
            val identities: List<Any>,
            @SerializedName("identifyTag")
            val identifyTag: List<String>,
            @SerializedName("briefDesc")
            val briefDesc: String,
            @SerializedName("rank")
            val rank: Rank,
            @SerializedName("albumSize")
            val albumSize: Int,
            @SerializedName("musicSize")
            val musicSize: Int,
            @SerializedName("mvSize")
            val mvSize: Int
        ) {
            data class Rank(
                @SerializedName("rank")
                val rank: Int,
                @SerializedName("type")
                val type: Int
            )
        }

        data class SecondaryExpertIdentiy(
            @SerializedName("expertIdentiyId")
            val expertIdentiyId: Int,
            @SerializedName("expertIdentiyName")
            val expertIdentiyName: String,
            @SerializedName("expertIdentiyCount")
            val expertIdentiyCount: Int
        )

        data class User(
            @SerializedName("backgroundUrl")
            val backgroundUrl: String,
            @SerializedName("birthday")
            val birthday: Long,
            @SerializedName("detailDescription")
            val detailDescription: String,
            @SerializedName("authenticated")
            val authenticated: Boolean,
            @SerializedName("gender")
            val gender: Int,
            @SerializedName("city")
            val city: Int,
            @SerializedName("signature")
            val signature: String,
            @SerializedName("description")
            val description: String,
            @SerializedName("remarkName")
            val remarkName: Any,
            @SerializedName("shortUserName")
            val shortUserName: String,
            @SerializedName("accountStatus")
            val accountStatus: Int,
            @SerializedName("locationStatus")
            val locationStatus: Int,
            @SerializedName("avatarImgId")
            val avatarImgId: Long,
            @SerializedName("defaultAvatar")
            val defaultAvatar: Boolean,
            @SerializedName("province")
            val province: Int,
            @SerializedName("nickname")
            val nickname: String,
            @SerializedName("expertTags")
            val expertTags: Any,
            @SerializedName("djStatus")
            val djStatus: Int,
            @SerializedName("avatarUrl")
            val avatarUrl: String,
            @SerializedName("accountType")
            val accountType: Int,
            @SerializedName("authStatus")
            val authStatus: Int,
            @SerializedName("vipType")
            val vipType: Int,
            @SerializedName("userName")
            val userName: String,
            @SerializedName("followed")
            val followed: Boolean,
            @SerializedName("userId")
            val userId: Long,
            @SerializedName("lastLoginIP")
            val lastLoginIP: String,
            @SerializedName("lastLoginTime")
            val lastLoginTime: Long,
            @SerializedName("authenticationTypes")
            val authenticationTypes: Int,
            @SerializedName("mutual")
            val mutual: Boolean,
            @SerializedName("createTime")
            val createTime: Long,
            @SerializedName("anchor")
            val anchor: Boolean,
            @SerializedName("authority")
            val authority: Int,
            @SerializedName("backgroundImgId")
            val backgroundImgId: Long,
            @SerializedName("userType")
            val userType: Int,
            @SerializedName("experts")
            val experts: Any,
            @SerializedName("avatarDetail")
            val avatarDetail: AvatarDetail
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
}

