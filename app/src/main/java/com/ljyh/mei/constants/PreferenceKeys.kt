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
val SearchHistoryKey = stringPreferencesKey("searchHistory")
val DesktopLyricsEnabledKey = booleanPreferencesKey("desktopLyricsEnabled")
val DesktopLyricsLockedKey = booleanPreferencesKey("desktopLyricsLocked")
val DesktopLyricsTextColorKey = stringPreferencesKey("desktopLyricsTextColor")
val DesktopLyricsTranslationColorKey = stringPreferencesKey("desktopLyricsTranslationColor")
val DesktopLyricsFontSizeKey = intPreferencesKey("desktopLyricsFontSize")
val DesktopLyricsTranslationFontSizeKey = intPreferencesKey("desktopLyricsTranslationFontSize")
val DesktopLyricsControlsHideDelayKey = intPreferencesKey("desktopLyricsControlsHideDelay")
val DesktopLyricsXKey = intPreferencesKey("desktopLyricsX")
val DesktopLyricsYKey = intPreferencesKey("desktopLyricsY")

const val DefaultDesktopLyricsTextColor = "#FFFFFFFF"
const val DefaultDesktopLyricsTranslationColor = "#FFCECED3"
const val DefaultDesktopLyricsFontSize = 18
const val DefaultDesktopLyricsTranslationFontSize = 13
const val DefaultDesktopLyricsControlsHideDelay = 12


val CookieKey = stringPreferencesKey("cookie")
val QqCookieKey = stringPreferencesKey("qq_music_cookie")
val MusicQualityKey = stringPreferencesKey("musicQuality")
val ImageCacheLimitMbKey = intPreferencesKey("imageCacheLimitMb")
val MusicCacheLimitMbKey = intPreferencesKey("musicCacheLimitMb")


val CoverStyleKey = stringPreferencesKey("coverStyle")
val DynamicThemeKey = booleanPreferencesKey("dynamicTheme")
val PlayerActionKey = stringPreferencesKey("playerBottomAction")

val NormalLyricTextSizeKey = stringPreferencesKey("lyricTextSize")
val NormalLyricTextBoldKey = booleanPreferencesKey("lyricTextBold")

val AccompanimentLyricTextSizeKey = stringPreferencesKey("accompanimentLyricTextSize")
val AccompanimentLyricTextBoldKey = booleanPreferencesKey("accompanimentLyricTextBold")

val LoopPlaybackKey = booleanPreferencesKey("loopPlayback")
val KeepPlayerScreenOnKey = booleanPreferencesKey("keepPlayerScreenOn")
val NoAudioSourceKey = booleanPreferencesKey("noAudioSource")
val SmartTransitionEnabledKey = booleanPreferencesKey("smartTransition.enabled")
val SmartTransitionModeKey = stringPreferencesKey("smartTransition.mode")
val SmartTransitionDurationKey = intPreferencesKey("smartTransition.durationSeconds")
val IsShuffleModeKey = booleanPreferencesKey("shuffleMode")
val RepeatModeKey = intPreferencesKey("repeatMode")
val LastPlaybackQueueKey = stringPreferencesKey("lastPlaybackQueue")

// Playback effects. Band levels are stored as millibels, separated by commas.
val EqualizerEnabledKey = booleanPreferencesKey("equalizerEnabled")
val EqualizerPresetKey = stringPreferencesKey("equalizerPreset")
val EqualizerBandLevelsKey = stringPreferencesKey("equalizerBandLevels")
val LoudnessEnhancerGainKey = intPreferencesKey("loudnessEnhancerGain")
val ParametricEqualizerProfileKey = stringPreferencesKey("parametricEqualizerProfile")
val UserEqualizerPresetsKey = stringPreferencesKey("userEqualizerPresets")

val DeviceIdKey = stringPreferencesKey("deviceId")
val DebugKey = booleanPreferencesKey("debug")
val DevModeKey = booleanPreferencesKey("dev_mode")
val AndroidIdKey = stringPreferencesKey("androidId")

// 原图封面
val OriginalCoverKey = booleanPreferencesKey("originalCover")
val DynamicCoverKey = booleanPreferencesKey("dynamicCover")
val AppleMotionEnglishTitlesOnlyKey = booleanPreferencesKey("appleMotionEnglishTitlesOnly")
val AppleMusicAlbumMatchCacheKey = stringPreferencesKey("appleMusicAlbumMatchCache")
val AppleMusicWebTokenKey = stringPreferencesKey("appleMusicWebToken")
val AppleMusicWebTokenRefreshAtKey = longPreferencesKey("appleMusicWebTokenRefreshAt")
val ProgressBarStyleKey = stringPreferencesKey("progressBarStyle")

val MeshFlowSpeedKey = floatPreferencesKey("meshFlowSpeed")
val MeshRenderScaleKey = floatPreferencesKey("meshRenderScale")
val MeshStaticModeKey = booleanPreferencesKey("meshStaticMode")
val MeshPlayingKey = booleanPreferencesKey("meshPlaying")
val MeshSubdivisionKey = intPreferencesKey("meshSubdivision")

val PlaylistCoverStyleKey = stringPreferencesKey("playlistCoverStyle")
val PlaylistTrackTableHeaderKey = booleanPreferencesKey("playlistTrackTableHeader")
val TabletAnimationStyleKey = stringPreferencesKey("tabletAnimationStyle")

val DownloadPathKey = stringPreferencesKey("downloadPath")
val DownloadQualityKey = stringPreferencesKey("downloadQuality")
val EmbedOriginalTtmlKey = booleanPreferencesKey("embedOriginalTtml")
val QqTimeoutKey = stringPreferencesKey("qq_timeout")
val TtmlLyricsBaseUrlKey = stringPreferencesKey("ttmlLyricsBaseUrl")

const val DefaultTtmlLyricsBaseUrl = "https://amlldb.bikonoo.com"

enum class QqTimeout(val seconds: Int, val label: String) {
    Sec3(3, "3秒"),
    Sec5(5, "5秒"),
    Sec8(8, "8秒"),
    Sec10(10, "10秒"),
    Sec15(15, "15秒")
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
enum class SmartTransitionMode { Smart, Fixed }

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
    Size32(32),
    Size34(34),
    Size36(36),
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
