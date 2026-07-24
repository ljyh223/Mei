package com.ljyh.mei.data.model.qq.u
import com.google.gson.annotations.SerializedName

data class SearchResult(
    @SerializedName("code")
    val code: Long,
    @SerializedName("ts")
    val ts: Long,
    @SerializedName("start_ts")
    val startTs: Long,
    @SerializedName("traceid")
    val traceid: String,
    @SerializedName("request")
    val request: Request
) {
    data class Request(
        @SerializedName("code")
        val code: Long,
        @SerializedName("data")
        val `data`: Data
    ) {
        data class Data(
            @SerializedName("body")
            val body: Body,
            @SerializedName("code")
            val code: Long,
            @SerializedName("feedbackURL")
            val feedbackURL: String,
            @SerializedName("meta")
            val meta: Meta,
            @SerializedName("ver")
            val ver: Long
        ) {
            data class Body(
                @SerializedName("direct_group")
                val directGroup: DirectGroup,
                @SerializedName("gedantip")
                val gedantip: Gedantip,
                @SerializedName("head")
                val head: String,
                @SerializedName("item_album")
                val itemAlbum: List<Any?>,
                @SerializedName("item_audio")
                val itemAudio: List<Any?>,
                @SerializedName("item_mv")
                val itemMv: List<Any?>,
                @SerializedName("item_song")
                val itemSong: List<ItemSong>,
                @SerializedName("item_songlist")
                val itemSonglist: List<Any?>,
                @SerializedName("multi_extern_info")
                val multiExternInfo: MultiExternInfo,
                @SerializedName("qc")
                val qc: List<Any?>,
                @SerializedName("showMore")
                val showMore: Long,
                @SerializedName("showMoreText")
                val showMoreText: String,
                @SerializedName("showMoreUrl")
                val showMoreUrl: String,
                @SerializedName("singer")
                val singer: List<Any?>,
                @SerializedName("subtab_infos")
                val subtabInfos: List<Any?>
            ) {
                data class DirectGroup(
                    @SerializedName("extra_info")
                    val extraInfo: ExtraInfo,
                    @SerializedName("lateral_list")
                    val lateralList: List<Any?>,
                    @SerializedName("region")
                    val region: String,
                    @SerializedName("show_pattern")
                    val showPattern: Long,
                    @SerializedName("title")
                    val title: String,
                    @SerializedName("vertical_list")
                    val verticalList: List<Any?>
                ) {
                    data class ExtraInfo(
                        @SerializedName("content")
                        val content: String,
                        @SerializedName("search_ext")
                        val searchExt: String,
                        @SerializedName("tjreport")
                        val tjreport: String
                    )
                }

                data class Gedantip(
                    @SerializedName("tab")
                    val tab: Long,
                    @SerializedName("tip")
                    val tip: String
                )

                data class ItemSong(
                    @SerializedName("act")
                    val act: Long,
                    @SerializedName("action")
                    val action: Action,
                    @SerializedName("album")
                    val album: Album,
                    @SerializedName("author")
                    val author: String,
                    @SerializedName("bpm")
                    val bpm: Long,
                    @SerializedName("content")
                    val content: String,
                    @SerializedName("custom_data")
                    val customData: String,
                    @SerializedName("data_type")
                    val dataType: Long,
                    @SerializedName("desc")
                    val desc: String,
                    @SerializedName("docid")
                    val docid: String,
                    @SerializedName("eq")
                    val eq: Long,
                    @SerializedName("es")
                    val es: String,
                    @SerializedName("file")
                    val `file`: File,
                    @SerializedName("fnote")
                    val fnote: Long,
                    @SerializedName("genre")
                    val genre: Long,
                    @SerializedName("grp")
                    val grp: List<Grp>,
                    @SerializedName("hotness_desc")
                    val hotnessDesc: String,
                    @SerializedName("href3")
                    val href3: String,
                    @SerializedName("id")
                    val id: Long,
                    @SerializedName("index_album")
                    val indexAlbum: Long,
                    @SerializedName("index_cd")
                    val indexCd: Long,
                    @SerializedName("interval")
                    val interval: Long,
                    @SerializedName("isonly")
                    val isonly: Long,
                    @SerializedName("ksong")
                    val ksong: Ksong,
                    @SerializedName("label")
                    val label: String,
                    @SerializedName("language")
                    val language: Long,
                    @SerializedName("lyric")
                    val lyric: String,
                    @SerializedName("mid")
                    val mid: String,
                    @SerializedName("mv")
                    val mv: Mv,
                    @SerializedName("name")
                    val name: String,
                    @SerializedName("newStatus")
                    val newStatus: Long,
                    @SerializedName("ov")
                    val ov: Long,
                    @SerializedName("pay")
                    val pay: Pay,
                    @SerializedName("protect")
                    val protect: Long,
                    @SerializedName("sa")
                    val sa: Long,
                    @SerializedName("search_title")
                    val searchTitle: String,
                    @SerializedName("singer")
                    val singer: List<Singer>,
                    @SerializedName("status")
                    val status: Long,
                    @SerializedName("subtitle")
                    val subtitle: String,
                    @SerializedName("tag")
                    val tag: Long,
                    @SerializedName("tid")
                    val tid: Long,
                    @SerializedName("time_public")
                    val timePublic: String,
                    @SerializedName("title")
                    val title: String,
                    @SerializedName("title_extra")
                    val titleExtra: String,
                    @SerializedName("title_main")
                    val titleMain: String,
                    @SerializedName("type")
                    val type: Long,
                    @SerializedName("url")
                    val url: String,
                    @SerializedName("vec_hotness")
                    val vecHotness: List<Any?>,
                    @SerializedName("version")
                    val version: Long,
                    @SerializedName("volume")
                    val volume: Volume,
                    @SerializedName("vs")
                    val vs: List<String>
                ) {
                    data class Action(
                        @SerializedName("alert")
                        val alert: Long,
                        @SerializedName("icon2")
                        val icon2: Long,
                        @SerializedName("icons")
                        val icons: Long,
                        @SerializedName("msgdown")
                        val msgdown: Long,
                        @SerializedName("msgfav")
                        val msgfav: Long,
                        @SerializedName("msgid")
                        val msgid: Long,
                        @SerializedName("msgpay")
                        val msgpay: Long,
                        @SerializedName("msgshare")
                        val msgshare: Long,
                        @SerializedName("switch")
                        val switch: Long,
                        @SerializedName("switch2")
                        val switch2: Long
                    )

                    data class Album(
                        @SerializedName("id")
                        val id: Long,
                        @SerializedName("mid")
                        val mid: String,
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("pmid")
                        val pmid: String,
                        @SerializedName("subtitle")
                        val subtitle: String,
                        @SerializedName("time_public")
                        val timePublic: String,
                        @SerializedName("title")
                        val title: String
                    )

                    data class File(
                        @SerializedName("b_30s")
                        val b30s: Long,
                        @SerializedName("e_30s")
                        val e30s: Long,
                        @SerializedName("hires_bitdepth")
                        val hiresBitdepth: Long,
                        @SerializedName("hires_sample")
                        val hiresSample: Long,
                        @SerializedName("media_mid")
                        val mediaMid: String,
                        @SerializedName("size_128mp3")
                        val size128mp3: Long,
                        @SerializedName("size_192aac")
                        val size192aac: Long,
                        @SerializedName("size_192ogg")
                        val size192ogg: Long,
                        @SerializedName("size_24aac")
                        val size24aac: Long,
                        @SerializedName("size_320mp3")
                        val size320mp3: Long,
                        @SerializedName("size_360ra")
                        val size360ra: List<Any?>,
                        @SerializedName("size_48aac")
                        val size48aac: Long,
                        @SerializedName("size_96aac")
                        val size96aac: Long,
                        @SerializedName("size_96ogg")
                        val size96ogg: Long,
                        @SerializedName("size_ape")
                        val sizeApe: Long,
                        @SerializedName("size_dolby")
                        val sizeDolby: Long,
                        @SerializedName("size_dts")
                        val sizeDts: Long,
                        @SerializedName("size_flac")
                        val sizeFlac: Long,
                        @SerializedName("size_hires")
                        val sizeHires: Long,
                        @SerializedName("size_new")
                        val sizeNew: List<Long>,
                        @SerializedName("size_try")
                        val sizeTry: Long,
                        @SerializedName("try_begin")
                        val tryBegin: Long,
                        @SerializedName("try_end")
                        val tryEnd: Long,
                        @SerializedName("url")
                        val url: String
                    )

                    data class Grp(
                        @SerializedName("act")
                        val act: Long,
                        @SerializedName("action")
                        val action: Action,
                        @SerializedName("album")
                        val album: Album,
                        @SerializedName("author")
                        val author: String,
                        @SerializedName("bpm")
                        val bpm: Long,
                        @SerializedName("content")
                        val content: String,
                        @SerializedName("custom_data")
                        val customData: String,
                        @SerializedName("data_type")
                        val dataType: Long,
                        @SerializedName("desc")
                        val desc: String,
                        @SerializedName("docid")
                        val docid: String,
                        @SerializedName("eq")
                        val eq: Long,
                        @SerializedName("es")
                        val es: String,
                        @SerializedName("file")
                        val `file`: File,
                        @SerializedName("fnote")
                        val fnote: Long,
                        @SerializedName("genre")
                        val genre: Long,
                        @SerializedName("grp")
                        val grp: List<Any?>,
                        @SerializedName("hotness_desc")
                        val hotnessDesc: String,
                        @SerializedName("href3")
                        val href3: String,
                        @SerializedName("id")
                        val id: Long,
                        @SerializedName("index_album")
                        val indexAlbum: Long,
                        @SerializedName("index_cd")
                        val indexCd: Long,
                        @SerializedName("interval")
                        val interval: Long,
                        @SerializedName("isonly")
                        val isonly: Long,
                        @SerializedName("ksong")
                        val ksong: Ksong,
                        @SerializedName("label")
                        val label: String,
                        @SerializedName("language")
                        val language: Long,
                        @SerializedName("lyric")
                        val lyric: String,
                        @SerializedName("mid")
                        val mid: String,
                        @SerializedName("mv")
                        val mv: Mv,
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("newStatus")
                        val newStatus: Long,
                        @SerializedName("ov")
                        val ov: Long,
                        @SerializedName("pay")
                        val pay: Pay,
                        @SerializedName("protect")
                        val protect: Long,
                        @SerializedName("sa")
                        val sa: Long,
                        @SerializedName("search_title")
                        val searchTitle: String,
                        @SerializedName("singer")
                        val singer: List<Singer>,
                        @SerializedName("status")
                        val status: Long,
                        @SerializedName("subtitle")
                        val subtitle: String,
                        @SerializedName("tag")
                        val tag: Long,
                        @SerializedName("tid")
                        val tid: Long,
                        @SerializedName("time_public")
                        val timePublic: String,
                        @SerializedName("title")
                        val title: String,
                        @SerializedName("title_extra")
                        val titleExtra: String,
                        @SerializedName("title_main")
                        val titleMain: String,
                        @SerializedName("type")
                        val type: Long,
                        @SerializedName("url")
                        val url: String,
                        @SerializedName("vec_hotness")
                        val vecHotness: List<Any?>,
                        @SerializedName("version")
                        val version: Long,
                        @SerializedName("volume")
                        val volume: Volume,
                        @SerializedName("vs")
                        val vs: List<String>
                    ) {
                        data class Action(
                            @SerializedName("alert")
                            val alert: Long,
                            @SerializedName("icon2")
                            val icon2: Long,
                            @SerializedName("icons")
                            val icons: Long,
                            @SerializedName("msgdown")
                            val msgdown: Long,
                            @SerializedName("msgfav")
                            val msgfav: Long,
                            @SerializedName("msgid")
                            val msgid: Long,
                            @SerializedName("msgpay")
                            val msgpay: Long,
                            @SerializedName("msgshare")
                            val msgshare: Long,
                            @SerializedName("switch")
                            val switch: Long,
                            @SerializedName("switch2")
                            val switch2: Long
                        )

                        data class Album(
                            @SerializedName("id")
                            val id: Long,
                            @SerializedName("mid")
                            val mid: String,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("pmid")
                            val pmid: String,
                            @SerializedName("subtitle")
                            val subtitle: String,
                            @SerializedName("time_public")
                            val timePublic: String,
                            @SerializedName("title")
                            val title: String
                        )

                        data class File(
                            @SerializedName("b_30s")
                            val b30s: Long,
                            @SerializedName("e_30s")
                            val e30s: Long,
                            @SerializedName("hires_bitdepth")
                            val hiresBitdepth: Long,
                            @SerializedName("hires_sample")
                            val hiresSample: Long,
                            @SerializedName("media_mid")
                            val mediaMid: String,
                            @SerializedName("size_128mp3")
                            val size128mp3: Long,
                            @SerializedName("size_192aac")
                            val size192aac: Long,
                            @SerializedName("size_192ogg")
                            val size192ogg: Long,
                            @SerializedName("size_24aac")
                            val size24aac: Long,
                            @SerializedName("size_320mp3")
                            val size320mp3: Long,
                            @SerializedName("size_360ra")
                            val size360ra: List<Any?>,
                            @SerializedName("size_48aac")
                            val size48aac: Long,
                            @SerializedName("size_96aac")
                            val size96aac: Long,
                            @SerializedName("size_96ogg")
                            val size96ogg: Long,
                            @SerializedName("size_ape")
                            val sizeApe: Long,
                            @SerializedName("size_dolby")
                            val sizeDolby: Long,
                            @SerializedName("size_dts")
                            val sizeDts: Long,
                            @SerializedName("size_flac")
                            val sizeFlac: Long,
                            @SerializedName("size_hires")
                            val sizeHires: Long,
                            @SerializedName("size_new")
                            val sizeNew: List<Long>,
                            @SerializedName("size_try")
                            val sizeTry: Long,
                            @SerializedName("try_begin")
                            val tryBegin: Long,
                            @SerializedName("try_end")
                            val tryEnd: Long,
                            @SerializedName("url")
                            val url: String
                        )

                        data class Ksong(
                            @SerializedName("id")
                            val id: Long,
                            @SerializedName("mid")
                            val mid: String
                        )

                        data class Mv(
                            @SerializedName("id")
                            val id: Long,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("title")
                            val title: String,
                            @SerializedName("vid")
                            val vid: String,
                            @SerializedName("vt")
                            val vt: Long
                        )

                        data class Pay(
                            @SerializedName("pay_down")
                            val payDown: Long,
                            @SerializedName("pay_month")
                            val payMonth: Long,
                            @SerializedName("pay_play")
                            val payPlay: Long,
                            @SerializedName("pay_status")
                            val payStatus: Long,
                            @SerializedName("price_album")
                            val priceAlbum: Long,
                            @SerializedName("price_track")
                            val priceTrack: Long,
                            @SerializedName("time_free")
                            val timeFree: Long
                        )

                        data class Singer(
                            @SerializedName("id")
                            val id: Long,
                            @SerializedName("mid")
                            val mid: String,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("pmid")
                            val pmid: String,
                            @SerializedName("title")
                            val title: String,
                            @SerializedName("type")
                            val type: Long,
                            @SerializedName("uin")
                            val uin: Long
                        )

                        data class Volume(
                            @SerializedName("gain")
                            val gain: Double,
                            @SerializedName("lra")
                            val lra: Double,
                            @SerializedName("peak")
                            val peak: Double
                        )
                    }

                    data class Ksong(
                        @SerializedName("id")
                        val id: Long,
                        @SerializedName("mid")
                        val mid: String
                    )

                    data class Mv(
                        @SerializedName("id")
                        val id: Long,
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("title")
                        val title: String,
                        @SerializedName("vid")
                        val vid: String,
                        @SerializedName("vt")
                        val vt: Long
                    )

                    data class Pay(
                        @SerializedName("pay_down")
                        val payDown: Long,
                        @SerializedName("pay_month")
                        val payMonth: Long,
                        @SerializedName("pay_play")
                        val payPlay: Long,
                        @SerializedName("pay_status")
                        val payStatus: Long,
                        @SerializedName("price_album")
                        val priceAlbum: Long,
                        @SerializedName("price_track")
                        val priceTrack: Long,
                        @SerializedName("time_free")
                        val timeFree: Long
                    )

                    data class Singer(
                        @SerializedName("id")
                        val id: Long,
                        @SerializedName("mid")
                        val mid: String,
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("pmid")
                        val pmid: String,
                        @SerializedName("title")
                        val title: String,
                        @SerializedName("type")
                        val type: Long,
                        @SerializedName("uin")
                        val uin: Long
                    )

                    data class Volume(
                        @SerializedName("gain")
                        val gain: Double,
                        @SerializedName("lra")
                        val lra: Double,
                        @SerializedName("peak")
                        val peak: Double
                    )
                }

                data class MultiExternInfo(
                    @SerializedName("is_show")
                    val isShow: Long,
                    @SerializedName("restype")
                    val restype: String,
                    @SerializedName("selectors")
                    val selectors: List<Any?>,
                    @SerializedName("show_num")
                    val showNum: Long,
                    @SerializedName("show_rows")
                    val showRows: Long,
                    @SerializedName("style")
                    val style: Long
                )
            }

            data class Meta(
                @SerializedName("cid")
                val cid: String,
                @SerializedName("curpage")
                val curpage: Long,
                @SerializedName("dir")
                val dir: String,
                @SerializedName("display_order")
                val displayOrder: List<Any?>,
                @SerializedName("ein")
                val ein: Long,
                @SerializedName("estimate_sum")
                val estimateSum: Long,
                @SerializedName("expid")
                val expid: String,
                @SerializedName("feedbackPlaceId")
                val feedbackPlaceId: String,
                @SerializedName("is_filter")
                val isFilter: Long,
                @SerializedName("next_page_start")
                val nextPageStart: Map<String, Any>,
                @SerializedName("nextpage")
                val nextpage: Long,
                @SerializedName("perpage")
                val perpage: Long,
                @SerializedName("query")
                val query: String,
                @SerializedName("report_info")
                val reportInfo: ReportInfo,
                @SerializedName("result_trustworthy")
                val resultTrustworthy: Long,
                @SerializedName("ret")
                val ret: Long,
                @SerializedName("safetyType")
                val safetyType: Long,
                @SerializedName("safetyUrl")
                val safetyUrl: String,
                @SerializedName("searchid")
                val searchid: String,
                @SerializedName("sid")
                val sid: String,
                @SerializedName("sin")
                val sin: Long,
                @SerializedName("step_rela_syntax_tree")
                val stepRelaSyntaxTree: Map<String, Any>,
                @SerializedName("sum")
                val sum: Long,
                @SerializedName("tab_list")
                val tabList: List<Any?>,
                @SerializedName("uid")
                val uid: String,
                @SerializedName("v")
                val v: Long
            ) {
                data class ReportInfo(
                    @SerializedName("items")
                    val items: Map<String, Any>
                )
            }
        }
    }
}
