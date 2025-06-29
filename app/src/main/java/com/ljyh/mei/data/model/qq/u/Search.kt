package com.ljyh.mei.data.model.qq.u
import com.google.gson.annotations.SerializedName


data class Search(
    @SerializedName("code")
    val code: Int,
    @SerializedName("req")
    val req: Req,
    @SerializedName("start_ts")
    val startTs: Long,
    @SerializedName("traceid")
    val traceid: String,
    @SerializedName("ts")
    val ts: Long
) {
    data class Req(
        @SerializedName("code")
        val code: Int,
        @SerializedName("data")
        val `data`: Data
    ) {
        data class Data(
            @SerializedName("body")
            val body: Body,
            @SerializedName("code")
            val code: Int,
            @SerializedName("feedbackURL")
            val feedbackURL: String,
            @SerializedName("meta")
            val meta: Meta,
            @SerializedName("ver")
            val ver: Int
        ) {
            data class Body(
                @SerializedName("album")
                val album: Album,
                @SerializedName("gedantip")
                val gedantip: Gedantip,
                @SerializedName("mv")
                val mv: Mv,
                @SerializedName("qc")
                val qc: List<Any>,
                @SerializedName("singer")
                val singer: Singer,
                @SerializedName("song")
                val song: Song,
                @SerializedName("songlist")
                val songlist: Songlist,
                @SerializedName("user")
                val user: User,
                @SerializedName("zhida")
                val zhida: Zhida
            ) {
                data class Album(
                    @SerializedName("list")
                    val list: List<Any>
                )

                data class Gedantip(
                    @SerializedName("tab")
                    val tab: Int,
                    @SerializedName("tip")
                    val tip: String
                )

                data class Mv(
                    @SerializedName("list")
                    val list: List<Any>
                )

                data class Singer(
                    @SerializedName("list")
                    val list: List<Any>
                )

                data class Song(
                    @SerializedName("list")
                    val list: List<S>
                ) {
                    data class S(
                        @SerializedName("act")
                        val act: Int,
                        @SerializedName("action")
                        val action: Action,
                        @SerializedName("album")
                        val album: Album,
                        @SerializedName("bpm")
                        val bpm: Int,
                        @SerializedName("content")
                        val content: String,
                        @SerializedName("desc")
                        val desc: String,
                        @SerializedName("desc_hilight")
                        val descHilight: String,
                        @SerializedName("docid")
                        val docid: String,
                        @SerializedName("eq")
                        val eq: Int,
                        @SerializedName("es")
                        val es: String,
                        @SerializedName("file")
                        val `file`: File,
                        @SerializedName("fnote")
                        val fnote: Int,
                        @SerializedName("genre")
                        val genre: Int,
                        @SerializedName("grp")
                        val grp: List<Any>,
                        @SerializedName("hotness")
                        val hotness: Hotness,
                        @SerializedName("href3")
                        val href3: String,
                        @SerializedName("id")
                        val id: Int,
                        @SerializedName("index_album")
                        val indexAlbum: Int,
                        @SerializedName("index_cd")
                        val indexCd: Int,
                        @SerializedName("interval")
                        val interval: Int,
                        @SerializedName("isonly")
                        val isonly: Int,
                        @SerializedName("ksong")
                        val ksong: Ksong,
                        @SerializedName("label")
                        val label: String,
                        @SerializedName("language")
                        val language: Int,
                        @SerializedName("lyric")
                        val lyric: String,
                        @SerializedName("lyric_hilight")
                        val lyricHilight: String,
                        @SerializedName("mid")
                        val mid: String,
                        @SerializedName("mv")
                        val mv: Mv,
                        @SerializedName("name")
                        val name: String,
                        @SerializedName("newStatus")
                        val newStatus: Int,
                        @SerializedName("ov")
                        val ov: Int,
                        @SerializedName("pay")
                        val pay: Pay,
                        @SerializedName("protect")
                        val protect: Int,
                        @SerializedName("sa")
                        val sa: Int,
                        @SerializedName("singer")
                        val singer: List<Singer>,
                        @SerializedName("status")
                        val status: Int,
                        @SerializedName("subtitle")
                        val subtitle: String,
                        @SerializedName("tag")
                        val tag: Int,
                        @SerializedName("tid")
                        val tid: Int,
                        @SerializedName("time_public")
                        val timePublic: String,
                        @SerializedName("title")
                        val title: String,
                        @SerializedName("title_hilight")
                        val titleHilight: String,
                        @SerializedName("type")
                        val type: Int,
                        @SerializedName("url")
                        val url: String,
                        @SerializedName("version")
                        val version: Int,
                        @SerializedName("vf")
                        val vf: List<Double>,
                        @SerializedName("vi")
                        val vi: List<Int>,
                        @SerializedName("volume")
                        val volume: Volume,
                        @SerializedName("vs")
                        val vs: List<String>
                    ) {
                        data class Action(
                            @SerializedName("alert")
                            val alert: Int,
                            @SerializedName("icon2")
                            val icon2: Long,
                            @SerializedName("icons")
                            val icons: Int,
                            @SerializedName("msgdown")
                            val msgdown: Int,
                            @SerializedName("msgfav")
                            val msgfav: Int,
                            @SerializedName("msgid")
                            val msgid: Int,
                            @SerializedName("msgpay")
                            val msgpay: Int,
                            @SerializedName("msgshare")
                            val msgshare: Int,
                            @SerializedName("switch")
                            val switch: Int,
                            @SerializedName("switch2")
                            val switch2: Int
                        )

                        data class Album(
                            @SerializedName("id")
                            val id: Int,
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
                            val b30s: Int,
                            @SerializedName("e_30s")
                            val e30s: Int,
                            @SerializedName("hires_bitdepth")
                            val hiresBitdepth: Int,
                            @SerializedName("hires_sample")
                            val hiresSample: Int,
                            @SerializedName("media_mid")
                            val mediaMid: String,
                            @SerializedName("size_128mp3")
                            val size128mp3: Int,
                            @SerializedName("size_192aac")
                            val size192aac: Int,
                            @SerializedName("size_192ogg")
                            val size192ogg: Int,
                            @SerializedName("size_24aac")
                            val size24aac: Int,
                            @SerializedName("size_320mp3")
                            val size320mp3: Int,
                            @SerializedName("size_360ra")
                            val size360ra: List<Any>,
                            @SerializedName("size_48aac")
                            val size48aac: Int,
                            @SerializedName("size_96aac")
                            val size96aac: Int,
                            @SerializedName("size_96ogg")
                            val size96ogg: Int,
                            @SerializedName("size_ape")
                            val sizeApe: Int,
                            @SerializedName("size_dolby")
                            val sizeDolby: Int,
                            @SerializedName("size_dts")
                            val sizeDts: Int,
                            @SerializedName("size_flac")
                            val sizeFlac: Int,
                            @SerializedName("size_hires")
                            val sizeHires: Int,
                            @SerializedName("size_new")
                            val sizeNew: List<Int>,
                            @SerializedName("size_try")
                            val sizeTry: Int,
                            @SerializedName("try_begin")
                            val tryBegin: Int,
                            @SerializedName("try_end")
                            val tryEnd: Int,
                            @SerializedName("url")
                            val url: String
                        )

                        data class Hotness(
                            @SerializedName("desc")
                            val desc: String,
                            @SerializedName("icon_url")
                            val iconUrl: String,
                            @SerializedName("jump_type")
                            val jumpType: Int,
                            @SerializedName("jump_url")
                            val jumpUrl: String
                        )

                        data class Ksong(
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("mid")
                            val mid: String
                        )

                        data class Mv(
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("title")
                            val title: String,
                            @SerializedName("vid")
                            val vid: String,
                            @SerializedName("vt")
                            val vt: Int
                        )

                        data class Pay(
                            @SerializedName("pay_down")
                            val payDown: Int,
                            @SerializedName("pay_month")
                            val payMonth: Int,
                            @SerializedName("pay_play")
                            val payPlay: Int,
                            @SerializedName("pay_status")
                            val payStatus: Int,
                            @SerializedName("price_album")
                            val priceAlbum: Int,
                            @SerializedName("price_track")
                            val priceTrack: Int,
                            @SerializedName("time_free")
                            val timeFree: Int
                        )

                        data class Singer(
                            @SerializedName("id")
                            val id: Int,
                            @SerializedName("mid")
                            val mid: String,
                            @SerializedName("name")
                            val name: String,
                            @SerializedName("pmid")
                            val pmid: String,
                            @SerializedName("title")
                            val title: String,
                            @SerializedName("type")
                            val type: Int,
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
                }

                data class Songlist(
                    @SerializedName("list")
                    val list: List<Any>
                )

                data class User(
                    @SerializedName("list")
                    val list: List<Any>
                )

                data class Zhida(
                    @SerializedName("list")
                    val list: List<Any>
                )
            }

            data class Meta(
                @SerializedName("cid")
                val cid: String,
                @SerializedName("curpage")
                val curpage: Int,
                @SerializedName("dir")
                val dir: String,
                @SerializedName("display_order")
                val displayOrder: List<Any>,
                @SerializedName("ein")
                val ein: Int,
                @SerializedName("estimate_sum")
                val estimateSum: Int,
                @SerializedName("expid")
                val expid: String,
                @SerializedName("feedbackPlaceId")
                val feedbackPlaceId: String,
                @SerializedName("is_filter")
                val isFilter: Int,
                @SerializedName("next_page_start")
                val nextPageStart: NextPageStart,
                @SerializedName("nextpage")
                val nextpage: Int,
                @SerializedName("perpage")
                val perpage: Int,
                @SerializedName("query")
                val query: String,
                @SerializedName("report_info")
                val reportInfo: ReportInfo,
                @SerializedName("result_trustworthy")
                val resultTrustworthy: Int,
                @SerializedName("ret")
                val ret: Int,
                @SerializedName("safetyType")
                val safetyType: Int,
                @SerializedName("safetyUrl")
                val safetyUrl: String,
                @SerializedName("searchid")
                val searchid: String,
                @SerializedName("sid")
                val sid: String,
                @SerializedName("sin")
                val sin: Int,
                @SerializedName("step_rela_syntax_tree")
                val stepRelaSyntaxTree: StepRelaSyntaxTree,
                @SerializedName("sum")
                val sum: Int,
                @SerializedName("tab_list")
                val tabList: List<Any>,
                @SerializedName("uid")
                val uid: String,
                @SerializedName("v")
                val v: Int
            ) {
                class NextPageStart

                data class ReportInfo(
                    @SerializedName("items")
                    val items: Items
                ) {
                    class Items
                }

                class StepRelaSyntaxTree
            }
        }
    }
}