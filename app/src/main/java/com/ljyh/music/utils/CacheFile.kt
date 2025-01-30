package com.ljyh.music.utils

import com.google.gson.Gson
import com.ljyh.music.AppContext
import com.ljyh.music.data.model.HomePageResourceShow
import com.ljyh.music.utils.DateUtils.isWithinSameDayWindow
import java.io.File
import java.io.IOException

object CacheFile {
    private const val PREF_NAME = "app"
    private val sharedPreferences = sharedPreferencesOf(PREF_NAME)
    private const val lastHomePage = "last_homepage"
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
        val radar=getJsonFromFile(lastHomePage)
        return Gson().fromJson(radar, HomePageResourceShow::class.java)

    }




    private fun updateLast(s: String) {
        val editor = sharedPreferences.edit()
        editor.putLong(s, System.currentTimeMillis())
        editor.apply()
    }

    fun updateHomepage(radar: HomePageResourceShow) {
        cacheJsonToFile(lastHomePage, Gson().toJson(radar))
        updateLast(lastHomePage)
    }



    fun shouldHomepage(): Boolean {
        return shouldLast(lastHomePage)
    }


    private fun shouldLast(s: String): Boolean {
        val lastRequestTime = sharedPreferences.getLong(s, 0)
        return isWithinSameDayWindow(lastRequestTime)
    }

}