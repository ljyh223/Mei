package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetSearch(
    @SerialName("s") val s: String,
    @SerialName("type") val type: Int = 1,
    @SerialName("limit") val limit: Int = 30,
    @SerialName("offset") val offset: Int = 0,
)
