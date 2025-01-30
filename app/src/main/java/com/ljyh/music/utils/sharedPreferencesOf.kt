package com.ljyh.music.utils

import android.content.Context
import android.content.SharedPreferences
import com.ljyh.music.AppContext

fun sharedPreferencesOf(
    name: String
): SharedPreferences = AppContext.instance.getSharedPreferences(name, Context.MODE_PRIVATE)