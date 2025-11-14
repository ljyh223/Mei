package com.ljyh.mei.data.model.api

import com.google.gson.annotations.SerializedName

data class BaseResponse(
    @SerializedName("code")
    val code: Int,
)

data class BaseMessageResponse(

    val code: Int,
    @SerializedName("msg")
    val msg: Any,
    @SerializedName("message")
    val message: Any,
    @SerializedName("data")
    val `data`: Any
)

