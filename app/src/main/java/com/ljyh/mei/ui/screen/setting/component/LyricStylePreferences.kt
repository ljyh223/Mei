package com.ljyh.mei.ui.screen.setting.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ljyh.mei.constants.AccompanimentLyricTextBoldKey
import com.ljyh.mei.constants.AccompanimentLyricTextSizeKey
import com.ljyh.mei.constants.LyricTextSize
import com.ljyh.mei.constants.NormalLyricTextBoldKey
import com.ljyh.mei.constants.NormalLyricTextSizeKey
import com.ljyh.mei.constants.PhoneticLyricTextBoldKey
import com.ljyh.mei.constants.PhoneticLyricTextSizeKey
import com.ljyh.mei.constants.SeekOnLyricTapKey
import com.ljyh.mei.constants.ShowLyricPhoneticKey
import com.ljyh.mei.constants.ShowLyricTransitionKey
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference

/** Shared by the settings page, sample preview and live player sheet. */
@Composable
internal fun LyricStylePreferences() {
    val (mainSize, onMainSize) = rememberEnumPreference(NormalLyricTextSizeKey, LyricTextSize.Size34)
    val (mainBold, onMainBold) = rememberPreference(NormalLyricTextBoldKey, true)
    val (showPhonetic, onShowPhonetic) = rememberPreference(ShowLyricPhoneticKey, true)
    val (showTranslation, onShowTranslation) = rememberPreference(ShowLyricTransitionKey, true)
    val (seekOnLyricTap, onSeekOnLyricTap) = rememberPreference(SeekOnLyricTapKey, true)
    val (translationSize, onTranslationSize) = rememberEnumPreference(
        AccompanimentLyricTextSizeKey, LyricTextSize.Size20,
    )
    val (phoneticSize, onPhoneticSize) = rememberEnumPreference(
        PhoneticLyricTextSizeKey, LyricTextSize.Size20
    )
    val (phoneticBold, onPhoneticBold) = rememberPreference(
        PhoneticLyricTextBoldKey, true
    )
    val (translationBold, onTranslationBold) = rememberPreference(AccompanimentLyricTextBoldKey, true)
    PreferenceSection("主歌词") {
        SwitchPreference(
            title = { Text("主歌词字体加粗") },
            icon = { Icon(Icons.Rounded.FormatBold, null) },
            checked = mainBold,
            onCheckedChange = onMainBold,
        )
        EnumListPreference(
            title = { Text("主歌词字体大小") },
            icon = { Icon(Icons.Rounded.FormatSize, null) },
            selectedValue = mainSize,
            onValueSelected = onMainSize,
            valueText = { it.text.toString() },
        )
        SwitchPreference(
            title = { Text("点击歌词跳转进度") },
            description = "关闭后点击歌词只显示播放控制",
            icon = { Icon(Icons.Rounded.Lyrics, null) },
            checked = seekOnLyricTap,
            onCheckedChange = onSeekOnLyricTap,
        )
    }
    PreferenceSection("翻译歌词") {
        SwitchPreference(
            title = { Text("显示翻译") },
            description = "歌词提供翻译显示",
            icon = { Icon(Icons.Rounded.Lyrics, null) },
            checked = showTranslation,
            onCheckedChange = onShowTranslation,
        )
        SwitchPreference(
            title = { Text("翻译歌词字体加粗") },
            icon = { Icon(Icons.Rounded.FormatBold, null) },
            checked = translationBold,
            onCheckedChange = onTranslationBold,
        )
        EnumListPreference(
            title = { Text("翻译歌词字体大小") },
            icon = { Icon(Icons.Rounded.FormatSize, null) },
            selectedValue = translationSize,
            onValueSelected = onTranslationSize,
            valueText = { it.text.toString() },
        )
    }

    PreferenceSection("音译歌词") {
        SwitchPreference(
            title = { Text("显示音译") },
            description = "歌词提供发音时显示",
            icon = { Icon(Icons.Rounded.Lyrics, null) },
            checked = showPhonetic,
            onCheckedChange = onShowPhonetic,
        )
        SwitchPreference(
            title = { Text("音译歌词字体加粗") },
            icon = { Icon(Icons.Rounded.FormatBold, null) },
            checked = phoneticBold,
            onCheckedChange = onPhoneticBold,
        )
        EnumListPreference(
            title = { Text("音译歌词字体大小") },
            icon = { Icon(Icons.Rounded.FormatSize, null) },
            selectedValue = phoneticSize,
            onValueSelected = onPhoneticSize,
            valueText = { it.text.toString() },
        )
    }
}
