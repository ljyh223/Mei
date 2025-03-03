package com.ljyh.music.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AlignHorizontalLeft
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AlignHorizontalLeft
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material.icons.rounded.TextRotationAngledown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ljyh.music.constants.CoverStyle
import com.ljyh.music.constants.CoverStyleKey
import com.ljyh.music.constants.DynamicThemeKey
import com.ljyh.music.constants.IrregularityCoverKey
import com.ljyh.music.constants.LyricTextAlignment
import com.ljyh.music.constants.LyricTextAlignmentKey
import com.ljyh.music.constants.LyricTextBoldKey
import com.ljyh.music.constants.LyricTextSize
import com.ljyh.music.constants.LyricTextSizeKey
import com.ljyh.music.constants.UseQQMusicLyricKey
import com.ljyh.music.ui.component.EditTextPreference
import com.ljyh.music.ui.component.EnumListPreference
import com.ljyh.music.ui.component.IconButton
import com.ljyh.music.ui.component.PreferenceGroupTitle
import com.ljyh.music.ui.component.SwitchPreference
import com.ljyh.music.ui.local.LocalNavController
import com.ljyh.music.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.music.ui.screen.backToMain
import com.ljyh.music.utils.rememberEnumPreference
import com.ljyh.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val navController = LocalNavController.current

    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(
        DynamicThemeKey,
        defaultValue = true
    )
    val (coverStyle, onCoverStyleChange) = rememberEnumPreference(
        CoverStyleKey,
        defaultValue = CoverStyle.Square
    )
    val (irregularityCover, onIrregularityCover) = rememberPreference(
        IrregularityCoverKey,
        defaultValue = false
    )
    val (lyricTextAlignment, onLyricTextAlignmentChange) = rememberEnumPreference(
        LyricTextAlignmentKey,
        defaultValue = LyricTextAlignment.Left
    )
    val (lyricTextSize, onLyricTextSizeChange) = rememberEnumPreference(
        LyricTextSizeKey,
        defaultValue = LyricTextSize.Size24
    )
    val (lyricTextBold, onLyricTextBoldChange) = rememberPreference(
        LyricTextBoldKey,
        defaultValue = true
    )

    val (dynamicStreamer,onDynamicStreamerChange) = rememberPreference(
        DynamicThemeKey, defaultValue = true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("内容设置") },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
        ) {

            PreferenceGroupTitle(
                title = "THEME"
            )
            SwitchPreference(
                title = { Text("启用动态主题") },
                icon = { Icon(Icons.Rounded.Palette, null) },
                checked = dynamicTheme,
                onCheckedChange = onDynamicStreamerChange
            )

            PreferenceGroupTitle(
                title = "PLAYER"
            )

            SwitchPreference(
                title = { Text("启用动态背景") },
                icon = { Icon(Icons.Rounded.TextRotationAngledown, null) },
                checked = dynamicStreamer,
                onCheckedChange = onDynamicThemeChange
            )
            SwitchPreference(
                title = { Text("允许不规则封面") },
                icon = { Icon(Icons.Rounded.Stairs, null) },
                checked = irregularityCover,
                onCheckedChange = onIrregularityCover
            )
            EnumListPreference(
                title = { Text("歌曲封面样式") },
                icon = { Icon(Icons.Rounded.Image, null) },
                selectedValue = coverStyle,
                onValueSelected = onCoverStyleChange,
                valueText = {
                    when (it) {
                        CoverStyle.Circle -> "圆形"
                        CoverStyle.Square -> "方形"
                    }
                }
            )


            PreferenceGroupTitle(
                title = "LYRIC"
            )
            SwitchPreference(
                title = { Text("歌词字体加粗") },
                icon = { Icon(Icons.Rounded.FormatBold, null)},
                checked = lyricTextBold,
                onCheckedChange = onLyricTextBoldChange
            )

            EnumListPreference(
                title = { Text("歌词文本对齐") },
                icon = { Icon(Icons.AutoMirrored.Rounded.AlignHorizontalLeft, null) },
                selectedValue = lyricTextAlignment,
                onValueSelected = onLyricTextAlignmentChange,
                valueText = {
                    when (it) {
                        LyricTextAlignment.Left -> "居左"
                        LyricTextAlignment.Center -> "居中"
                        LyricTextAlignment.Right -> "居右"
                    }
                }
            )

            EnumListPreference(
                title = { Text("歌词字体大小") },
                icon = { Icon(Icons.Rounded.FormatSize, null) },
                selectedValue = lyricTextSize,
                onValueSelected = onLyricTextSizeChange,
                valueText = {
                    when (it) {
                        LyricTextSize.Size18 -> "18"
                        LyricTextSize.Size20 -> "20"
                        LyricTextSize.Size22 -> "22"
                        LyricTextSize.Size24 -> "24"
                        LyricTextSize.Size26 -> "26"
                        LyricTextSize.Size28 -> "28"
                        LyricTextSize.Size30 -> "30"
                    }
                }
            )

        }
    }


}
