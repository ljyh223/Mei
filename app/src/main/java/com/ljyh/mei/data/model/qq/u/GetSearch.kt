package com.ljyh.mei.data.model.qq.u
import com.google.gson.annotations.SerializedName


data class GetSearchData(
    @SerializedName("comm")
    val comm: Comm1 = Comm1(),
    @SerializedName("req_0")
    val req: Req,
){
    data class Comm1(
        @SerializedName("ct")
        val ct: Int = 11,
        @SerializedName("cv")
        val cv: String = "1003006",
        @SerializedName("uin")
        val uin: Long = 0,
        @SerializedName("tmeAppID")
        val tmeAppID: String = "qqmusiclight"
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
            val searchType: Int = 0,
            @SerializedName("grp")
            val grp: Int = 1
        )
    }
}