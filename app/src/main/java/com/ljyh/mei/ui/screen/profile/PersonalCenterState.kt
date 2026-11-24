package com.ljyh.mei.ui.screen.profile

import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.model.response.UserVipInfo
import com.ljyh.mei.ui.screen.main.library.LibraryProfileUi

internal data class PersonalCenterState(
    val signedIn: Boolean,
    val profile: LibraryProfileUi?,
    val membership: ProfileMembership,
    val loading: Boolean,
    val refreshing: Boolean,
    val accountError: Boolean,
    val statisticsError: Boolean,
    val footprint: com.ljyh.mei.ui.screen.main.library.ListeningFootprintUi = com.ljyh.mei.ui.screen.main.library.ListeningFootprintUi(),
    val reportStatus: Resource<Unit> = Resource.Loading,
    val archive: ListeningArchiveState = ListeningArchiveState(),
)

internal data class PersonalCenterActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onManageAccount: () -> Unit,
    val onLogout: () -> Unit,
    val onRankPeriod: (Boolean) -> Unit = {},
    val onOpenRecentListening: () -> Unit = {},
    val onPlay: (List<com.ljyh.mei.data.model.response.ListeningTrack>, Int, String) -> Unit = { _, _, _ -> },
)

internal data class ProfileMembership(val label: String, val status: String, val expireTime: Long? = null)

internal fun profileMembership(resource: Resource<UserVipInfo>, now: Long): ProfileMembership {
    if (resource is Resource.Loading) return ProfileMembership("会员信息", "同步中")
    if (resource !is Resource.Success || resource.data.code != 200) return ProfileMembership("会员信息", "暂时无法获取")
    val data = resource.data.data ?: return ProfileMembership("会员信息", "暂无数据")
    val benefits = listOf("SVIP" to data.redplus, "VIP" to data.associator,
        "家庭会员" to data.familyVip, "音乐包" to data.musicPackage)
        .mapNotNull { (label, benefit) -> benefit?.takeIf { it.expireTime > 0 }?.let { label to it } }
    val active = benefits.firstOrNull { it.second.vipLevel > 0 && it.second.expireTime > now }
    if (active != null) return ProfileMembership(active.first, "有效", active.second.expireTime)
    val expired = benefits.filter { it.second.expireTime <= now }.maxByOrNull { it.second.expireTime }
    return if (expired != null) ProfileMembership(expired.first, "已过期", expired.second.expireTime)
    else ProfileMembership("暂无有效会员", "未开通")
}
