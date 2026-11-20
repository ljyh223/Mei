package com.ljyh.mei.ui.screen.setting

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ljyh.mei.constants.PageTransitionSpeed
import com.ljyh.mei.constants.PageTransitionSpeedKey
import com.ljyh.mei.constants.PageTransitionStyle
import com.ljyh.mei.constants.PageTransitionStyleKey
import com.ljyh.mei.constants.TabletAnimationStyle
import com.ljyh.mei.constants.TabletAnimationStyleKey
import com.ljyh.mei.ui.screen.setting.component.EnumListPreference
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.utils.preferences.rememberEnumPreference

@Composable
internal fun AnimationSettingsScreen() {
    val (tabletAnimStyle, onTabletAnimStyleChange) = rememberEnumPreference(
        key = TabletAnimationStyleKey,
        defaultValue = TabletAnimationStyle.FLIP_3D
    )

    val (pageTransitionStyle, onPageTransitionStyleChange) = rememberEnumPreference(
        PageTransitionStyleKey, PageTransitionStyle.Slide,
    )
    val (pageTransitionSpeed, onPageTransitionSpeedChange) = rememberEnumPreference(
        PageTransitionSpeedKey, PageTransitionSpeed.Standard,
    )
    AppearanceSettingsPage("动画与动效") {
        PreferenceSection("页面动画") {
            EnumListPreference(
                title = { Text("切换样式") },
                icon = { Icon(Icons.Rounded.SwapHoriz, null) },
                selectedValue = pageTransitionStyle,
                onValueSelected = onPageTransitionStyleChange,
                valueText = { it.label },
            )
            EnumListPreference(
                title = { Text("动画速度") },
                icon = { Icon(Icons.Rounded.Speed, null) },
                selectedValue = pageTransitionSpeed,
                onValueSelected = onPageTransitionSpeedChange,
                valueText = { it.label },
                isEnabled = pageTransitionStyle != PageTransitionStyle.None,
            )
        }
        PreferenceSection("播放器") {
            EnumListPreference(
                title = { Text("播放器面板切换动画") },
                icon = { Icon(Icons.Rounded.SwapHoriz, null) },
                selectedValue = tabletAnimStyle,
                onValueSelected = onTabletAnimStyleChange,
                valueText = { it.label }
            )
        }
    }
}
