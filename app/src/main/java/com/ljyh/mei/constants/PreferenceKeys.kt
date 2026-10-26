package com.ljyh.mei.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
val HidePlayerTitleBracketsKey = booleanPreferencesKey("hidePlayerTitleBrackets")
val AlwaysExpandMiniPlayerKey = booleanPreferencesKey("appearance.alwaysExpandMiniPlayer")
val SliderStyleKey = stringPreferencesKey("sliderStyle")

val UserIdKey = stringPreferencesKey("userId")
val UserNicknameKey = stringPreferencesKey("userNickname")
val UserAvatarUrlKey = stringPreferencesKey("userAvatarUrl")
val UserPhotoKey = stringPreferencesKey("userPhoto")
val ShowLyricsKey = booleanPreferencesKey("showLyrics")
val SearchHistoryKey = stringPreferencesKey("searchHistory")
val RejectedQqLyricSongIdsKey = stringSetPreferencesKey("rejectedQqLyricSongIds")
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
val UsbExclusivePlaybackKey = booleanPreferencesKey("usbExclusivePlayback")
/** 临时停用解灰兜底，仅用于单独验证自定义音源。 */
val LxOnlyPlaybackKey = booleanPreferencesKey("lxOnlyPlayback")
val ImageCacheLimitMbKey = intPreferencesKey("imageCacheLimitMb")
val MusicCacheLimitMbKey = intPreferencesKey("musicCacheLimitMb")


val CoverStyleKey = stringPreferencesKey("coverStyle")
val DynamicThemeKey = booleanPreferencesKey("dynamicTheme")
val LiquidGlassEnabledKey = booleanPreferencesKey("appearance.liquidGlass")
val GlassOpacityKey = floatPreferencesKey("appearance.glass.opacity")
val GlassBlurKey = floatPreferencesKey("appearance.glass.blur")
val GlassRefractionKey = floatPreferencesKey("appearance.glass.refraction")
val GlassHighlightKey = floatPreferencesKey("appearance.glass.highlight")
val GlassSaturationKey = floatPreferencesKey("appearance.glass.saturation")
val GlassStrongContentKey = booleanPreferencesKey("appearance.glass.strongContent")
val GlassReduceTransparencyKey = booleanPreferencesKey("appearance.glass.reduceTransparency")
val GlassMaterialVersionKey = intPreferencesKey("appearance.glass.materialVersion")
val ThemeColorKey = stringPreferencesKey("themeColor")
const val DefaultThemeColor = "#FF0061A4"
val PlayerActionKey = stringPreferencesKey("playerBottomAction")
val MoreActionOrderKey = stringPreferencesKey("moreActionOrder")

val NormalLyricTextSizeKey = stringPreferencesKey("lyricTextSize")
val NormalLyricTextBoldKey = booleanPreferencesKey("lyricTextBold")
val ShowLyricPhoneticKey = booleanPreferencesKey("showLyricPhonetic")
val SeekOnLyricTapKey = booleanPreferencesKey("seekOnLyricTap")

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
val ReverbProfileKey = stringPreferencesKey("reverbProfile")
val OutputLimiterEnabledKey = booleanPreferencesKey("outputLimiterEnabled")
val AutoEqEnabledKey = booleanPreferencesKey("autoEqEnabled")
val AutoEqIntensityKey = intPreferencesKey("autoEqIntensity")
val UserEqualizerPresetsKey = stringPreferencesKey("userEqualizerPresets")

val DeviceIdKey = stringPreferencesKey("deviceId")
val DebugKey = booleanPreferencesKey("debug")
val DevModeKey = booleanPreferencesKey("dev_mode")
val AndroidIdKey = stringPreferencesKey("androidId")

