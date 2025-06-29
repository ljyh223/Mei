package com.ljyh.mei.data.model.qq.u
import com.google.gson.annotations.SerializedName


data class GetSearchData(
    @SerializedName("comm")
    val comm: Comm1 = Comm1(),
    @SerializedName("req_0")
    val req: Req,
){
    data class Comm1(
        @SerializedName("g_tk")
        val gTk: Int = 0,
        @SerializedName("uin")
        val uin: Long = 0,
        @SerializedName("format")
        val format: String = "",
        @SerializedName("inCharset")
        val inCharset: String = "",
        @SerializedName("outCharset")
        val outCharset: String = "",
        @SerializedName("notice")
        val notice: Int = 0,
        @SerializedName("platform")
        val platform: String = "",
        @SerializedName("needNewCode")
        val needNewCode: Int = 0,
        @SerializedName("ct")
        val ct: Int = 0,
        @SerializedName("cv")
        val cv: Int = 0
    )


    data class Req(
        @SerializedName("method")
        val method: String = "DoSearchForQQMusicDesktop",
        @SerializedName("module")
        val module: String = "music.search.SearchCgiService",
        @SerializedName("param")
        val `param`: Param
    ) {
        data class Param(
            @SerializedName("num_per_page")
            val numPerPage: Int = 20,
            @SerializedName("page_num")
            val pageNum: Int = 1,
            @SerializedName("query")
            val query: String,
            @SerializedName("search_type")
            val searchType: Int = 0
        )
    }
}