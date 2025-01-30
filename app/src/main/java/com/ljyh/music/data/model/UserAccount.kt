package com.ljyh.music.data.model
import com.google.gson.annotations.SerializedName


data class UserAccount(
    @SerializedName("account")
    val account: Account,
    @SerializedName("code")
    val code: Int,
    @SerializedName("profile")
    val profile: Profile
) {
    data class Account(
        @SerializedName("anonimousUser")
        val anonimousUser: Boolean,
        @SerializedName("ban")
        val ban: Int,
        @SerializedName("baoyueVersion")
        val baoyueVersion: Int,
        @SerializedName("createTime")
        val createTime: Long,
        @SerializedName("donateVersion")
        val donateVersion: Int,
        @SerializedName("id")
        val id: Long,
        @SerializedName("paidFee")
        val paidFee: Boolean,
        @SerializedName("status")
        val status: Int,
        @SerializedName("tokenVersion")
        val tokenVersion: Int,
        @SerializedName("type")
        val type: Int,
        @SerializedName("userName")
        val userName: String,
        @SerializedName("vipType")
        val vipType: Int,
        @SerializedName("whitelistAuthority")
        val whitelistAuthority: Int
    )

    data class Profile(
        @SerializedName("accountStatus")
        val accountStatus: Int,
        @SerializedName("accountType")
        val accountType: Int,
        @SerializedName("anchor")
        val anchor: Boolean,
        @SerializedName("authStatus")
        val authStatus: Int,
        @SerializedName("authenticated")
        val authenticated: Boolean,
        @SerializedName("authenticationTypes")
        val authenticationTypes: Int,
        @SerializedName("authority")
        val authority: Int,
        @SerializedName("avatarDetail")
        val avatarDetail: Any,
        @SerializedName("avatarImgId")
        val avatarImgId: Long,
        @SerializedName("avatarUrl")
        val avatarUrl: String,
        @SerializedName("backgroundImgId")
        val backgroundImgId: Long,
        @SerializedName("backgroundUrl")
        val backgroundUrl: String,
        @SerializedName("birthday")
        val birthday: Long,
        @SerializedName("city")
        val city: Int,
        @SerializedName("createTime")
        val createTime: Long,
        @SerializedName("defaultAvatar")
        val defaultAvatar: Boolean,
        @SerializedName("description")
        val description: Any,
        @SerializedName("detailDescription")
        val detailDescription: Any,
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
        @SerializedName("lastLoginIP")
        val lastLoginIP: String,
        @SerializedName("lastLoginTime")
        val lastLoginTime: Long,
        @SerializedName("locationStatus")
        val locationStatus: Int,
        @SerializedName("mutual")
        val mutual: Boolean,
        @SerializedName("nickname")
        val nickname: String,
        @SerializedName("province")
        val province: Int,
        @SerializedName("remarkName")
        val remarkName: Any,
        @SerializedName("shortUserName")
        val shortUserName: String,
        @SerializedName("signature")
        val signature: String,
        @SerializedName("userId")
        val userId: Long,
        @SerializedName("userName")
        val userName: String,
        @SerializedName("userType")
        val userType: Int,
        @SerializedName("vipType")
        val vipType: Int,
        @SerializedName("viptypeVersion")
        val viptypeVersion: Long
    )
}