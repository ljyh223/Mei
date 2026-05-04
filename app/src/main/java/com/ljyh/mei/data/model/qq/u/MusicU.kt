package com.ljyh.mei.data.model.qq.u
import com.google.gson.annotations.SerializedName


data class MusicU(
    @SerializedName("comm")
    val comm: Comm,
    @SerializedName("req")
    val req: Req
) {
    data class Comm(
        @SerializedName("ct")
        val ct: Int = 11,
        @SerializedName("cv")
        val cv: String = "1003006",
        @SerializedName("uin")
        val uin: String = "",
        @SerializedName("tmeAppID")
        val tmeAppID: String = "qqmusiclight"
    )

    data class Req(
        @SerializedName("method")
        val method: String,
        @SerializedName("module")
        val module: String,
        @SerializedName("param")
        val `param`: Param
    ) {
        data class Param(
            @SerializedName("grp")
            val grp: Int,
            @SerializedName("num_per_page")
            val numPerPage: Int,
            @SerializedName("page_num")
            val pageNum: Int,
            @SerializedName("query")
            val query: String,
            @SerializedName("search_type")
            val searchType: Int
        )
    }
}