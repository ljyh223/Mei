package com.ljyh.mei.data.network.api

import kotlinx.serialization.Serializable

@Serializable
data class DynamicCoverResponse(
    val code: Int,
    val data: DynamicCoverData?
)

@Serializable
data class DynamicCoverData(
    val videoPlayUrl: String?
)
