package com.ljyh.mei.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ljyh.mei.playback.PlayMode
import com.materialkolor.scheme.DynamicScheme

val LastHomePageTime = longPreferencesKey("lastHomePageTime")
val LastHomePageData_1 = stringPreferencesKey("lastHomePageData_1")
val LastHomePageData_2 = stringPreferencesKey("lastHomePageData_2")
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
val NetEaseCloudMusicApiServiceHostKey = stringPreferencesKey("netEaseCloudMusicApiServiceHost")


val CoverStyleKey = stringPreferencesKey("coverStyle")
val IrregularityCoverKey = booleanPreferencesKey("irregularityCover")
val DynamicThemeKey = booleanPreferencesKey("dynamicTheme")


val NormalLyricTextSizeKey = stringPreferencesKey("lyricTextSize")
val NormalLyricTextBoldKey = booleanPreferencesKey("lyricTextBold")

val AccompanimentLyricTextSizeKey = stringPreferencesKey("accompanimentLyricTextSize")
val AccompanimentLyricTextBoldKey = booleanPreferencesKey("accompanimentLyricTextBold")

val UseQQMusicLyricKey = booleanPreferencesKey("useQQMusicLyric")
val MusicQualityKey = stringPreferencesKey("musicQuality")
val LoopPlaybackKey = booleanPreferencesKey("loopPlayback")
val PlayModeKey = intPreferencesKey("playMode")
val DeviceIdKey = stringPreferencesKey("deviceId")
val DebugKey = booleanPreferencesKey("debug")


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


// standard, exhigh, lossless, hires, jyeffect(高清环绕声), sky(沉浸环绕声), jymaster(超清母带) 进行音质判断
enum class MusicQuality(val text: String) {
    STANDARD("standard"),
    EXHIGH("exhigh"),
    LOSSLESS("lossless"),
    HIRES("hires"),
    JYEFFECT("jyeffect"),
    SKY("sky"),
    JYMASTER("jymaster")
}


enum class LyricTextSize(val text: Int) {
    Size18(18),
    Size20(20),
    Size22(22),
    Size24(24),
    Size26(26),
    Size28(28),
    Size30(30),
    Size32(32)
}

