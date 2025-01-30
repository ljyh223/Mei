package com.ljyh.music.data.model
import com.google.gson.annotations.SerializedName


data class HomePageResourceShow(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("trp")
    val trp: Trp
) {
    data class Data(
        @SerializedName("blockCodeOrderList")
        val blockCodeOrderList: String,
        @SerializedName("blocks")
        val blocks: List<Block>,
        @SerializedName("callbackParameters")
        val callbackParameters: String,
        @SerializedName("cursor")
        val cursor: Int,
        @SerializedName("demote")
        val demote: Boolean,
        @SerializedName("extMap")
        val extMap: ExtMap,
        @SerializedName("hasDoubleFlow")
        val hasDoubleFlow: Boolean,
        @SerializedName("hasMore")
        val hasMore: Boolean,
        @SerializedName("requestBlockOrder")
        val requestBlockOrder: List<String>
    ) {
        data class Block(
            @SerializedName("bizCode")
            val bizCode: String,
            @SerializedName("channelCode")
            val channelCode: String,
            @SerializedName("constructLogId")
            val constructLogId: String,
            @SerializedName("crossPlatformConfig")
            val crossPlatformConfig: CrossPlatformConfig,
            @SerializedName("dslData")
            val dslData: DslData,
            @SerializedName("extMap")
            val extMap: ExtMap,
            @SerializedName("likePosition")
            val likePosition: Int,
            @SerializedName("log")
            val log: Log,
            @SerializedName("logMap")
            val logMap: LogMap,
            @SerializedName("needClientCover")
            val needClientCover: Boolean,
            @SerializedName("positionCode")
            val positionCode: String,
            @SerializedName("showTitle")
            val showTitle: Boolean
        ) {
            data class CrossPlatformConfig(
                @SerializedName("alertConfig")
                val alertConfig: AlertConfig,
                @SerializedName("containerType")
                val containerType: String,
                @SerializedName("dslContent")
                val dslContent: DslContent
            ) {
                data class AlertConfig(
                    @SerializedName("hPadding")
                    val hPadding: Int,
                    @SerializedName("hpadding")
                    val hpadding: Int,
                    @SerializedName("needCloseBtn")
                    val needCloseBtn: Boolean,
                    @SerializedName("widthExpand")
                    val widthExpand: Boolean
                )

                data class DslContent(
                    @SerializedName("lunaforcmLoadType")
                    val lunaforcmLoadType: String,
                    @SerializedName("lunaforcmSence")
                    val lunaforcmSence: String,
                    @SerializedName("lunaforcmTempletContent")
                    val lunaforcmTempletContent: LunaforcmTempletContent,
                    @SerializedName("lunaforcmTempletId")
                    val lunaforcmTempletId: String
                ) {
                    data class LunaforcmTempletContent(
                        @SerializedName("blockId")
                        val blockId: Int,
                        @SerializedName("blockName")
                        val blockName: String,
                        @SerializedName("blockType")
                        val blockType: String,
                        @SerializedName("dslMap")
                        val dslMap: DslMap,
                        @SerializedName("dslRootId")
                        val dslRootId: String,
                        @SerializedName("needScaleForPad")
                        val needScaleForPad: Boolean,
                        @SerializedName("publishTime")
                        val publishTime: String,
                        @SerializedName("sceneName")
                        val sceneName: String,
                        @SerializedName("templateConfig")
                        val templateConfig: String
                    ) {
                        data class DslMap(
                            @SerializedName("artiste_parachute_greeting_hpak321yot")
                            val artisteParachuteGreetingHpak321yot: ArtisteParachuteGreetingHpak321yot,
                            @SerializedName("daily_mix_single_line_text_copy_ly2bff6we2")
                            val dailyMixSingleLineTextCopyLy2bff6we2: DailyMixSingleLineTextCopyLy2bff6we2,
                            @SerializedName("daily_mix_single_line_text_copy_qqy0we33tm")
                            val dailyMixSingleLineTextCopyQqy0we33tm: DailyMixSingleLineTextCopyQqy0we33tm,
                            @SerializedName("daily_rcmd_annual_and_monthly_playlist_item_47yuuxhs5s")
                            val dailyRcmdAnnualAndMonthlyPlaylistItem47yuuxhs5s: DailyRcmdAnnualAndMonthlyPlaylistItem47yuuxhs5s,
                            @SerializedName("empty_icon_d8vh7d7n9p")
                            val emptyIconD8vh7d7n9p: EmptyIconD8vh7d7n9p,
                            @SerializedName("empty_icon_eqs0sun5u3")
                            val emptyIconEqs0sun5u3: EmptyIconEqs0sun5u3,
                            @SerializedName("empty_icon_hvetjbwns3")
                            val emptyIconHvetjbwns3: EmptyIconHvetjbwns3,
                            @SerializedName("empty_icon_i8h4kd56rz")
                            val emptyIconI8h4kd56rz: EmptyIconI8h4kd56rz,
                            @SerializedName("empty_icon_lflntqarff")
                            val emptyIconLflntqarff: EmptyIconLflntqarff,
                            @SerializedName("empty_icon_thrc7s8px1")
                            val emptyIconThrc7s8px1: EmptyIconThrc7s8px1,
                            @SerializedName("ground_glass_back_ground_uh06xewrtz")
                            val groundGlassBackGroundUh06xewrtz: GroundGlassBackGroundUh06xewrtz,
                            @SerializedName("ground_glass_back_ground_uh06xewrtz_14")
                            val groundGlassBackGroundUh06xewrtz14: GroundGlassBackGroundUh06xewrtz14,
                            @SerializedName("ground_glass_back_ground_uh06xewrtz_18")
                            val groundGlassBackGroundUh06xewrtz18: GroundGlassBackGroundUh06xewrtz18,
                            @SerializedName("ground_glass_back_ground_uh06xewrtz_20")
                            val groundGlassBackGroundUh06xewrtz20: GroundGlassBackGroundUh06xewrtz20,
                            @SerializedName("ground_glass_back_ground_uh06xewrtz_23")
                            val groundGlassBackGroundUh06xewrtz23: GroundGlassBackGroundUh06xewrtz23,
                            @SerializedName("ground_glass_back_ground_uh06xewrtz_30")
                            val groundGlassBackGroundUh06xewrtz30: GroundGlassBackGroundUh06xewrtz30,
                            @SerializedName("home_bottom_margin_65cju8vvls")
                            val homeBottomMargin65cju8vvls: HomeBottomMargin65cju8vvls,
                            @SerializedName("home_bottom_margin_ao7hq65rhh")
                            val homeBottomMarginAo7hq65rhh: HomeBottomMarginAo7hq65rhh,
                            @SerializedName("home_bottom_margin_hfcxc755tw")
                            val homeBottomMarginHfcxc755tw: HomeBottomMarginHfcxc755tw,
                            @SerializedName("home_common_rcmd_song_item_u59ognvt3u")
                            val homeCommonRcmdSongItemU59ognvt3u: HomeCommonRcmdSongItemU59ognvt3u,
                            @SerializedName("home_common_rcmd_song_list_lwxbsnqyiy")
                            val homeCommonRcmdSongListLwxbsnqyiy: HomeCommonRcmdSongListLwxbsnqyiy,
                            @SerializedName("home_common_rcmd_songs_module_8pim9hfovz")
                            val homeCommonRcmdSongsModule8pim9hfovz: HomeCommonRcmdSongsModule8pim9hfovz,
                            @SerializedName("home_common_title_nc13i9w3i3")
                            val homeCommonTitleNc13i9w3i3: HomeCommonTitleNc13i9w3i3,
                            @SerializedName("home_common_title_pczt00v2qu")
                            val homeCommonTitlePczt00v2qu: HomeCommonTitlePczt00v2qu,
                            @SerializedName("home_common_title_with_nick_name_sxp3xmlqd7")
                            val homeCommonTitleWithNickNameSxp3xmlqd7: HomeCommonTitleWithNickNameSxp3xmlqd7,
                            @SerializedName("home_ground_glass_back_ground_926ehi7wma")
                            val homeGroundGlassBackGround926ehi7wma: HomeGroundGlassBackGround926ehi7wma,
                            @SerializedName("home_ground_glass_back_ground_d8w9yhqyfq")
                            val homeGroundGlassBackGroundD8w9yhqyfq: HomeGroundGlassBackGroundD8w9yhqyfq,
                            @SerializedName("home_ground_glass_back_ground_mxogq4yvvn")
                            val homeGroundGlassBackGroundMxogq4yvvn: HomeGroundGlassBackGroundMxogq4yvvn,
                            @SerializedName("home_ground_glass_back_ground_yc96n95amx")
                            val homeGroundGlassBackGroundYc96n95amx: HomeGroundGlassBackGroundYc96n95amx,
                            @SerializedName("home_page_rcmd_cloud_village_play_list_item_copy_3e6thhksc0")
                            val homePageRcmdCloudVillagePlayListItemCopy3e6thhksc0: HomePageRcmdCloudVillagePlayListItemCopy3e6thhksc0,
                            @SerializedName("home_page_rcmd_cloud_village_play_list_list_table_28mch2inm1")
                            val homePageRcmdCloudVillagePlayListListTable28mch2inm1: HomePageRcmdCloudVillagePlayListListTable28mch2inm1,
                            @SerializedName("home_page_rcmd_cloud_village_play_list_module_faw1pbe7y8")
                            val homePageRcmdCloudVillagePlayListModuleFaw1pbe7y8: HomePageRcmdCloudVillagePlayListModuleFaw1pbe7y8,
                            @SerializedName("new_home_page_vip_center_guide_module_42w4ziiqcf")
                            val newHomePageVipCenterGuideModule42w4ziiqcf: NewHomePageVipCenterGuideModule42w4ziiqcf,
                            @SerializedName("page_rcmd_greeting_activity_bar_c0bqitk6g2")
                            val pageRcmdGreetingActivityBarC0bqitk6g2: PageRcmdGreetingActivityBarC0bqitk6g2,
                            @SerializedName("page_rcmd_greeting_bar_list_jl4b6qchz3")
                            val pageRcmdGreetingBarListJl4b6qchz3: PageRcmdGreetingBarListJl4b6qchz3,
                            @SerializedName("page_rcmd_top_greeting_2bnzys1v1e")
                            val pageRcmdTopGreeting2bnzys1v1e: PageRcmdTopGreeting2bnzys1v1e,
                            @SerializedName("page_recommend_shared_playlist_avatar_item_ppiwrujhes")
                            val pageRecommendSharedPlaylistAvatarItemPpiwrujhes: PageRecommendSharedPlaylistAvatarItemPpiwrujhes,
                            @SerializedName("play_count_ggdliuj3or")
                            val playCountGgdliuj3or: PlayCountGgdliuj3or,
                            @SerializedName("rcmd_daily_big_card_item_copy_copy_rank_up2miqp06e")
                            val rcmdDailyBigCardItemCopyCopyRankUp2miqp06e: RcmdDailyBigCardItemCopyCopyRankUp2miqp06e,
                            @SerializedName("rcmd_daily_big_card_item_copy_copy_v2_fm_h5m567dqoh")
                            val rcmdDailyBigCardItemCopyCopyV2FmH5m567dqoh: RcmdDailyBigCardItemCopyCopyV2FmH5m567dqoh,
                            @SerializedName("rcmd_daily_big_card_item_copy_copy_v2_play_list_album_copy_1c5gxgw7zw")
                            val rcmdDailyBigCardItemCopyCopyV2PlayListAlbumCopy1c5gxgw7zw: RcmdDailyBigCardItemCopyCopyV2PlayListAlbumCopy1c5gxgw7zw,
                            @SerializedName("rcmd_daily_big_card_item_heart_mode_44cgowqchu")
                            val rcmdDailyBigCardItemHeartMode44cgowqchu: RcmdDailyBigCardItemHeartMode44cgowqchu,
                            @SerializedName("rcmd_daily_big_card_item_normal_with_icon_b8sjvnpzec")
                            val rcmdDailyBigCardItemNormalWithIconB8sjvnpzec: RcmdDailyBigCardItemNormalWithIconB8sjvnpzec,
                            @SerializedName("rcmd_daily_big_card_item_recall_playlist_tatdbvm9na")
                            val rcmdDailyBigCardItemRecallPlaylistTatdbvm9na: RcmdDailyBigCardItemRecallPlaylistTatdbvm9na,
                            @SerializedName("rcmd_daily_big_card_item_similar_artist_01l23vvgnp")
                            val rcmdDailyBigCardItemSimilarArtist01l23vvgnp: RcmdDailyBigCardItemSimilarArtist01l23vvgnp,
                            @SerializedName("rcmd_daily_big_card_item_similar_song_d2n95tot6l")
                            val rcmdDailyBigCardItemSimilarSongD2n95tot6l: RcmdDailyBigCardItemSimilarSongD2n95tot6l,
                            @SerializedName("rcmd_daily_card_common_item_s457tn5uue")
                            val rcmdDailyCardCommonItemS457tn5uue: RcmdDailyCardCommonItemS457tn5uue,
                            @SerializedName("rcmd_daily_card_item_fm_qhwied3mg2")
                            val rcmdDailyCardItemFmQhwied3mg2: RcmdDailyCardItemFmQhwied3mg2,
                            @SerializedName("rcmd_daily_card_item_mini_banner_gbflt5s6id")
                            val rcmdDailyCardItemMiniBannerGbflt5s6id: RcmdDailyCardItemMiniBannerGbflt5s6id,
                            @SerializedName("rcmd_daily_card_item_play_list_album_ab6yx1ds2f")
                            val rcmdDailyCardItemPlayListAlbumAb6yx1ds2f: RcmdDailyCardItemPlayListAlbumAb6yx1ds2f,
                            @SerializedName("rcmd_daily_card_item_play_list_album_copy_894sjc9sxd")
                            val rcmdDailyCardItemPlayListAlbumCopy894sjc9sxd: RcmdDailyCardItemPlayListAlbumCopy894sjc9sxd,
                            @SerializedName("rcmd_daily_card_item_style_rcmd_57rg03kwiz")
                            val rcmdDailyCardItemStyleRcmd57rg03kwiz: RcmdDailyCardItemStyleRcmd57rg03kwiz,
                            @SerializedName("rcmd_daily_card_list_okidnj8j06")
                            val rcmdDailyCardListOkidnj8j06: RcmdDailyCardListOkidnj8j06,
                            @SerializedName("rcmd_daily_card_live_item_u9qm0drcli")
                            val rcmdDailyCardLiveItemU9qm0drcli: RcmdDailyCardLiveItemU9qm0drcli,
                            @SerializedName("rcmd_daily_card_module_q9aeks8g75")
                            val rcmdDailyCardModuleQ9aeks8g75: RcmdDailyCardModuleQ9aeks8g75,
                            @SerializedName("rcmd_daily_common_no_play_btn_0vjlxbwkuy")
                            val rcmdDailyCommonNoPlayBtn0vjlxbwkuy: RcmdDailyCommonNoPlayBtn0vjlxbwkuy,
                            @SerializedName("rcmd_daily_podcast_big_card_item_c4c6636xx6")
                            val rcmdDailyPodcastBigCardItemC4c6636xx6: RcmdDailyPodcastBigCardItemC4c6636xx6,
                            @SerializedName("rcmd_daily_rcmd_dynamic_hidden_header_8srypues38")
                            val rcmdDailyRcmdDynamicHiddenHeader8srypues38: RcmdDailyRcmdDynamicHiddenHeader8srypues38,
                            @SerializedName("rcmd_radar_list_with_cover_item0710_cndqberk6i")
                            val rcmdRadarListWithCoverItem0710Cndqberk6i: RcmdRadarListWithCoverItem0710Cndqberk6i,
                            @SerializedName("rcmd_radar_with_mask_list0710_7e7xn390w8")
                            val rcmdRadarWithMaskList07107e7xn390w8: RcmdRadarWithMaskList07107e7xn390w8,
                            @SerializedName("rcmd_radar_with_mask_module0710_bd22kgyeqo")
                            val rcmdRadarWithMaskModule0710Bd22kgyeqo: RcmdRadarWithMaskModule0710Bd22kgyeqo,
                            @SerializedName("rcmd_top_greeting_ad_jvyaq7tx21")
                            val rcmdTopGreetingAdJvyaq7tx21: RcmdTopGreetingAdJvyaq7tx21,
                            @SerializedName("static_scroll_container_u59ognvt3u")
                            val staticScrollContainerU59ognvt3u: StaticScrollContainerU59ognvt3u,
                            @SerializedName("static_scroll_container_u59ognvt3u_virtual")
                            val staticScrollContainerU59ognvt3uVirtual: StaticScrollContainerU59ognvt3uVirtual,
                            @SerializedName("template_142003")
                            val template142003: Template142003,
                            @SerializedName("template_314201")
                            val template314201: Template314201,
                            @SerializedName("template_349202")
                            val template349202: Template349202,
                            @SerializedName("template_355201")
                            val template355201: Template355201,
                            @SerializedName("template_98001")
                            val template98001: Template98001
                        ) {
                            data class ArtisteParachuteGreetingHpak321yot(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class DailyMixSingleLineTextCopyLy2bff6we2(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class DailyMixSingleLineTextCopyQqy0we33tm(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class DailyRcmdAnnualAndMonthlyPlaylistItem47yuuxhs5s(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class EmptyIconD8vh7d7n9p(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class EmptyIconEqs0sun5u3(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class EmptyIconHvetjbwns3(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class EmptyIconI8h4kd56rz(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class EmptyIconLflntqarff(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class EmptyIconThrc7s8px1(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class GroundGlassBackGroundUh06xewrtz(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class GroundGlassBackGroundUh06xewrtz14(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class GroundGlassBackGroundUh06xewrtz18(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class GroundGlassBackGroundUh06xewrtz20(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class GroundGlassBackGroundUh06xewrtz23(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class GroundGlassBackGroundUh06xewrtz30(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeBottomMargin65cju8vvls(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeBottomMarginAo7hq65rhh(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeBottomMarginHfcxc755tw(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeCommonRcmdSongItemU59ognvt3u(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeCommonRcmdSongListLwxbsnqyiy(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeCommonRcmdSongsModule8pim9hfovz(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeCommonTitleNc13i9w3i3(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeCommonTitlePczt00v2qu(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeCommonTitleWithNickNameSxp3xmlqd7(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeGroundGlassBackGround926ehi7wma(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeGroundGlassBackGroundD8w9yhqyfq(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeGroundGlassBackGroundMxogq4yvvn(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomeGroundGlassBackGroundYc96n95amx(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomePageRcmdCloudVillagePlayListItemCopy3e6thhksc0(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomePageRcmdCloudVillagePlayListListTable28mch2inm1(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class HomePageRcmdCloudVillagePlayListModuleFaw1pbe7y8(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class NewHomePageVipCenterGuideModule42w4ziiqcf(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class PageRcmdGreetingActivityBarC0bqitk6g2(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class PageRcmdGreetingBarListJl4b6qchz3(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class PageRcmdTopGreeting2bnzys1v1e(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class PageRecommendSharedPlaylistAvatarItemPpiwrujhes(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class PlayCountGgdliuj3or(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyBigCardItemCopyCopyRankUp2miqp06e(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyBigCardItemCopyCopyV2FmH5m567dqoh(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyBigCardItemCopyCopyV2PlayListAlbumCopy1c5gxgw7zw(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyBigCardItemHeartMode44cgowqchu(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyBigCardItemNormalWithIconB8sjvnpzec(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyBigCardItemRecallPlaylistTatdbvm9na(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyBigCardItemSimilarArtist01l23vvgnp(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyBigCardItemSimilarSongD2n95tot6l(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyCardCommonItemS457tn5uue(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyCardItemFmQhwied3mg2(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyCardItemMiniBannerGbflt5s6id(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyCardItemPlayListAlbumAb6yx1ds2f(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyCardItemPlayListAlbumCopy894sjc9sxd(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyCardItemStyleRcmd57rg03kwiz(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyCardListOkidnj8j06(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyCardLiveItemU9qm0drcli(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyCardModuleQ9aeks8g75(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyCommonNoPlayBtn0vjlxbwkuy(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyPodcastBigCardItemC4c6636xx6(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdDailyRcmdDynamicHiddenHeader8srypues38(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdRadarListWithCoverItem0710Cndqberk6i(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdRadarWithMaskList07107e7xn390w8(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdRadarWithMaskModule0710Bd22kgyeqo(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class RcmdTopGreetingAdJvyaq7tx21(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class StaticScrollContainerU59ognvt3u(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class StaticScrollContainerU59ognvt3uVirtual(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class Template142003(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class Template314201(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class Template349202(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class Template355201(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )

                            data class Template98001(
                                @SerializedName("css")
                                val css: String,
                                @SerializedName("databinding")
                                val databinding: String,
                                @SerializedName("json")
                                val json: String
                            )
                        }
                    }
                }
            }

            data class DslData(
                @SerializedName("blockResource")
                val blockResource: BlockResource,
                @SerializedName("code")
                val code: Int,
                @SerializedName("dslShowTitle")
                val dslShowTitle: Boolean,
                @SerializedName("home_common_rcmd_songs_module_8pim9hfovz")
                val homeCommonRcmdSongsModule8pim9hfovz: HomeCommonRcmdSongsModule8pim9hfovz,
                @SerializedName("page_rcmd_top_greeting_2bnzys1v1e")
                val pageRcmdTopGreeting2bnzys1v1e: PageRcmdTopGreeting2bnzys1v1e,
                @SerializedName("responseFrom")
                val responseFrom: String
            ) {
                data class BlockResource(
                    @SerializedName("action")
                    val action: String,
                    @SerializedName("alg")
                    val alg: String,
                    @SerializedName("minLimitSize")
                    val minLimitSize: Int,
                    @SerializedName("resources")
                    val resources: List<Resource>,
                    @SerializedName("showMore")
                    val showMore: Boolean,
                    @SerializedName("subTitle")
                    val subTitle: String,
                    @SerializedName("title")
                    val title: String
                ) {
                    data class Resource(
                        @SerializedName("action")
                        val action: String,
                        @SerializedName("alg")
                        val alg: String,
                        @SerializedName("algTitle")
                        val algTitle: String,
                        @SerializedName("coverAlg")
                        val coverAlg: String,
                        @SerializedName("coverId")
                        val coverId: String,
                        @SerializedName("coverImg")
                        val coverImg: String,
                        @SerializedName("coverType")
                        val coverType: String,
                        @SerializedName("dayOfMonth")
                        val dayOfMonth: String,
                        @SerializedName("extInfo")
                        val extInfo: ExtInfo,
                        @SerializedName("firstSongId")
                        val firstSongId: String,
                        @SerializedName("iconDesc")
                        val iconDesc: IconDesc,
                        @SerializedName("lunaItemType")
                        val lunaItemType: String,
                        @SerializedName("moduleType")
                        val moduleType: String,
                        @SerializedName("playBtnData")
                        val playBtnData: String,
                        @SerializedName("resourceExtInfo")
                        val resourceExtInfo: ResourceExtInfo,
                        @SerializedName("resourceId")
                        val resourceId: String,
                        @SerializedName("resourceInteractInfo")
                        val resourceInteractInfo: ResourceInteractInfo,
                        @SerializedName("resourceStates")
                        val resourceStates: String,
                        @SerializedName("resourceType")
                        val resourceType: String,
                        @SerializedName("singleLineTitle")
                        val singleLineTitle: String,
                        @SerializedName("songIdsStr")
                        val songIdsStr: String,
                        @SerializedName("subTitle")
                        val subTitle: String,
                        @SerializedName("tagDisplay")
                        val tagDisplay: String,
                        @SerializedName("tagImgUrl")
                        val tagImgUrl: String,
                        @SerializedName("tagText")
                        val tagText: String,
                        @SerializedName("tags")
                        val tags: List<String>,
                        @SerializedName("title")
                        val title: String
                    ) {
                        data class ExtInfo(
                            @SerializedName("desc")
                            val desc: String,
                            @SerializedName("elementId")
                            val elementId: String,
                            @SerializedName("fmCoverConfig")
                            val fmCoverConfig: FmCoverConfig,
                            @SerializedName("fmCoverMaskConfig")
                            val fmCoverMaskConfig: FmCoverMaskConfig,
                            @SerializedName("fmSubTitleConfig")
                            val fmSubTitleConfig: FmSubTitleConfig,
                            @SerializedName("fmTitleBgConfig")
                            val fmTitleBgConfig: FmTitleBgConfig,
                            @SerializedName("ignoreGuide")
                            val ignoreGuide: Boolean,
                            @SerializedName("mode")
                            val mode: String,
                            @SerializedName("once")
                            val once: Boolean,
                            @SerializedName("playlist")
                            val playlist: Playlist,
                            @SerializedName("showFmTitle")
                            val showFmTitle: Boolean,
                            @SerializedName("songId")
                            val songId: Int,
                            @SerializedName("subMode")
                            val subMode: String,
                            @SerializedName("topGradient")
                            val topGradient: String,
                            @SerializedName("useMiniIcon")
                            val useMiniIcon: Boolean
                        ) {
                            data class FmCoverConfig(
                                @SerializedName("aidjDarkCover")
                                val aidjDarkCover: String,
                                @SerializedName("aidjLightCover")
                                val aidjLightCover: String,
                                @SerializedName("algTitle")
                                val algTitle: String,
                                @SerializedName("coverImg")
                                val coverImg: String,
                                @SerializedName("isFmCard")
                                val isFmCard: Boolean,
                                @SerializedName("modeCode")
                                val modeCode: String,
                                @SerializedName("subModeCode")
                                val subModeCode: String
                            )

                            data class FmCoverMaskConfig(
                                @SerializedName("algTitle")
                                val algTitle: String,
                                @SerializedName("colorRect")
                                val colorRect: ColorRect,
                                @SerializedName("direction")
                                val direction: String,
                                @SerializedName("enableCrossDissolve")
                                val enableCrossDissolve: Boolean,
                                @SerializedName("excludeBlackWhiteColor")
                                val excludeBlackWhiteColor: Boolean,
                                @SerializedName("iconConfig")
                                val iconConfig: IconConfig,
                                @SerializedName("modeCode")
                                val modeCode: String,
                                @SerializedName("serverColorArea")
                                val serverColorArea: String,
                                @SerializedName("source")
                                val source: String,
                                @SerializedName("subModeCode")
                                val subModeCode: String,
                                @SerializedName("type")
                                val type: String
                            ) {
                                data class ColorRect(
                                    @SerializedName("heightRatio")
                                    val heightRatio: Int,
                                    @SerializedName("widthRatio")
                                    val widthRatio: Int,
                                    @SerializedName("xRatio")
                                    val xRatio: Int,
                                    @SerializedName("yRatio")
                                    val yRatio: Int
                                )

                                data class IconConfig(
                                    @SerializedName("iconHeightRatio")
                                    val iconHeightRatio: Double,
                                    @SerializedName("iconUrl")
                                    val iconUrl: String,
                                    @SerializedName("iconWidthRatio")
                                    val iconWidthRatio: Double,
                                    @SerializedName("marginLeftRatio")
                                    val marginLeftRatio: Double,
                                    @SerializedName("marginTopRatio")
                                    val marginTopRatio: Double
                                )
                            }

                            data class FmSubTitleConfig(
                                @SerializedName("algTitle")
                                val algTitle: String,
                                @SerializedName("colorRect")
                                val colorRect: ColorRect,
                                @SerializedName("direction")
                                val direction: String,
                                @SerializedName("enableCrossDissolve")
                                val enableCrossDissolve: Boolean,
                                @SerializedName("excludeBlackWhiteColor")
                                val excludeBlackWhiteColor: Boolean,
                                @SerializedName("hiddenInAIDJ")
                                val hiddenInAIDJ: Boolean,
                                @SerializedName("isFmCard")
                                val isFmCard: Boolean,
                                @SerializedName("modeCode")
                                val modeCode: String,
                                @SerializedName("serverColorArea")
                                val serverColorArea: String,
                                @SerializedName("source")
                                val source: String,
                                @SerializedName("subModeCode")
                                val subModeCode: String,
                                @SerializedName("type")
                                val type: String
                            ) {
                                data class ColorRect(
                                    @SerializedName("heightRatio")
                                    val heightRatio: Double,
                                    @SerializedName("widthRatio")
                                    val widthRatio: Int,
                                    @SerializedName("xRatio")
                                    val xRatio: Int,
                                    @SerializedName("yRatio")
                                    val yRatio: Int
                                )
                            }

                            data class FmTitleBgConfig(
                                @SerializedName("algTitle")
                                val algTitle: String,
                                @SerializedName("colorRect")
                                val colorRect: ColorRect,
                                @SerializedName("direction")
                                val direction: String,
                                @SerializedName("enableCrossDissolve")
                                val enableCrossDissolve: Boolean,
                                @SerializedName("excludeBlackWhiteColor")
                                val excludeBlackWhiteColor: Boolean,
                                @SerializedName("isFmCard")
                                val isFmCard: Boolean,
                                @SerializedName("modeCode")
                                val modeCode: String,
                                @SerializedName("serverColorArea")
                                val serverColorArea: String,
                                @SerializedName("source")
                                val source: String,
                                @SerializedName("subModeCode")
                                val subModeCode: String,
                                @SerializedName("type")
                                val type: String
                            ) {
                                data class ColorRect(
                                    @SerializedName("heightRatio")
                                    val heightRatio: Double,
                                    @SerializedName("widthRatio")
                                    val widthRatio: Int,
                                    @SerializedName("xRatio")
                                    val xRatio: Int,
                                    @SerializedName("yRatio")
                                    val yRatio: Double
                                )
                            }

                            data class Playlist(
                                @SerializedName("adType")
                                val adType: Int,
                                @SerializedName("adjustedPlayCount")
                                val adjustedPlayCount: Int,
                                @SerializedName("anonimous")
                                val anonimous: Boolean,
                                @SerializedName("approved")
                                val approved: Boolean,
                                @SerializedName("auditRejected")
                                val auditRejected: Boolean,
                                @SerializedName("auditStatus")
                                val auditStatus: Int,
                                @SerializedName("auditTime")
                                val auditTime: Int,
                                @SerializedName("bookTime")
                                val bookTime: Int,
                                @SerializedName("bookedCount")
                                val bookedCount: Int,
                                @SerializedName("commentThreadId")
                                val commentThreadId: Int,
                                @SerializedName("copied")
                                val copied: Boolean,
                                @SerializedName("coverImgId")
                                val coverImgId: Long,
                                @SerializedName("coverImgUrl")
                                val coverImgUrl: String,
                                @SerializedName("coverStatus")
                                val coverStatus: Int,
                                @SerializedName("createEventId")
                                val createEventId: Int,
                                @SerializedName("createTime")
                                val createTime: Long,
                                @SerializedName("defaultCover")
                                val defaultCover: Boolean,
                                @SerializedName("freeLimitExpire")
                                val freeLimitExpire: Boolean,
                                @SerializedName("highQuality")
                                val highQuality: Boolean,
                                @SerializedName("id")
                                val id: Long,
                                @SerializedName("name")
                                val name: String,
                                @SerializedName("newImported")
                                val newImported: Boolean,
                                @SerializedName("opRecommend")
                                val opRecommend: Boolean,
                                @SerializedName("playCount")
                                val playCount: Int,
                                @SerializedName("playlistType")
                                val playlistType: String,
                                @SerializedName("privacy")
                                val privacy: Int,
                                @SerializedName("recommended")
                                val recommended: Boolean,
                                @SerializedName("specialType")
                                val specialType: Int,
                                @SerializedName("status")
                                val status: Int,
                                @SerializedName("tags")
                                val tags: List<Any>,
                                @SerializedName("totalDuration")
                                val totalDuration: Int,
                                @SerializedName("trackCount")
                                val trackCount: Int,
                                @SerializedName("trackNumberUpdateTime")
                                val trackNumberUpdateTime: Long,
                                @SerializedName("trackUpdateTime")
                                val trackUpdateTime: Long,
                                @SerializedName("updateTime")
                                val updateTime: Long,
                                @SerializedName("userId")
                                val userId: Long,
                                @SerializedName("userOrdered")
                                val userOrdered: Boolean,
                                @SerializedName("validCloudTrackCount")
                                val validCloudTrackCount: Int,
                                @SerializedName("validTrackCount")
                                val validTrackCount: Int,
                                @SerializedName("weightValue")
                                val weightValue: Int
                            )
                        }

                        data class IconDesc(
                            @SerializedName("blur_bottom")
                            val blurBottom: String?,
                            @SerializedName("blur_left")
                            val blurLeft: String?,
                            @SerializedName("blur_right")
                            val blurRight: String?,
                            @SerializedName("blur_show")
                            val blurShow: Boolean,
                            @SerializedName("blur_top")
                            val blurTop: String?,
                            @SerializedName("image")
                            val image: String?
                        )

                        data class ResourceExtInfo(
                            @SerializedName("artists")
                            val artists: List<Artist>,
                            @SerializedName("coverText")
                            val coverText: List<String>
                        ) {
                            data class Artist(
                                @SerializedName("id")
                                val id: Int,
                                @SerializedName("imgUrl")
                                val imgUrl: String,
                                @SerializedName("name")
                                val name: String,
                                @SerializedName("nickName")
                                val nickName: String
                            )
                        }

                        data class ResourceInteractInfo(
                            @SerializedName("playCount")
                            val playCount: String
                        )
                    }
                }

                data class HomeCommonRcmdSongsModule8pim9hfovz(
                    @SerializedName("content")
                    val content: Content,
                    @SerializedName("ctype")
                    val ctype: String,
                    @SerializedName("header")
                    val header: Header,
                    @SerializedName("oid")
                    val oid: String
                ) {
                    data class Content(
                        @SerializedName("items")
                        val items: List<Item>,
                        @SerializedName("oid")
                        val oid: String
                    ) {
                        data class Item(
                            @SerializedName("items")
                            val items: List<Item>
                        ) {
                            data class Item(
                                @SerializedName("alg")
                                val alg: String,
                                @SerializedName("artistName")
                                val artistName: String,
                                @SerializedName("clickAction")
                                val clickAction: ClickAction,
                                @SerializedName("coverUrl")
                                val coverUrl: String,
                                @SerializedName("like")
                                val like: Int,
                                @SerializedName("likeDisplay")
                                val likeDisplay: String,
                                @SerializedName("oid")
                                val oid: String,
                                @SerializedName("playBtn")
                                val playBtn: PlayBtn,
                                @SerializedName("position")
                                val position: Int,
                                @SerializedName("recReasonDisplay")
                                val recReasonDisplay: String,
                                @SerializedName("resourceId")
                                val resourceId: String,
                                @SerializedName("resourceType")
                                val resourceType: String,
                                @SerializedName("singleSongCtrp")
                                val singleSongCtrp: String,
                                @SerializedName("tag")
                                val tag: String,
                                @SerializedName("tagDisplay")
                                val tagDisplay: String,
                                @SerializedName("title")
                                val title: String
                            ) {
                                data class ClickAction(
                                    @SerializedName("msg")
                                    val msg: Msg,
                                    @SerializedName("type")
                                    val type: String
                                ) {
                                    data class Msg(
                                        @SerializedName("method")
                                        val method: String,
                                        @SerializedName("module")
                                        val module: String,
                                        @SerializedName("params")
                                        val params: Params
                                    ) {
                                        data class Params(
                                            @SerializedName("algs")
                                            val algs: List<String>,
                                            @SerializedName("playParams")
                                            val playParams: PlayParams,
                                            @SerializedName("songIds")
                                            val songIds: List<String>,
                                            @SerializedName("songIndex")
                                            val songIndex: Int
                                        ) {
                                            data class PlayParams(
                                                @SerializedName("playerType")
                                                val playerType: String,
                                                @SerializedName("playingShowUI")
                                                val playingShowUI: Boolean,
                                                @SerializedName("showUI")
                                                val showUI: Boolean,
                                                @SerializedName("sourceDes")
                                                val sourceDes: String,
                                                @SerializedName("sourceModuleName")
                                                val sourceModuleName: String,
                                                @SerializedName("sourceResourceName")
                                                val sourceResourceName: String,
                                                @SerializedName("trialSceneMode")
                                                val trialSceneMode: Int
                                            )
                                        }
                                    }
                                }

                                data class PlayBtn(
                                    @SerializedName("detailUrl")
                                    val detailUrl: String,
                                    @SerializedName("pauseType")
                                    val pauseType: String,
                                    @SerializedName("playAction")
                                    val playAction: PlayAction,
                                    @SerializedName("playType")
                                    val playType: String,
                                    @SerializedName("resourceId")
                                    val resourceId: String,
                                    @SerializedName("tintColor")
                                    val tintColor: String
                                ) {
                                    data class PlayAction(
                                        @SerializedName("algs")
                                        val algs: List<String>,
                                        @SerializedName("playParams")
                                        val playParams: PlayParams,
                                        @SerializedName("songIds")
                                        val songIds: List<String>,
                                        @SerializedName("songIndex")
                                        val songIndex: Int
                                    ) {
                                        data class PlayParams(
                                            @SerializedName("playerType")
                                            val playerType: String,
                                            @SerializedName("playingShowUI")
                                            val playingShowUI: Boolean,
                                            @SerializedName("showUI")
                                            val showUI: Boolean,
                                            @SerializedName("sourceDes")
                                            val sourceDes: String,
                                            @SerializedName("sourceModuleName")
                                            val sourceModuleName: String,
                                            @SerializedName("sourceResourceName")
                                            val sourceResourceName: String,
                                            @SerializedName("trialSceneMode")
                                            val trialSceneMode: Int
                                        )
                                    }
                                }
                            }
                        }
                    }

                    data class Header(
                        @SerializedName("showMore")
                        val showMore: Boolean,
                        @SerializedName("title")
                        val title: String
                    )
                }

                data class PageRcmdTopGreeting2bnzys1v1e(
                    @SerializedName("commonResourceList")
                    val commonResourceList: List<CommonResource>,
                    @SerializedName("dataGroupResourceList")
                    val dataGroupResourceList: List<DataGroupResource>
                ) {
                    data class CommonResource(
                        @SerializedName("actionUrl")
                        val actionUrl: String,
                        @SerializedName("blockName")
                        val blockName: String,
                        @SerializedName("c_flowGroupId")
                        val cFlowGroupId: Int,
                        @SerializedName("channelCode")
                        val channelCode: String,
                        @SerializedName("constructLogId")
                        val constructLogId: String,
                        @SerializedName("creativeId")
                        val creativeId: Int,
                        @SerializedName("creativeReachId")
                        val creativeReachId: String,
                        @SerializedName("duration")
                        val duration: Int,
                        @SerializedName("endTime")
                        val endTime: Int,
                        @SerializedName("extraMap")
                        val extraMap: ExtraMap,
                        @SerializedName("home_greet_rcmd_title_resource")
                        val homeGreetRcmdTitleResource: HomeGreetRcmdTitleResource,
                        @SerializedName("icon")
                        val icon: String,
                        @SerializedName("iconType")
                        val iconType: Int,
                        @SerializedName("log")
                        val log: Log,
                        @SerializedName("logMap")
                        val logMap: LogMap,
                        @SerializedName("lunaItemType")
                        val lunaItemType: String,
                        @SerializedName("planId")
                        val planId: String,
                        @SerializedName("position")
                        val position: Int,
                        @SerializedName("positionCode")
                        val positionCode: String,
                        @SerializedName("resourceId")
                        val resourceId: String,
                        @SerializedName("resourceType")
                        val resourceType: String,
                        @SerializedName("s_ctrp")
                        val sCtrp: String,
                        @SerializedName("showType")
                        val showType: String,
                        @SerializedName("startTime")
                        val startTime: Int,
                        @SerializedName("subCommonResourceList")
                        val subCommonResourceList: List<SubCommonResource>,
                        @SerializedName("subIndex")
                        val subIndex: Int,
                        @SerializedName("summary")
                        val summary: String,
                        @SerializedName("templateId")
                        val templateId: Int,
                        @SerializedName("title")
                        val title: String,
                        @SerializedName("trp_id")
                        val trpId: String,
                        @SerializedName("trp_type")
                        val trpType: String
                    ) {
                        data class ExtraMap(
                            @SerializedName("arrowIconJson")
                            val arrowIconJson: String,
                            @SerializedName("backgroundColor")
                            val backgroundColor: String,
                            @SerializedName("borderColor")
                            val borderColor: String,
                            @SerializedName("darkIcon")
                            val darkIcon: String,
                            @SerializedName("iconHeight")
                            val iconHeight: Int,
                            @SerializedName("iconWidth")
                            val iconWidth: Int,
                            @SerializedName("isSquareIcon")
                            val isSquareIcon: Boolean,
                            @SerializedName("titleColor")
                            val titleColor: String
                        )

                        data class HomeGreetRcmdTitleResource(
                            @SerializedName("title")
                            val title: String,
                            @SerializedName("title_en_US")
                            val titleEnUS: String,
                            @SerializedName("title_zh_HK")
                            val titleZhHK: String,
                            @SerializedName("title_zh_TW")
                            val titleZhTW: String
                        )

                        data class Log(
                            @SerializedName("hitType")
                            val hitType: String,
                            @SerializedName("logContext")
                            val logContext: String,
                            @SerializedName("s_ctrp")
                            val sCtrp: String
                        )

                        data class LogMap(
                            @SerializedName("cc")
                            val cc: String,
                            @SerializedName("fgid")
                            val fgid: String,
                            @SerializedName("pc")
                            val pc: String
                        )

                        data class SubCommonResource(
                            @SerializedName("actionUrl")
                            val actionUrl: String,
                            @SerializedName("constructLogId")
                            val constructLogId: String,
                            @SerializedName("duration")
                            val duration: Int,
                            @SerializedName("endTime")
                            val endTime: Int,
                            @SerializedName("extraMap")
                            val extraMap: ExtraMap,
                            @SerializedName("icon")
                            val icon: String,
                            @SerializedName("iconType")
                            val iconType: Int,
                            @SerializedName("log")
                            val log: Log,
                            @SerializedName("logMap")
                            val logMap: LogMap,
                            @SerializedName("lunaItemType")
                            val lunaItemType: String,
                            @SerializedName("position")
                            val position: Int,
                            @SerializedName("resourceType")
                            val resourceType: String,
                            @SerializedName("showType")
                            val showType: String,
                            @SerializedName("startTime")
                            val startTime: Int,
                            @SerializedName("title")
                            val title: String
                        ) {
                            data class ExtraMap(
                                @SerializedName("arrowIconJson")
                                val arrowIconJson: String,
                                @SerializedName("backgroundColor")
                                val backgroundColor: String,
                                @SerializedName("borderColor")
                                val borderColor: String,
                                @SerializedName("darkIcon")
                                val darkIcon: String,
                                @SerializedName("iconHeight")
                                val iconHeight: Int,
                                @SerializedName("iconWidth")
                                val iconWidth: Int,
                                @SerializedName("isSquareIcon")
                                val isSquareIcon: Boolean,
                                @SerializedName("titleColor")
                                val titleColor: String
                            )

                            data class Log(
                                @SerializedName("hitType")
                                val hitType: String,
                                @SerializedName("logContext")
                                val logContext: String
                            )

                            data class LogMap(
                                @SerializedName("fgid")
                                val fgid: String
                            )
                        }
                    }

                    data class DataGroupResource(
                        @SerializedName("blockName")
                        val blockName: String,
                        @SerializedName("channelCode")
                        val channelCode: String,
                        @SerializedName("creativeId")
                        val creativeId: Int,
                        @SerializedName("creativeReachId")
                        val creativeReachId: String,
                        @SerializedName("home_greet_rcmd_title_resource")
                        val homeGreetRcmdTitleResource: HomeGreetRcmdTitleResource,
                        @SerializedName("log")
                        val log: Log,
                        @SerializedName("planId")
                        val planId: String,
                        @SerializedName("positionCode")
                        val positionCode: String,
                        @SerializedName("resourceId")
                        val resourceId: String,
                        @SerializedName("resourceType")
                        val resourceType: String,
                        @SerializedName("s_ctrp")
                        val sCtrp: String,
                        @SerializedName("subIndex")
                        val subIndex: Int,
                        @SerializedName("templateId")
                        val templateId: Int,
                        @SerializedName("trp_id")
                        val trpId: String,
                        @SerializedName("trp_type")
                        val trpType: String
                    ) {
                        data class HomeGreetRcmdTitleResource(
                            @SerializedName("title")
                            val title: String,
                            @SerializedName("title_en_US")
                            val titleEnUS: String,
                            @SerializedName("title_zh_HK")
                            val titleZhHK: String,
                            @SerializedName("title_zh_TW")
                            val titleZhTW: String
                        )

                        data class Log(
                            @SerializedName("s_ctrp")
                            val sCtrp: String
                        )
                    }
                }
            }

            data class ExtMap(
                @SerializedName("cacheable")
                val cacheable: String,
                @SerializedName("clientCacheExpireTime")
                val clientCacheExpireTime: String,
                @SerializedName("demote")
                val demote: String
            )

            data class Log(
                @SerializedName("s_ctrp")
                val sCtrp: String
            )

            data class LogMap(
                @SerializedName("cc")
                val cc: String,
                @SerializedName("fgid")
                val fgid: String,
                @SerializedName("pc")
                val pc: String
            )
        }

        data class ExtMap(
            @SerializedName("pageStyleType")
            val pageStyleType: String
        )
    }

    data class Trp(
        @SerializedName("rules")
        val rules: List<String>
    )
}