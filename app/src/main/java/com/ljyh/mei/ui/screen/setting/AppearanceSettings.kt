package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Kitesurfing
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ljyh.mei.constants.AccompanimentLyricTextBoldKey
import com.ljyh.mei.constants.AccompanimentLyricTextSizeKey
import com.ljyh.mei.constants.MeshFlowSpeedKey
import com.ljyh.mei.constants.MeshLowFreqVolumeKey
import com.ljyh.mei.constants.MeshPlayingKey
import com.ljyh.mei.constants.MeshRenderScaleKey
import com.ljyh.mei.constants.MeshStaticModeKey
import com.ljyh.mei.constants.MeshSubdivisionKey
import com.ljyh.mei.constants.CoverStyle
import com.ljyh.mei.constants.CoverStyleKey
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.constants.DynamicThemeKey
import com.ljyh.mei.constants.LyricTextSize
import com.ljyh.mei.constants.NormalLyricTextBoldKey
import com.ljyh.mei.constants.NormalLyricTextSizeKey
import com.ljyh.mei.constants.OriginalCoverKey
import com.ljyh.mei.constants.PlayerStyle
import com.ljyh.mei.constants.PlayerStyleKey
import com.ljyh.mei.constants.PlaylistCoverStyle
import com.ljyh.mei.constants.PlaylistCoverStyleKey
import com.ljyh.mei.constants.PlaylistTrackTableHeaderKey
import com.ljyh.mei.constants.ProgressBarStyle
import com.ljyh.mei.constants.ProgressBarStyleKey
import com.ljyh.mei.ui.component.EnumListPreference
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.PreferenceGroupTitle
import com.ljyh.mei.ui.component.SliderPreference
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

    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(
        DynamicThemeKey,
        defaultValue = true
    )


    val (coverStyle, onCoverStyleChange) = rememberEnumPreference(
        CoverStyleKey,
        defaultValue = CoverStyle.Square
    )


    val (normalLyricTextSize, onNormalLyricTextSizeChange) = rememberEnumPreference(
        NormalLyricTextSizeKey,
        defaultValue = LyricTextSize.Size30
    )
    val (normalLyricTextBold, onNormalLyricTextBoldChange) = rememberPreference(
        NormalLyricTextBoldKey,
        defaultValue = true
    )

    val (accompanimentLyricTextSize, onAccompanimentLyricTextSizeChange) = rememberEnumPreference(
        AccompanimentLyricTextSizeKey,
        defaultValue = LyricTextSize.Size20
    )

    val (accompanimentLyricTextBold, onAccompanimentLyricTextBoldChange) = rememberPreference(
        AccompanimentLyricTextBoldKey,
        defaultValue = true
    )



    val (originalCover, onOriginalCover) = rememberPreference(
        OriginalCoverKey,
        defaultValue = false
    )

    val (debug, onDebug) = rememberPreference(
        DebugKey, defaultValue = false
    )
    val (progressBarStyle, onProgressBarStyleChange) = rememberEnumPreference(
        key = ProgressBarStyleKey,
        defaultValue = ProgressBarStyle.WAVE // 默认值设为你喜欢的
    )

    val (playerStyle, onPlayerStyleChange) = rememberEnumPreference(
        key = PlayerStyleKey,
        defaultValue = PlayerStyle.AppleMusic
    )

    val (playlistStyle, onPlaylistStyleChange) = rememberEnumPreference(
        key = PlaylistCoverStyleKey,
        defaultValue = PlaylistCoverStyle.Combination
    )

    val (playlistTrackTableHeader, onPlaylistTrackTableHeaderChange) = rememberPreference(
        key = PlaylistTrackTableHeaderKey,
        defaultValue = false
    )
    val (meshFlowSpeed, onMeshFlowSpeedChange) = rememberPreference(
        MeshFlowSpeedKey, defaultValue = 0.25f
    )
    val (meshRenderScale, onMeshRenderScaleChange) = rememberPreference(
        MeshRenderScaleKey, defaultValue = 0.75f
    )
    val (meshStaticMode, onMeshStaticModeChange) = rememberPreference(
        MeshStaticModeKey, defaultValue = false
    )
    val (meshPlaying, onMeshPlayingChange) = rememberPreference(
        MeshPlayingKey, defaultValue = true
    )
    val (meshLowFreqVolume, onMeshLowFreqVolumeChange) = rememberPreference(
        MeshLowFreqVolumeKey, defaultValue = 0.1f
    )
    val (meshSubdivision, onMeshSubdivisionChange) = rememberPreference(
        MeshSubdivisionKey, defaultValue = 50
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

            PreferenceGroupTitle(title = "THEME")
            SwitchPreference(
                title = { Text("动态主题") },
                icon = { Icon(Icons.Rounded.Palette, null) },
                checked = dynamicTheme,
                onCheckedChange = onDynamicThemeChange
            )
            PreferenceGroupTitle(
                title = "PLAYLIST"
            )

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

            PreferenceGroupTitle(
                title = "PLAYER"
            )

            EnumListPreference(
                title = { Text("播放器样式") },
                icon = { Icon(Icons.Rounded.MusicVideo, null) },
                selectedValue = playerStyle,
                onValueSelected = onPlayerStyleChange,
                valueText = {
                    when (it) {
                        PlayerStyle.AppleMusic -> "Apple Music"
                        PlayerStyle.Classic -> "经典"
                    }
                }
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
                isEnabled = playerStyle == PlayerStyle.Classic,
                valueText = {
                    when (it) {
                        CoverStyle.Circle -> "圆形"
                        CoverStyle.Square -> "方形"
                    }
                }
            )
            EnumListPreference(
                title = { Text("进度条样式") },
                icon = { Icon(Icons.Rounded.LinearScale, null) },
                selectedValue = progressBarStyle,
                onValueSelected = onProgressBarStyleChange,
                valueText = { it.label }
            )

            PreferenceGroupTitle(
                title = "FLUID BACKGROUND"
            )
            SliderPreference(
                title = { Text("流动速度") },
                description = "渐变动画速度，值越大流动越快",
                icon = { Icon(Icons.Rounded.Speed, null) },
                value = meshFlowSpeed,
                onValueChange = onMeshFlowSpeedChange,
                valueRange = 0.05f..2f
            )
            SliderPreference(
                title = { Text("渲染精度") },
                description = "渲染分辨率缩放，越低越省电但越模糊",
                icon = { Icon(Icons.Rounded.Visibility, null) },
                value = meshRenderScale,
                onValueChange = onMeshRenderScaleChange,
                valueRange = 0.25f..1f,
                steps = 2
            )
            SliderPreference(
                title = { Text("节拍灵敏度") },
                description = "低频节拍对背景的影响程度",
                icon = { Icon(Icons.Rounded.Highlight, null) },
                value = meshLowFreqVolume,
                onValueChange = onMeshLowFreqVolumeChange,
                valueRange = 0f..0.5f,
                steps = 4
            )
            SliderPreference(
                title = { Text("网格细分") },
                description = "网格细分级数，越大越平滑越耗GPU",
                icon = { Icon(Icons.Rounded.GridOn, null) },
                value = meshSubdivision.toFloat(),
                onValueChange = { onMeshSubdivisionChange(it.toInt()) },
                valueRange = 10f..80f,
                steps = 6
            )
            SwitchPreference(
                title = { Text("静态模式") },
                description = "过渡完成后停止动画，省电",
                icon = { Icon(Icons.Rounded.Timer, null) },
                checked = meshStaticMode,
                onCheckedChange = onMeshStaticModeChange
            )
            SwitchPreference(
                title = { Text("播放动画") },
                description = "暂停/恢复背景渲染",
                icon = { Icon(Icons.Rounded.MusicVideo, null) },
                checked = meshPlaying,
                onCheckedChange = onMeshPlayingChange
            )

            PreferenceGroupTitle(
                title = "LYRIC"
            )

            SwitchPreference(
                title = { Text("主歌词字体加粗") },
                icon = { Icon(Icons.Rounded.FormatBold, null) },
                checked = normalLyricTextBold,
                onCheckedChange = onNormalLyricTextBoldChange
            )


            EnumListPreference(
                title = { Text("主歌词字体大小") },
                icon = { Icon(Icons.Rounded.FormatSize, null) },
                selectedValue = normalLyricTextSize,
                onValueSelected = onNormalLyricTextSizeChange,
                valueText = { it.text.toString() }
            )


            SwitchPreference(
                title = { Text("翻译歌词字体加粗") },
                icon = { Icon(Icons.Rounded.FormatBold, null) },
                checked = accompanimentLyricTextBold,
                onCheckedChange = onAccompanimentLyricTextBoldChange
            )


            EnumListPreference(
                title = { Text("翻译歌词字体大小") },
                icon = { Icon(Icons.Rounded.FormatSize, null) },
                selectedValue = accompanimentLyricTextSize,
                onValueSelected = onAccompanimentLyricTextSizeChange,
                valueText = { it.text.toString() }
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