// 原图封面
val OriginalCoverKey = booleanPreferencesKey("originalCover")
val DynamicCoverKey = booleanPreferencesKey("dynamicCover")
val DynamicCoverEnabledKey = booleanPreferencesKey("dynamicCover.enabled")
val AppleDynamicCoverKey = booleanPreferencesKey("dynamicCover.appleMusic")
val NeteaseDynamicCoverKey = booleanPreferencesKey("dynamicCover.netease")
val CoverBackgroundEnabledKey = booleanPreferencesKey("coverBackground.enabled")
val CoverBackgroundToleranceKey = floatPreferencesKey("coverBackground.tolerance")
val CoverBackgroundConfidenceKey = floatPreferencesKey("coverBackground.confidence")
val CoverSolidEdgeMatchKey = floatPreferencesKey("coverBackground.solidEdgeMatch")
val CoverSoftEdgeMatchKey = floatPreferencesKey("coverBackground.softEdgeMatch")
val CoverSingleEdgeMatchKey = floatPreferencesKey("coverBackground.singleEdgeMatch")
val CoverBackgroundPresetKey = stringPreferencesKey("coverBackground.preset")
val CoverMaximumTextureKey = floatPreferencesKey("coverBackground.maximumBandTexture")
val CoverMaximumGradientKey = floatPreferencesKey("coverBackground.maximumBandGradient")
val CoverMinimumBandShareKey = floatPreferencesKey("coverBackground.minimumBandShare")
val CoverMinimumWholeShareKey = floatPreferencesKey("coverBackground.minimumWholeShare")
val CoverMaximumSolidGapKey = floatPreferencesKey("coverBackground.maximumSolidGap")
val CoverMaximumSoftGapKey = floatPreferencesKey("coverBackground.maximumSoftGap")
val CoverSolidSideSlackKey = floatPreferencesKey("coverBackground.solidSideSlack")
val CoverSingleEdgeStrongMatchKey = floatPreferencesKey("coverBackground.singleEdgeStrongMatch")
val CoverThreeSideMinimumMatchKey = floatPreferencesKey("coverBackground.threeSideMinimumMatch")
val CoverAllowSingleOccupiedSideKey = booleanPreferencesKey("coverBackground.allowSingleOccupiedSide")
val CoverAllowThreeStrongSidesKey = booleanPreferencesKey("coverBackground.allowThreeStrongSides")
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
val PageTransitionStyleKey = stringPreferencesKey("pageTransitionStyle")
val PageTransitionSpeedKey = stringPreferencesKey("pageTransitionSpeed")

val DownloadPathKey = stringPreferencesKey("downloadPath")
val DownloadQualityKey = stringPreferencesKey("downloadQuality")
val EmbedOriginalTtmlKey = booleanPreferencesKey("embedOriginalTtml")
val QqTimeoutKey = stringPreferencesKey("qq_timeout")
val QqLyricApiKey = stringPreferencesKey("qq_lyric_api")
val TtmlLyricsBaseUrlKey = stringPreferencesKey("ttmlLyricsBaseUrl")

const val DefaultTtmlLyricsBaseUrl = "https://amlldb.bikonoo.com"

/**
 * QQ 搜索与歌词走哪个客户端接口。
 *
 * 项目里目前存在两套 QQ 歌词实现，互不影响：
 *  - [Android]  = 新：Android 客户端协议（musics.fcg + sign/mask），见 [com.ljyh.mei.data.network.qqandroid.QqAndroidClient]
 *  - [Legacy]   = 旧：musicu.fcg + cookie，见 [com.ljyh.unblockneteasemusic.qq.QqMusicProvider]
 */
enum class QqLyricApi(val label: String) {
    Auto("自动（Android 优先，失败回退旧版）"),
    Android("Android 客户端协议"),
    Legacy("旧版 musicu.fcg")
}

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


enum class PageTransitionStyle(val label: String) {
    Slide("横向推进"),
    Fade("淡入淡出"),
    None("无动画"),
}

enum class PageTransitionSpeed(val label: String, val slideMillis: Int, val fadeMillis: Int) {
    Fast("快速", 200, 120),
    Standard("标准", 300, 180),
    Relaxed("舒缓", 400, 240),
}
