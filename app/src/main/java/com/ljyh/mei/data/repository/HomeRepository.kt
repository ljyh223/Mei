package com.ljyh.mei.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class HomeRepository(private val eApiService: EApiService, private val apiService: ApiService) {

    val context = AppContext.instance
    suspend fun getHomePageResourceShow(
        refresh: Boolean = false
    ): Resource<List<HomePageResourceShow.Data.Block>> {

        // 1. 如果是新的一天或者刷新，尝试去网络请求更新缓存
        if (isNewDay(getLastFetchTime(context)) || refresh) {
            Timber.tag("getHomePageResourceShow").d("新加载")
            try {
                val page1 = eApiService.getHomePageResourceShow(body = buildGetHomePageResourceShow(refresh = "false"))
                val page2 = eApiService.getHomePageResourceShow(body = buildGetHomePageResourceShow(refresh = refresh.toString()))

                saveLastHomePage(context, page = 1, newData = page1.data.blocks)
                saveLastHomePage(context, page = 2, newData = page2.data.blocks)

                Timber.tag("getHomePageResourceShow").d("更新缓存")
            } catch (e: Exception) {
                // 断网或请求失败时捕获，不让 App 崩溃
                Timber.tag("getHomePageResourceShow").e(t = e, message = "断网或网络请求失败，降级读取缓存")
            }
        }

        // 2. 无论上面联网成功还是失败（断网），统一从本地缓存读取数据返回给 UI
        val page1 = getLastHomePage(context, page = 1)

        // 只拿 page1 的数据（避免与 page2 拼接冲突），并且对 blocks 内部按照 blockCode 或实例彻底去重
        val safeBlocks = page1?.distinctBy { it.toString() } ?: emptyList()
        return Resource.Success(data = safeBlocks)
    }

    private suspend fun saveLastHomePage(
        context: Context,
        page: Int,
        newData: List<HomePageResourceShow.Data.Block>
    ) {
        withContext(Dispatchers.IO) {
            val file = getFileForPage(context, page)
            val json = Gson().toJson(newData)
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
                    val gson = Gson()
                    gson.fromJson(
                        json,
                        object : TypeToken<List<HomePageResourceShow.Data.Block>>() {}.type
                    )
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
