package com.ljyh.music.ui.local

import androidx.compose.runtime.compositionLocalOf
import com.ljyh.music.data.model.UserData

val LocalUserData = compositionLocalOf<UserData> {
    UserData.VISITOR
}