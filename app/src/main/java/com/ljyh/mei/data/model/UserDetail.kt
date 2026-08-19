package com.ljyh.mei.data.model

import com.google.gson.annotations.SerializedName

data class UserDetail(
    @SerializedName("code") val code: Int,
    @SerializedName("level") val level: Int,
    @SerializedName("listenSongs") val listenSongs: Int,
    @SerializedName("profile") val profile: Profile,
) {
    data class Profile(
        @SerializedName("follows") val follows: Int,
        @SerializedName("followeds") val followeds: Int,
    )
}

data class UserVipInfo(
    @SerializedName("message") val message: String,
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: Data?,
) {
    data class Data(
        @SerializedName("redVipLevelIcon") val redVipLevelIcon: String?,
        @SerializedName("redVipLevel") val redVipLevel: Int,
        @SerializedName("redVipAnnualCount") val redVipAnnualCount: Int,
        @SerializedName("associator") val associator: Benefit?,
        @SerializedName("musicPackage") val musicPackage: Benefit?,
        @SerializedName("familyVip") val familyVip: Benefit?,
        @SerializedName("redplus") val redplus: Benefit?,
        @SerializedName("redVipDynamicIconUrl") val redVipDynamicIconUrl: String?,
        @SerializedName("redVipDynamicIconUrl2") val redVipDynamicIconUrl2: String?,
    )

    data class Benefit(
        @SerializedName("vipCode") val vipCode: Int = 0,
        @SerializedName("expireTime") val expireTime: Long = 0L,
        @SerializedName("iconUrl") val iconUrl: String?,
        @SerializedName("dynamicIconUrl") val dynamicIconUrl: String?,
        @SerializedName("vipLevel") val vipLevel: Int = 0,
        @SerializedName("isSign") val isSign: Boolean = false,
        @SerializedName("isSignDeduct") val isSignDeduct: Boolean = false,
        @SerializedName("isSignIap") val isSignIap: Boolean = false,
        @SerializedName("isSignIapDeduct") val isSignIapDeduct: Boolean = false,
    )

    private fun Benefit?.isActive(now: Long): Boolean =
        this != null && vipLevel > 0 && expireTime > now

    val label: String?
        get() {
            val now = System.currentTimeMillis()
            return when {
                data?.redplus.isActive(now) -> "SVIP"
                data?.associator.isActive(now) -> "VIP"
                data?.musicPackage.isActive(now) -> "音乐包"
                else -> null
            }
        }

    val iconUrl: String?
        get() {
            val now = System.currentTimeMillis()
            return when {
                data?.redplus.isActive(now) -> data?.redplus?.iconUrl
                data?.associator.isActive(now) -> data?.associator?.iconUrl ?: data?.redVipLevelIcon
                data?.musicPackage.isActive(now) -> data?.musicPackage?.iconUrl
                else -> null
            }
        }
}
