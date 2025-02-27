package com.ljyh.music.data.repository

import android.util.Log
import com.ljyh.music.data.model.HomePageResourceShow
import com.ljyh.music.data.model.weapi.GetHomePageResourceShow
import com.ljyh.music.data.network.Resource
import com.ljyh.music.data.network.api.WeApiService
import com.ljyh.music.data.network.safeApiCall
import com.ljyh.music.utils.CacheFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeRepository(private val weApiService: WeApiService) {

    suspend fun getHomePageResourceShow(
        res:GetHomePageResourceShow
    ): Resource<HomePageResourceShow> {


        if(CacheFile.shouldHomepage()){
            Log.d("getHomePageResourceShow","新加载")
            val rseult=weApiService.getHomePageResourceShow(res)
            CacheFile.updateHomepage(rseult)
            Log.d("getHomePageResourceShow","更新缓存")
            return withContext(Dispatchers.IO) {
                safeApiCall { rseult }
            }
        }else{
            Log.d("getHomePageResourceShow","加载缓存")
            val json=CacheFile.getHomePageResourceShow()
            return Resource.Success(json)
        }
    }
}
