package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable

data class GetAlbumList(
    @SerialName("limit")
    val limit: String = "25",
    @SerialName("offset")
    val offset: String = "0",
    @SerialName("total")
    val total: String = "true"

)