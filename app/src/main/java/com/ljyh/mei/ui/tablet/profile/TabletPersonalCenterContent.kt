package com.ljyh.mei.ui.tablet.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.screen.profile.*
import com.ljyh.mei.ui.tablet.component.TabletDetailPage

@Composable
internal fun TabletPersonalCenterContent(state: PersonalCenterState, actions: PersonalCenterActions) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = LocalPhoneLandscape.current
        val sideWidth = personalCenterSideWidth(maxWidth.value, LocalDensity.current.fontScale, landscape)
            .takeUnless { landscape && !state.signedIn }
        val saved = rememberSaveableStateHolder()
        saved.SaveableStateProvider(if (sideWidth == null) "compact" else "wide") {
            if (sideWidth == null) PersonalCenterContent(state, actions) else
            TabletDetailPage(title = "听歌档案", sideWidth = sideWidth.dp, onBack = actions.onBack,
                showToolbar = !landscape,
                actions = { ProfileRefreshAction(state, actions) },
                sidePane = { insets ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(insets)
                        .padding(horizontal = meiPageGutter()), verticalArrangement = Arrangement.spacedBy(if (landscape) 16.dp else 32.dp)) {
                        ProfileIdentity(state, actions, centered = true, avatarSize = if (landscape) 80.dp else 112.dp)
                        ProfileAccountActions(state, actions)
                    }
                },
                mainPane = { insets ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(insets)
                        .padding(start = 4.dp, end = meiPageGutter()), verticalArrangement = Arrangement.spacedBy(if (landscape) 12.dp else 24.dp)) {
                        if (landscape) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("听歌档案", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                                ProfileRefreshAction(state, actions, inToolbar = false)
                            }
                        }
                        when {
                            !state.signedIn -> {
                                Text("让音乐留下足迹", style = MaterialTheme.typography.headlineSmall)
                                Text("登录后查看听歌时长、常听歌曲和最近收听。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            state.profile != null -> ListeningArchiveContent(state, actions)
                            state.loading -> CircularProgressIndicator(Modifier.size(24.dp))
                            else -> {
                                Text("暂时无法获取听歌档案", style = MaterialTheme.typography.titleLarge)
                                TextButton(onClick = actions.onRefresh, enabled = !state.refreshing) { Text("重新同步") }
                            }
                        }
                    }
                },
            )
        }
    }
}

/** Profile data needs less width than artwork grids; keep ordinary portrait tablets split. */
internal fun personalCenterSideWidth(widthDp: Float, fontScale: Float, landscape: Boolean = false): Float? {
    if (!widthDp.isFinite()) return null
    val scale = fontScale.takeIf { it.isFinite() }?.coerceAtLeast(1f) ?: 1f
    val sideMinimum = (if (landscape) 204f else 224f) * scale
    val mainMinimum = (if (landscape) 296f else 320f) * scale
    val paneMinimum = sideMinimum + mainMinimum + 24f
    val widthMinimum = if (landscape) paneMinimum else maxOf(600f, paneMinimum)
    if (widthDp < widthMinimum) return null
    return (widthDp * 0.34f).coerceIn(sideMinimum, minOf(360f * scale, widthDp - mainMinimum - 24f))
}
