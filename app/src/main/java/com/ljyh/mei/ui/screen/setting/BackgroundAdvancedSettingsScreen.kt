package com.ljyh.mei.ui.screen.setting

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ljyh.mei.constants.MeshRenderScaleKey
import com.ljyh.mei.constants.MeshSubdivisionKey
import com.ljyh.mei.ui.screen.setting.component.ListPreference
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.utils.preferences.rememberPreference

@Composable
internal fun BackgroundAdvancedSettingsScreen() {
    val (meshRenderScale, onMeshRenderScaleChange) = rememberPreference(
        MeshRenderScaleKey, defaultValue = 0.75f
    )
    val (meshSubdivision, onMeshSubdivisionChange) = rememberPreference(
        MeshSubdivisionKey, defaultValue = 50
    )
    AppearanceSettingsPage("背景高级设置") {
        PreferenceSection("渲染") {
            ListPreference(
                title = { Text("渲染精度") },
                description = "渲染分辨率缩放，越低越省电但越模糊",
                icon = { Icon(Icons.Rounded.Visibility, null) },
                selectedValue = meshRenderScale,
                values = listOf(0.25f, 0.5f, 0.75f, 1.0f),
                onValueSelected = onMeshRenderScaleChange,
                valueText = { "${(it * 100).toInt()}%" }
            )
            ListPreference(
                title = { Text("网格细分") },
                description = "网格细分级数，越大越平滑越耗GPU",
                icon = { Icon(Icons.Rounded.GridOn, null) },
                selectedValue = meshSubdivision,
                values = (1..8).map { it * 10 },
                onValueSelected = onMeshSubdivisionChange,
                valueText = { "$it" }
            )
        }
    }
}
