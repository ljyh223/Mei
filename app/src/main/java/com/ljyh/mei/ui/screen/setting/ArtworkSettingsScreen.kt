package com.ljyh.mei.ui.screen.setting

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.AlbumArtworkMode
import com.ljyh.mei.constants.AppleArtistPortraitEnabledKey
import com.ljyh.mei.constants.AppleMotionEnglishTitlesOnlyKey
import com.ljyh.mei.constants.OriginalCoverKey
import com.ljyh.mei.constants.PlaylistCoverStyle
import com.ljyh.mei.constants.PlaylistCoverStyleKey
import com.ljyh.mei.constants.PlaylistTrackTableHeaderKey
import com.ljyh.mei.constants.updateDynamicCoverSettings
import com.ljyh.mei.ui.screen.setting.component.DynamicCoverPreference
import com.ljyh.mei.ui.screen.setting.component.EnumListPreference
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.ui.screen.setting.component.SwitchPreference
import com.ljyh.mei.utils.preferences.dataStore
import com.ljyh.mei.utils.preferences.rememberAlbumArtworkPreferences
import com.ljyh.mei.utils.preferences.rememberDynamicCoverSettings
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference
import kotlinx.coroutines.launch

@Composable
internal fun ArtworkSettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val albumArtworkPreferences = rememberAlbumArtworkPreferences()
    val (originalCover, onOriginalCover) = rememberPreference(
        OriginalCoverKey,
        defaultValue = false
    )
    val (appleArtistPortraitEnabled, onAppleArtistPortraitEnabledChange) = rememberPreference(
        AppleArtistPortraitEnabledKey, defaultValue = false,
    )
    val dynamicCoverSettings by rememberDynamicCoverSettings()
    val (appleEnglishTitlesOnly, onAppleEnglishTitlesOnlyChange) = rememberPreference(
        AppleMotionEnglishTitlesOnlyKey,
        defaultValue = false
    )
    val (playlistStyle, onPlaylistStyleChange) = rememberEnumPreference(
        key = PlaylistCoverStyleKey,
        defaultValue = PlaylistCoverStyle.Cover
    )
    val (playlistTrackTableHeader, onPlaylistTrackTableHeaderChange) = rememberPreference(
        key = PlaylistTrackTableHeaderKey,
        defaultValue = false
    )
    AppearanceSettingsPage("内容展示") {
        PreferenceSection("歌单") {

            EnumListPreference(
                title = { Text("歌单封面样式") },
                icon = { Icon(Icons.Rounded.Image, null) },
                selectedValue = playlistStyle,
                onValueSelected = onPlaylistStyleChange,
                valueText = {
                    when (it) {
                        PlaylistCoverStyle.Cover -> "封面"
                        PlaylistCoverStyle.FirstSongImage -> "第一首歌曲封面"
                        PlaylistCoverStyle.Combination -> "组合图片"
                    }
                }
            )
            SwitchPreference(
                title = { Text("歌单开启表头") },
                description = "平板模式下歌单显示表头",
                icon = { Icon(Icons.Rounded.FormatBold, null) },
                checked = playlistTrackTableHeader,
                onCheckedChange = onPlaylistTrackTableHeaderChange
            )
        }
        PreferenceSection("专辑") {
            EnumListPreference(
                title = { Text("专辑封面展示") },
                icon = { Icon(Icons.Rounded.Image, null) },
                selectedValue = albumArtworkPreferences.value.mode,
                onValueSelected = albumArtworkPreferences.onModeChange,
                valueText = { it.label },
            )
            if (albumArtworkPreferences.value.mode == AlbumArtworkMode.Model) {
                EnumListPreference(
                    title = { Text("年代模型样式") },
                    icon = { Icon(Icons.Rounded.Image, null) },
                    selectedValue = albumArtworkPreferences.value.model,
                    onValueSelected = albumArtworkPreferences.onModelChange,
                    valueText = { it.label },
                )
            }
        }
        PreferenceSection("歌手") {
            SwitchPreference(
                title = { Text("使用 Apple Music 歌手人像") },
                description = "按歌手名从 Apple Music 匹配人像及可用艺术字，人像不可用时使用网易云图片",
                icon = { Icon(Icons.Rounded.Person, null) },
                checked = appleArtistPortraitEnabled,
                onCheckedChange = onAppleArtistPortraitEnabledChange,
            )
        }
        PreferenceSection("动态封面") {
            DynamicCoverPreference(
                settings = dynamicCoverSettings,
                englishTitlesOnly = appleEnglishTitlesOnly,
                onEnabledChange = { enabled ->
                    scope.launch {
                        context.dataStore.edit {
                            it.updateDynamicCoverSettings { settings -> settings.copy(enabled = enabled) }
                        }
                    }
                },
                onAppleMusicChange = { enabled ->
                    scope.launch {
                        context.dataStore.edit {
                            it.updateDynamicCoverSettings { settings ->
                                settings.copy(selectedSources = settings.selectedSources.copy(appleMusic = enabled))
                            }
                        }
                    }
                },
                onNeteaseChange = { enabled ->
                    scope.launch {
                        context.dataStore.edit {
                            it.updateDynamicCoverSettings { settings ->
                                settings.copy(selectedSources = settings.selectedSources.copy(netease = enabled))
                            }
                        }
                    }
                },
                onEnglishTitlesOnlyChange = onAppleEnglishTitlesOnlyChange,
            )
        }
        PreferenceSection("图片清晰度") {
            SwitchPreference(
                title = { Text("使用原图加载封面") },
                icon = { Icon(Icons.Rounded.Image, null) },
                checked = originalCover,
                onCheckedChange = onOriginalCover
            )
        }
    }
}
