package com.ljyh.mei.data.network.api

data class DynamicCoverResponse(
    val code: Int,
    val data: DynamicCoverData?
)

data class DynamicCoverData(
    val videoPlayUrl: String?
)
