package com.ljyh.mei.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
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
val MusicQualityKey = stringPreferencesKey("musicQuality")


val CoverStyleKey = stringPreferencesKey("coverStyle")
val DynamicThemeKey = booleanPreferencesKey("dynamicTheme")
val PlayerActionKey = stringPreferencesKey("playerBottomAction")

val NormalLyricTextSizeKey = stringPreferencesKey("lyricTextSize")
val NormalLyricTextBoldKey = booleanPreferencesKey("lyricTextBold")

val AccompanimentLyricTextSizeKey = stringPreferencesKey("accompanimentLyricTextSize")
val AccompanimentLyricTextBoldKey = booleanPreferencesKey("accompanimentLyricTextBold")

val LoopPlaybackKey = booleanPreferencesKey("loopPlayback")
val PreviousPlaybackKey = booleanPreferencesKey("previousPlayback")
val NoAudioSourceKey = booleanPreferencesKey("noAudioSource")
val IsShuffleModeKey = booleanPreferencesKey("shuffleMode")
val RepeatModeKey = intPreferencesKey("repeatMode")

val DeviceIdKey = stringPreferencesKey("deviceId")
val DebugKey = booleanPreferencesKey("debug")
val DevModeKey = booleanPreferencesKey("dev_mode")
val AndroidIdKey = stringPreferencesKey("androidId")

// 原图封面
val OriginalCoverKey = booleanPreferencesKey("originalCover")
val ProgressBarStyleKey = stringPreferencesKey("progressBarStyle")

val MeshFlowSpeedKey = floatPreferencesKey("meshFlowSpeed")
val MeshRenderScaleKey = floatPreferencesKey("meshRenderScale")
val MeshStaticModeKey = booleanPreferencesKey("meshStaticMode")
val MeshPlayingKey = booleanPreferencesKey("meshPlaying")
val MeshLowFreqVolumeKey = floatPreferencesKey("meshLowFreqVolume")
val MeshSubdivisionKey = intPreferencesKey("meshSubdivision")

val PlayerStyleKey = stringPreferencesKey("playerStyle")
val PlaylistCoverStyleKey = stringPreferencesKey("playlistCoverStyle")
val PlaylistTrackTableHeaderKey = booleanPreferencesKey("playlistTrackTableHeader")
val TabletAnimationStyleKey = stringPreferencesKey("tabletAnimationStyle")

val AiTriggerModeKey = stringPreferencesKey("ai_trigger_mode")
val AiBaseUrlKey = stringPreferencesKey("ai_base_url")
val AiApiKeyKey = stringPreferencesKey("ai_api_key")
val AiModelKey = stringPreferencesKey("ai_model")
val DownloadPathKey = stringPreferencesKey("downloadPath")
val DownloadQualityKey = stringPreferencesKey("downloadQuality")
val QqTimeoutKey = stringPreferencesKey("qq_timeout")

enum class AiTriggerMode {
    Off,
    OnMissing,
    Always
}

enum class QqTimeout(val seconds: Int, val label: String) {
    Sec3(3, "3秒"),
    Sec5(5, "5秒"),
    Sec8(8, "8秒"),
    Sec10(10, "10秒"),
    Sec15(15, "15秒")
}

enum class PlayerStyle {
    AppleMusic,
    Classic
}


enum class PlaylistCoverStyle {
    Cover,
    FirstSongImage,
    Combination
}
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
enum class MusicQuality(val text: String, val explanation:String) {
    STANDARD("standard", "标准"),
    EXHIGH("exhigh","极高"),
    LOSSLESS("lossless","无损"),
    HIRES("hires","Hi-Res"),
    JYEFFECT("jyeffect", "高清环绕声"),
    SKY("sky", "沉浸环绕声"),
    JYMASTER("jymaster", "超清母带")
}


enum class DownloadQuality(val text: String, val label: String, val description: String) {
    STANDARD("standard", "标准", "标准音质, 文件较小"),
    EXHIGH("exhigh", "极高", "极高音质, 推荐"),
    LOSSLESS("lossless", "无损", "CD级无损音质"),
    HIRES("hires", "Hi-Res", "高解析度无损"),
    JYMASTER("jymaster", "超清母带", "母带级音质");

    fun toMusicQuality(): MusicQuality = when (this) {
        STANDARD -> MusicQuality.STANDARD
        EXHIGH -> MusicQuality.EXHIGH
        LOSSLESS -> MusicQuality.LOSSLESS
        HIRES -> MusicQuality.HIRES
        JYMASTER -> MusicQuality.JYMASTER
    }
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

enum class ProgressBarStyle(val label: String) {
    WAVE("动态波浪"),       // 原来的波浪样式
    LINEAR("正常样式")   // 新写的直线样式
}

enum class TabletAnimationStyle(val label: String) {
    SLIDE("水平滑动"),
    CROSSFADE("渐变"),
    ZOOM("缩放"),
    FLIP_3D("3D翻转")
}