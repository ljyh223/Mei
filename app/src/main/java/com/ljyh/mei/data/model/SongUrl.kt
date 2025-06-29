package com.ljyh.mei.data.model
import com.google.gson.annotations.SerializedName


data class SongUrl(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: List<Data>
) {
    data class Data(
        @SerializedName("br")
        val br: Int,
        @SerializedName("canExtend")
        val canExtend: Boolean,
        @SerializedName("channelLayout")
        val channelLayout: Any,
        @SerializedName("closedGain")
        val closedGain: Any,
        @SerializedName("closedPeak")
        val closedPeak: Any,
        @SerializedName("code")
        val code: Int,
        @SerializedName("effectTypes")
        val effectTypes: Any,
        @SerializedName("encodeType")
        val encodeType: String,
        @SerializedName("expi")
        val expi: Int,
        @SerializedName("fee")
        val fee: Int,
        @SerializedName("flag")
        val flag: Int,
        @SerializedName("freeTimeTrialPrivilege")
        val freeTimeTrialPrivilege: FreeTimeTrialPrivilege,
        @SerializedName("freeTrialInfo")
        val freeTrialInfo: Any,
        @SerializedName("freeTrialPrivilege")
        val freeTrialPrivilege: FreeTrialPrivilege,
        @SerializedName("gain")
        val gain: Any,
        @SerializedName("id")
        val id: Long,
        @SerializedName("level")
        val level: String,
        @SerializedName("levelConfuse")
        val levelConfuse: Any,
        @SerializedName("md5")
        val md5: String,
        @SerializedName("message")
        val message: Any,
        @SerializedName("musicId")
        val musicId: String,
        @SerializedName("payed")
        val payed: Int,
        @SerializedName("peak")
        val peak: Double,
        @SerializedName("podcastCtrp")
        val podcastCtrp: Any,
        @SerializedName("rightSource")
        val rightSource: Int,
        @SerializedName("size")
        val size: Int,
        @SerializedName("time")
        val time: Int,
        @SerializedName("type")
        val type: String,
        @SerializedName("uf")
        val uf: Any,
        @SerializedName("url")
        val url: String?,
        @SerializedName("urlSource")
        val urlSource: Int
    ) {
        data class FreeTimeTrialPrivilege(
            @SerializedName("remainTime")
            val remainTime: Int,
            @SerializedName("resConsumable")
            val resConsumable: Boolean,
            @SerializedName("type")
            val type: Int,
            @SerializedName("userConsumable")
            val userConsumable: Boolean
        )

        data class FreeTrialPrivilege(
            @SerializedName("cannotListenReason")
            val cannotListenReason: Any,
            @SerializedName("freeLimitTagType")
            val freeLimitTagType: Any,
            @SerializedName("listenType")
            val listenType: Any,
            @SerializedName("playReason")
            val playReason: Any,
            @SerializedName("resConsumable")
            val resConsumable: Boolean,
            @SerializedName("userConsumable")
            val userConsumable: Boolean
        )
    }
}