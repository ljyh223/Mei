package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.api.Intelligence
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaMetadata
import com.ljyh.mei.data.network.Resource

internal fun intelligencePlaybackTracks(
    response: Intelligence,
    seed: MediaMetadata?,
): Resource<List<MediaMetadata>> {
    if (response.code != 200) {
        return Resource.Error(response.message?.takeIf(String::isNotBlank) ?: "心动模式暂不可用，请稍后重试")
    }
    val recommendations = response.data.mapNotNull { it.toMediaMetadata() }
    if (recommendations.isEmpty()) return Resource.Error("暂时没有可播放的心动推荐，请稍后重试")
    return Resource.Success((listOfNotNull(seed) + recommendations).distinctBy { it.id })
}
