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
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ljyh.mei.constants.DownloadPathKey
import com.ljyh.mei.constants.DownloadQuality
import com.ljyh.mei.constants.DownloadQualityKey
import com.ljyh.mei.constants.EmbedOriginalTtmlKey
import com.ljyh.mei.ui.screen.setting.component.EditTextPreference
import com.ljyh.mei.ui.screen.setting.component.EnumListPreference
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.ui.screen.setting.component.SwitchPreference
import com.ljyh.mei.ui.local.LocalNavController
import androidx.compose.foundation.layout.fillMaxSize
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.download.DownloadManager
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingScreen(
    scrollBehavior: TopAppBarScrollBehavior
) {
    val navController = LocalNavController.current

    val defaultPath = DownloadManager.getDefaultDownloadPath()

    val (downloadPath, onDownloadPathChange) = rememberPreference(
        key = DownloadPathKey,
        defaultValue = defaultPath
    )

    val (downloadQuality, onDownloadQualityChange) = rememberEnumPreference(
        key = DownloadQualityKey,
        defaultValue = DownloadQuality.EXHIGH,
    )
    val (embedOriginalTtml, onEmbedOriginalTtmlChange) = rememberPreference(
        key = EmbedOriginalTtmlKey,
        defaultValue = false
    )

    MeiPinnedPage(
        title = "下载",
        onNavigateBack = navController::navigateUp,
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(LocalMeiPageScroll.current!!)
                .padding(paddingValues)
        ) {
            MeiLargeTitle("下载")

            PreferenceSection("下载文件") {

                EditTextPreference(
                    title = { Text("保存位置") },
                    icon = { Icon(Icons.Rounded.Folder, null) },
                    value = downloadPath,
                    onValueChange = onDownloadPathChange
                )

                EnumListPreference(
                    title = { Text("下载音质") },
                    icon = { Icon(Icons.Rounded.HighQuality, null) },
                    selectedValue = downloadQuality,
                    onValueSelected = onDownloadQualityChange,
                    valueText = { "${it.label} - ${it.description}" }
                )

                SwitchPreference(
                    title = { Text("保留原始 TTML 歌词") },
                    description = "有 TTML 时原样嵌入并保存 .ttml；关闭时使用普通 LRC",
                    icon = { Icon(Icons.Rounded.Lyrics, null) },
                    checked = embedOriginalTtml,
                    onCheckedChange = onEmbedOriginalTtmlChange
                )
            }
        }
    }
}
