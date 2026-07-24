package com.ljyh.mei.data.model.qq.u
import com.google.gson.annotations.SerializedName


data class GetSearchData(
    @SerializedName("comm")
    val comm: Comm = Comm(),
    @SerializedName("request")
    val req: Req,
){
    data class Comm(
        @SerializedName("ct")
        val ct: Int = 11,
        @SerializedName("cv")
        val cv: String = "1003006",
        @SerializedName("v")
        val v: String = "1003006",
        @SerializedName("os_ver")
        val osVer: String = "15",
        @SerializedName("phonetype")
        val phonetype: String = "24122RKC7C",
        @SerializedName("tmeAppID")
        val tmeAppID: String = "qqmusiclight",
        @SerializedName("nettype")
        val nettype: String = "NETWORK_WIFI",
        @SerializedName("udid")
        val udid: String = "0",
    )


    data class Req(
        @SerializedName("method")
        val method: String = "DoSearchForQQMusicLite",
        @SerializedName("module")
        val module: String = "music.search.SearchCgiService",
        @SerializedName("param")
        val `param`: Param
    ) {
        data class Param(
            @SerializedName("query")
            val query: String,
            @SerializedName("search_type")
            val searchType: Int = 0,
            @SerializedName("page_num")
            val pageNum: Int = 1,
            @SerializedName("num_per_page")
            val numPerPage: Int = 20,
            @SerializedName("highlight")
            val highlight: Int = 0,
            @SerializedName("nqc_flag")
            val nqcFlag: Int = 0,
            @SerializedName("page_id")
            val pageId: Int = 1,
            @SerializedName("grp")
            val grp: Int = 1
        )
    }
}
