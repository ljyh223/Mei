package com.ljyh.mei.utils

import com.google.gson.Gson
import com.ljyh.mei.AppContext
import com.ljyh.mei.data.model.eapi.HomePageResourceShow
import com.ljyh.mei.utils.DateUtils.isWithinSameDayWindow
import java.io.File
import java.io.IOException

object CacheFile {
    private const val LastHomePage = "last_homepage"
    private fun cacheJsonToFile(fileName: String, json: String) {
        try {
            val file = File(AppContext.instance.cacheDir, fileName)
            file.writeText(json)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getJsonFromFile(fileName: String): String? {
        return try {
            val file = File(AppContext.instance.cacheDir, fileName)
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }



    fun getHomePageResourceShow(): HomePageResourceShow {
        val radar=getJsonFromFile(LastHomePage)
        return Gson().fromJson(radar, HomePageResourceShow::class.java)

    }

    fun isNewDay(lastRequestTime:Long): Boolean {
        return isWithinSameDayWindow(lastRequestTime)
    }






}