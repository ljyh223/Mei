package com.ljyh.mei.data.model.api

import com.google.gson.annotations.SerializedName

data class GetAlbumList(
    @SerializedName("limit")
    val limit: String = "25",
    @SerializedName("offset")
    val offset: String = "0",
    @SerializedName("total")
    val total: String = "true"

)