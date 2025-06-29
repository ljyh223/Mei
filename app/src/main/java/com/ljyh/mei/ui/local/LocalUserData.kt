package com.ljyh.mei.ui.local

import androidx.compose.runtime.compositionLocalOf
import com.ljyh.mei.data.model.UserData

val LocalUserData = compositionLocalOf<UserData> {
    UserData.VISITOR
}