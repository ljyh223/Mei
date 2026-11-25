package com.ljyh.mei.data.repository

import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.eapi.HomePageResourceShow
import com.ljyh.mei.data.model.eapi.HomeFeedPolicy
import com.ljyh.mei.data.model.weapi.buildGetHomePageResourceShow
import com.ljyh.mei.data.model.weapi.buildAdExtJson
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.safeApiCall
import com.ljyh.mei.utils.preferences.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import com.ljyh.mei.data.network.Resource
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString

interface HomeFeedSource {
    fun isHomePageCacheFresh(userId: String): Boolean
    suspend fun getCachedHomePageResourceShow(userId: String): HomePageResourceShow.Data?
    suspend fun getHomePageResourceShow(refresh: Boolean = false): Resource<HomePageResourceShow.Data>
    suspend fun loadMore(previous: HomePageResourceShow.Data): Resource<HomePageResourceShow.Data>
}

class HomeRepository(private val eApiService: EApiService, private val apiService: ApiService) : HomeFeedSource {
    private val context = AppContext.instance
    private val adDeviceIdProvider = AdDeviceIdProvider(context, eApiService)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

    private val cache = HomeFeedCache(context.filesDir, json)

    override fun isHomePageCacheFresh(userId: String): Boolean =
        cache.peek(userId)?.let { HomeFeedCachePolicy.isFresh(it.savedAt, System.currentTimeMillis()) } == true

    override suspend fun getCachedHomePageResourceShow(userId: String) = withContext(Dispatchers.IO) {
        if (context.dataStore.data.first()[UserIdKey].orEmpty() != userId) null else cache.read(userId)?.data
    }

    override suspend fun getHomePageResourceShow(refresh: Boolean) = fetch(null, refresh)
    override suspend fun loadMore(previous: HomePageResourceShow.Data) = fetch(previous, false)

    private suspend fun fetch(previous: HomePageResourceShow.Data?, refresh: Boolean) = withContext(Dispatchers.IO) {
        safeApiCall {
            val user = context.dataStore.data.first()[UserIdKey].orEmpty()
            // Pagination already carries its cursor and downgrade state; do not decode the growing cache again.
            val stored = if (previous == null) cache.read(user) else null
            val saved = stored?.data
            if (previous == null && !refresh && stored != null &&
                HomeFeedCachePolicy.isFresh(stored.savedAt, System.currentTimeMillis())) {
                return@safeApiCall stored.data
            }
            val adId = adDeviceIdProvider.cachedAndRefresh()
            val request = buildGetHomePageResourceShow("true") {
                isFirstScreen = (previous == null).toString()
                cursor = (previous?.cursor ?: 0).toString()
                loadedPositionCodes = previous?.let {
                    json.encodeToString((it.loadedPositionCodes + it.blocks.map { block -> block.positionCode }).distinct())
                }.orEmpty()
                blockCodeOrderList = previous?.blockCodeOrderList
                algDemoteBlockCodeOrderList = if (previous == null) {
                    saved?.algDemoteBlockCodeOrderList ?: json.encodeToString(HomeFeedPolicy.fallbackOrder)
                } else null
                callbackParameters = previous?.callbackParameters.orEmpty()
                clientCacheBlockCode = "[]"
                val metrics = context.resources.displayMetrics
                widthDp = (metrics.widthPixels / metrics.density).toString()
                heightDp = (metrics.heightPixels / metrics.density).toString()
                val extras = json.parseToJsonElement(extJson).jsonObject.toMutableMap()
                extras["refreshAction"] = JsonPrimitive(if (previous != null) "loadMore" else if (refresh) "pull" else "init")
                extras["firstRequestPerLaunch"] = JsonPrimitive(previous == null && !refresh)
                extJson = JsonObject(extras).toString()
                adExtJson = json.encodeToString(buildAdExtJson { adDeviceId = adId })
            }
            val requestedAt = System.currentTimeMillis()
            val response = eApiService.getHomePageResourceShow(request)
            check(response.code == 200) { "Home request failed: ${response.code}" }
            val page = response.data
            timber.log.Timber.tag("HomeFeed").d("cursor=%d next=%d hasMore=%s modules=%s",
                previous?.cursor ?: 0, page.cursor, page.hasMore,
                page.blocks.joinToString { it.positionCode })
            val combined = previous?.append(page) ?: page.copy(loadedPositionCodes = page.blocks.map { it.positionCode }.distinct())
            if (context.dataStore.data.first()[UserIdKey].orEmpty() == user) {
                if (previous == null) cache.store(user, combined, requestedAt)
                else cache.storeNextPage(user, previous, combined)
            }
            combined
        }
    }
}
