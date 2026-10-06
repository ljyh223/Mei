package com.ljyh.mei.data.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable

data class BaseResponse(
    @SerialName("code")
    val code: Int,
)

@Serializable

data class BaseMessageResponse(

    val code: Int,
    @SerialName("msg")
    val msg: JsonElement,
    @SerialName("message")
    val message: JsonElement,
    @SerialName("data")
    val `data`: JsonElement
)

