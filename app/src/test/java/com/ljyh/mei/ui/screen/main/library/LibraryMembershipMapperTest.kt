package com.ljyh.mei.ui.screen.main.library

import com.ljyh.mei.data.model.UserVipInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryMembershipMapperTest {
    @Test
    fun activeSvipTakesPriority() {
        val vip = vipInfo(
            associator = benefit(100, expireTime = 2_000L, iconUrl = "vip"),
            redplus = benefit(300, expireTime = 2_000L, iconUrl = "svip"),
        )

        assertEquals(MembershipUi("SVIP", "svip"), vip.toMembershipUi(now = 1_000L))
    }

    @Test
    fun expiredSvipFallsBackToActiveVip() {
        val vip = vipInfo(
            associator = benefit(100, expireTime = 2_000L, iconUrl = "vip"),
            redplus = benefit(300, expireTime = 500L, iconUrl = "svip"),
        )

        assertEquals(MembershipUi("VIP", "vip"), vip.toMembershipUi(now = 1_000L))
    }

    private fun vipInfo(
        associator: UserVipInfo.Benefit?,
        redplus: UserVipInfo.Benefit?,
    ) = UserVipInfo(
        message = "成功",
        code = 200,
        data = UserVipInfo.Data(
            redVipLevelIcon = "level",
            redVipLevel = 7,
            redVipAnnualCount = 1,
            associator = associator,
            musicPackage = null,
            familyVip = null,
            redplus = redplus,
            redVipDynamicIconUrl = null,
            redVipDynamicIconUrl2 = null,
        ),
    )

    private fun benefit(
        vipCode: Int,
        expireTime: Long,
        iconUrl: String,
    ) = UserVipInfo.Benefit(
        vipCode = vipCode,
        expireTime = expireTime,
        iconUrl = iconUrl,
        dynamicIconUrl = null,
        vipLevel = 7,
    )
}
