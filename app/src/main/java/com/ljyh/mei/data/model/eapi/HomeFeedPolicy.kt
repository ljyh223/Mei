package com.ljyh.mei.data.model.eapi

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** The music home surface has its own module order, independent of server template names. */
object HomeFeedPolicy {
    val moduleOrder = listOf(
        "PAGE_RECOMMEND_DAILY_RECOMMEND",
        "PAGE_RECOMMEND_RADAR",
        "PAGE_RECOMMEND_BANNER_1",
        "PAGE_RECOMMEND_FEELING_PLAYLIST_LOCATION",
        "PAGE_RECOMMEND_SHORTCUT",
        "PAGE_RECOMMEND_SCENE_PLAYLIST_LOCATION",
        "PAGE_RECOMMEND_COMBINATION",
        "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST",
        "PAGE_RECOMMEND_FIRM_PLAYLIST",
        "PAGE_RECOMMEND_RANK",
        "PAGE_RECOMMEND_NEW_SONG_AND_ALBUM",
        "PAGE_RECOMMEND_RED_SIMILAR_SONG",
        "PAGE_RECOMMEND_MONTH_YEAR_PLAYLIST",
        "PAGE_RECOMMEND_PRIVATE_RCMD_SONG",
        "PAGE_RECOMMEND_ARTIST_TREND",
    )
    // Server downgrade state is not the presentation order. Bootstrap from the
    // verified Android request; replace it with the server-issued value thereafter.
    val fallbackOrder = listOf(
        "PAGE_RECOMMEND_DAILY_RECOMMEND", "PAGE_RECOMMEND_FANS_GROUP_LEADER",
        "PAGE_RECOMMEND_VIP_SMALL_CARD", "PAGE_RECOMMEND_VIP_MODULE", "PAGE_RECOMMEND_BANNER_6",
        "PAGE_RECOMMEND_RADAR", "PAGE_RECOMMEND_BANNER_1", "PAGE_RECOMMEND_FEELING_PLAYLIST_LOCATION",
        "PAGE_RECOMMEND_SHORTCUT", "PAGE_RECOMMEND_SCENE_PLAYLIST_LOCATION", "PAGE_RECOMMEND_COMBINATION",
        "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST", "PAGE_RECOMMEND_SURVEY", "PAGE_RECOMMEND_FIRM_PLAYLIST",
        "PAGE_RECOMMEND_RANK", "PAGE_RECOMMEND_NEW_SONG_AND_ALBUM", "PAGE_RECOMMEND_RED_SIMILAR_SONG",
        "PAGE_RECOMMEND_MONTH_YEAR_PLAYLIST", "PAGE_RECOMMEND_PRIVATE_RCMD_SONG", "PAGE_RECOMMEND_ARTIST_TREND",
        "PAGE_RECOMMEND_PODCAST_RADIO_PROGRAM",
    )
    private val priority = moduleOrder.withIndex().associate { it.value to it.index }
    private val dailyMusicTypes = setOf("dailySongs", "star", "fm", "similarArtist", "playList")

    fun dailyMusicCards(cards: JsonArray?): List<JsonObject> = cards.orEmpty().filterIsInstance<JsonObject>()
        .filter { (it["resourceType"] as? JsonPrimitive)?.contentOrNull in dailyMusicTypes }

    fun visibleBanners(banners: JsonArray): List<JsonObject> = banners.filterIsInstance<JsonObject>()
        .filterNot { (it["showAdTag"] as? JsonPrimitive)?.booleanOrNull == true }

    fun visibleBlocks(blocks: List<HomePageResourceShow.Data.Block>) = blocks
        .filterNot {
            val code = it.positionCode.uppercase()
            val banners = it.nativeData?.get("banners") as? JsonArray
            code.contains("PODCAST") || code.contains("AUDIOBOOK") || code.contains("AUDIO_BOOK") ||
                (banners != null && visibleBanners(banners).isEmpty())
        }
        .sortedBy { priority[it.positionCode] ?: Int.MAX_VALUE }
}
