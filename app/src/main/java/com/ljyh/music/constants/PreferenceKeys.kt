package com.ljyh.music.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

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


val UseQQMusicLyricKey = booleanPreferencesKey("useQQMusicLyric")

enum class CoverStyle {
    Circle,
    Square
}
enum class LyricTextAlignment {
    Left,
    Center,
    Right
}