package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AlignHorizontalLeft
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Kitesurfing
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
import com.ljyh.mei.constants.CoverStyle
import com.ljyh.mei.constants.CoverStyleKey
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.constants.DynamicStreamerKey
import com.ljyh.mei.constants.DynamicStreamerType
import com.ljyh.mei.constants.DynamicStreamerTypeKey
import com.ljyh.mei.constants.DynamicThemeKey
import com.ljyh.mei.constants.IrregularityCoverKey
import com.ljyh.mei.constants.LyricTextAlignment
import com.ljyh.mei.constants.LyricTextAlignmentKey
import com.ljyh.mei.constants.LyricTextBoldKey
import com.ljyh.mei.constants.LyricTextSize
import com.ljyh.mei.constants.LyricTextSizeKey
import com.ljyh.mei.constants.OriginalCoverKey
import com.ljyh.mei.ui.component.EnumListPreference
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.PreferenceGroupTitle
import com.ljyh.mei.ui.component.SwitchPreference
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.utils.rememberEnumPreference
import com.ljyh.mei.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val navController = LocalNavController.current


    val (coverStyle, onCoverStyleChange) = rememberEnumPreference(
        CoverStyleKey,
        defaultValue = CoverStyle.Square
    )

    val (lyricTextAlignment, onLyricTextAlignmentChange) = rememberEnumPreference(
        LyricTextAlignmentKey,
        defaultValue = LyricTextAlignment.Left
    )
    val (lyricTextSize, onLyricTextSizeChange) = rememberEnumPreference(
        LyricTextSizeKey,
        defaultValue = LyricTextSize.Size20
    )
    val (lyricTextBold, onLyricTextBoldChange) = rememberPreference(
        LyricTextBoldKey,
        defaultValue = true
    )

    val (originalCover, onOriginalCover) = rememberPreference(
        OriginalCoverKey,
        defaultValue = false
    )

    val (debug, onDebug) = rememberPreference(
        DebugKey, defaultValue = false
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



            PreferenceGroupTitle(
                title = "PLAYER"
            )

            // 原图封面
            SwitchPreference(
                title = { Text("使用原图加载封面") },
                icon = { Icon(Icons.Rounded.Image, null) },
                checked = originalCover,
                onCheckedChange = onOriginalCover
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
                icon = { Icon(Icons.Rounded.FormatBold, null) },
                checked = lyricTextBold,
                onCheckedChange = onLyricTextBoldChange
            )

            EnumListPreference(
                title = { Text("歌词文本对齐") },
                icon = { Icon(Icons.AutoMirrored.Rounded.AlignHorizontalLeft, null) },
                selectedValue = lyricTextAlignment,
                onValueSelected = onLyricTextAlignmentChange,
                valueText = {
                    it.name
                }
            )

            EnumListPreference(
                title = { Text("歌词字体大小") },
                icon = { Icon(Icons.Rounded.FormatSize, null) },
                selectedValue = lyricTextSize,
                onValueSelected = onLyricTextSizeChange,
                valueText = { it.name}
            )


            PreferenceGroupTitle(
                title = "DEBUG"
            )

            SwitchPreference(
                title = { Text("Debug") },
                description = "player debug",
                icon = { Icon(Icons.Rounded.Kitesurfing, null) },
                checked = debug,
                onCheckedChange = onDebug
            )


        }
    }


}
