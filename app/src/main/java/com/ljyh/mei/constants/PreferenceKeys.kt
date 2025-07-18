package com.ljyh.mei.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ljyh.mei.playback.PlayMode
import com.materialkolor.scheme.DynamicScheme

val LastHomePageTime= longPreferencesKey("lastHomePageTime")
val LastHomePageData_1= stringPreferencesKey("lastHomePageData_1")
val LastHomePageData_2= stringPreferencesKey("lastHomePageData_2")
val FirstLaunchKey = booleanPreferencesKey("firstLaunch")
val HideExplicitKey = booleanPreferencesKey("hideExplicit")
val PauseListenHistoryKey = booleanPreferencesKey("pauseListenHistory")
val DarkModeKey = stringPreferencesKey("darkMode")
val PureBlackKey = booleanPreferencesKey("pureBlack")
val PlayerTextAlignmentKey = stringPreferencesKey("playerTextAlignment")
val SliderStyleKey = stringPreferencesKey("sliderStyle")

val UserIdKey = stringPreferencesKey("userId")
val UserNicknameKey = stringPreferencesKey("userNickname")
val UserAvatarUrlKey = stringPreferencesKey("userAvatarUrl")
val UserPhotoKey = stringPreferencesKey("userPhoto")
val ShowLyricsKey = booleanPreferencesKey("showLyrics")


val CookieKey = stringPreferencesKey("cookie")
val NetEaseCloudMusicApiServiceHostKey= stringPreferencesKey("netEaseCloudMusicApiServiceHost")


val CoverStyleKey = stringPreferencesKey("coverStyle")
val IrregularityCoverKey= booleanPreferencesKey("irregularityCover")
val DynamicThemeKey= booleanPreferencesKey("dynamicTheme")

val LyricTextAlignmentKey = stringPreferencesKey("lyricTextAlignment")
val LyricTextSizeKey = stringPreferencesKey("lyricTextSize")
val LyricTextBoldKey = booleanPreferencesKey("lyricTextBold")
val DynamicStreamerKey = booleanPreferencesKey("dynamicStreamer")

val UseQQMusicLyricKey = booleanPreferencesKey("useQQMusicLyric")
val PlayModeKey= intPreferencesKey("playMode")
val DeviceIdKey = stringPreferencesKey("deviceId")
val DebugKey = booleanPreferencesKey("debug")

val DynamicStreamerTypeKey= stringPreferencesKey("dynamicStreamerType")
val PauseSearchHistoryKey = booleanPreferencesKey("pauseSearchHistory")

// 原图封面
val OriginalCoverKey = booleanPreferencesKey("originalCover")
enum class CoverStyle {
    Circle,
    Square
}
enum class LyricTextAlignment {
    Left,
    Center,
    Right
}

enum class DynamicStreamerType {
    FluidBg,
    Image
}


//sealed class LyricTextSize(size:Int) {
//    data object Size18:LyricTextSize(18)
//    data object Size20:LyricTextSize(20)
//    data object Size22:LyricTextSize(22)
//    data object Size24:LyricTextSize(24)
//    data object Size26:LyricTextSize(26)
//    data object Size28:LyricTextSize(28)
//    data object Size30:LyricTextSize(30)
//    init {
//        when(size){
//            18,20,22,24,26,28,30->{}
//            else->throw IllegalArgumentException("LyricTextSize must be 18,20,22,24,26,28,30")
//        }
//    }
//}

enum class LyricTextSize {
    Size18,
    Size20,
    Size22,
    Size24,
    Size26,
    Size28,
    Size30
}