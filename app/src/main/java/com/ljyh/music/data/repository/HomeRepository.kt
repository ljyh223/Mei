package com.ljyh.music.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ljyh.music.AppContext
import com.ljyh.music.constants.LastHomePageData_1
import com.ljyh.music.constants.LastHomePageData_2
import com.ljyh.music.constants.LastHomePageTime
import com.ljyh.music.data.model.HomePageResourceShow
import com.ljyh.music.data.model.weapi.GetHomePageResourceShow
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.network.api.EApiService
import com.ljyh.music.data.network.api.WeApiService
import com.ljyh.music.data.network.safeApiCall
import com.ljyh.music.utils.CacheFile
import com.ljyh.music.utils.CacheFile.isNewDay
import com.ljyh.music.utils.dataStore
import com.ljyh.music.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class HomeRepository(private val eApiService: EApiService) {

    val context = AppContext.instance
    suspend fun getHomePageResourceShow(
        refresh: Boolean = false
    ): Resource<List<HomePageResourceShow.Data.Block>> {
        Log.d("NewDay",isNewDay(getLastFetchTime(context)).toString())
        Log.d("refresh",refresh.toString())
        if (isNewDay(getLastFetchTime(context)) || refresh) {
            Log.d("getHomePageResourceShow", "新加载")
            val page1 =
                eApiService.getHomePageResourceShow(GetHomePageResourceShow(refresh = refresh.toString()))
            val page2 = eApiService.getHomePageResourceShow(
                GetHomePageResourceShow(
                    cursor = "1",
                    refresh = refresh.toString()
                )
            )
            saveLastHomePage(context, 1, page1.data.blocks)
            saveLastHomePage(context, 2, page2.data.blocks)

            Log.d("getHomePageResourceShow", "更新缓存")
            return withContext(Dispatchers.IO) {
                safeApiCall {
                    page1.data.blocks + page2.data.blocks
                }
            }
        } else {
            Log.d("getHomePageResourceShow", "加载缓存")
            val page1 = getLastHomePage(context, 1)
            val page2 = getLastHomePage(context, 2)
            return Resource.Success(page1 + page2)
        }
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
                val json = file.readText()
                Gson().fromJson(
                    json,
                    object : TypeToken<List<HomePageResourceShow.Data.Block>>() {}.type
                )
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
        Log.d("getLastFetchTime", (preferences[LastHomePageTime] ?: 0L).toString())
        return preferences[LastHomePageTime] ?: 0L
    }
}
