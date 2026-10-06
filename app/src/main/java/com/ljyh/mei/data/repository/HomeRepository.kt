package com.ljyh.mei.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.AppContext
import com.ljyh.mei.constants.LastHomePageData_1
import com.ljyh.mei.constants.LastHomePageData_2
import com.ljyh.mei.constants.LastHomePageTime
import com.ljyh.mei.data.model.eapi.HomePageResourceShow
import com.ljyh.mei.data.model.api.GetSearch
import com.ljyh.mei.data.model.api.SearchResult
import com.ljyh.mei.data.model.weapi.GetHomePageResourceShow
import com.ljyh.mei.data.model.weapi.buildGetHomePageResourceShow
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.data.network.api.ApiService
import com.ljyh.mei.data.network.api.EApiService
import com.ljyh.mei.data.network.api.WeApiService
import com.ljyh.mei.data.network.safeApiCall
import com.ljyh.mei.utils.cache.CacheFile
import com.ljyh.mei.utils.cache.CacheFile.isNewDay
import com.ljyh.mei.utils.preferences.dataStore
import com.ljyh.mei.utils.preferences.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

class HomeRepository(private val eApiService: EApiService, private val apiService: ApiService) {
    private val cacheJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    val context = AppContext.instance
    suspend fun getHomePageResourceShow(
        refresh: Boolean = false
    ): Resource<List<HomePageResourceShow.Data.Block>> {
        Timber.tag("NewDay").d(isNewDay(getLastFetchTime(context)).toString())
        Timber.tag("refresh").d(refresh.toString())
        if (isNewDay(getLastFetchTime(context)) || refresh) {
            Timber.tag("getHomePageResourceShow").d("新加载")
            return withContext(Dispatchers.IO) {
                safeApiCall {
                    val blocks = eApiService.getHomePageResourceShow(
                        buildGetHomePageResourceShow(refresh = refresh.toString())
                    ).data.blocks
                    saveLastHomePage(context, 1, blocks)
                    Timber.tag("getHomePageResourceShow").d("更新缓存")
                    blocks
                }
            }
        } else {
            Timber.tag("getHomePageResourceShow").d("加载缓存")
            val page1 = getLastHomePage(context, 1)
            if (page1.isEmpty()) return getHomePageResourceShow(refresh = true)
//            val page2 = getLastHomePage(context, 2)
            return Resource.Success(page1)
        }
    }


    private suspend fun saveLastHomePage(
        context: Context,
        page: Int,
        newData: List<HomePageResourceShow.Data.Block>
    ) {
        withContext(Dispatchers.IO) {
            val file = getFileForPage(context, page)
            val json = cacheJson.encodeToString(newData)
            file.writeText(json)
            context.dataStore.edit {
                it[LastHomePageTime] = System.currentTimeMillis()
            }
        }
    }

    private suspend fun getLastHomePage(
        context: Context,
        page: Int
    ): List<HomePageResourceShow.Data.Block> {
        return withContext(Dispatchers.IO) {
            val file = getFileForPage(context, page)
            if (file.exists()) {
                try {
                    val json = file.readText()
                    if (json.isBlank()) {
                        return@withContext emptyList()
                    }
                    cacheJson.decodeFromString<List<HomePageResourceShow.Data.Block>>(json)
                } catch (e: Exception) {
                    // 捕获 JSON 语法错误 (JsonSyntaxException)、IO读取错误等所有异常
                    e.printStackTrace() // 打印错误日志方便调试，不需要的话可以删掉这行
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }



    private fun getFileForPage(context: Context, page: Int): File {
        return File(context.filesDir, "home_page_data_$page.json")
    }

    private suspend fun getLastFetchTime(context: Context): Long {
        val preferences = context.dataStore.data.first()
        Timber.tag("getLastFetchTime").d((preferences[LastHomePageTime] ?: 0L).toString())
        return preferences[LastHomePageTime] ?: 0L
    }
}
