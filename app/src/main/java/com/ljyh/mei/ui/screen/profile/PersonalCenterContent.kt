package com.ljyh.mei.ui.screen.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.mei.ui.component.glass.*
import com.ljyh.mei.ui.component.layout.meiPageGutter
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun ProfileRefreshAction(
    state: PersonalCenterState,
    actions: PersonalCenterActions,
    inToolbar: Boolean = true,
) {
    if (!state.signedIn) return
    val enabled = !state.refreshing && !state.loading
    val icon: @Composable () -> Unit = { Icon(Icons.Rounded.Refresh, contentDescription = "刷新听歌档案") }
    if (inToolbar) MeiGlassIconButton(onClick = actions.onRefresh, enabled = enabled, content = icon)
    else IconButton(onClick = actions.onRefresh, enabled = enabled, content = icon)
}

@Composable
internal fun PersonalCenterContent(state: PersonalCenterState, actions: PersonalCenterActions) {
    MeiPinnedPage(
        title = "听歌档案", onNavigateBack = actions.onBack,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        actions = { ProfileRefreshAction(state, actions) },
    ) { padding ->
        Column(Modifier.fillMaxSize().verticalScroll(LocalMeiPageScroll.current!!).padding(padding)
            .padding(horizontal = meiPageGutter()), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            MeiLargeTitle("听歌档案", startPadding = 0.dp)
            ProfileIdentity(state, actions)
            if (state.signedIn && state.profile != null) ListeningArchiveContent(state, actions)
            ProfileAccountActions(state, actions)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
internal fun ProfileIdentity(
    state: PersonalCenterState,
    actions: PersonalCenterActions,
    centered: Boolean = false,
    avatarSize: Dp = if (centered) 112.dp else 80.dp,
) {
    val profile = state.profile
    val name = when {
        !state.signedIn -> "让音乐留下足迹"
        profile != null -> profile.nickname.ifBlank { "网易云用户" }
        state.loading -> "正在同步资料"
        else -> "暂时无法获取资料"
    }
    val avatar: @Composable () -> Unit = {
        Box(Modifier.size(avatarSize).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Person, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!profile?.avatarUrl.isNullOrBlank()) AsyncImage(profile.avatarUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
    val identity: @Composable ColumnScope.() -> Unit = {
        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (profile != null) {
            val badges = listOfNotNull(profile.level?.let { "Lv.$it" }, profile.membershipLabel)
            if (badges.isNotEmpty()) Text(badges.joinToString("  ·  "), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("${formatProfileCount(profile.follows)} 关注  ·  ${formatProfileCount(profile.followers)} 粉丝",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (centered) Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) { avatar(); identity() }
        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            avatar()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), content = identity)
        }
        if (!profile?.signature.isNullOrBlank()) Text(profile.signature, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!state.signedIn) Text("登录后查看听歌时长、常听歌曲和最近收听。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.loading && profile == null) CircularProgressIndicator(Modifier.size(24.dp))
        if (state.accountError) {
            Text("账号资料暂未同步，请重试或重新登录。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = actions.onRefresh, enabled = !state.refreshing) { Text("重新同步") }
        }
    }
}

/** Account utilities stay secondary to music; expansion is retained when returning from settings. */
@Composable
internal fun ProfileAccountActions(state: PersonalCenterState, actions: PersonalCenterActions) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!state.signedIn) Button(onClick = actions.onManageAccount, modifier = Modifier.fillMaxWidth()) { Text("登录账号") }
        else {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (expanded) "收起账号与会员" else "账号与会员")
            }
            if (expanded) {
                ProfileDetails(state)
                OutlinedButton(onClick = actions.onManageAccount, modifier = Modifier.fillMaxWidth()) { Text("管理 / 切换账号") }
                TextButton(onClick = actions.onLogout, modifier = Modifier.fillMaxWidth()) { Text("退出登录", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun ProfileDetails(state: PersonalCenterState) {
    val profile = state.profile
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (profile != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ID  ${profile.userId}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    context.getSystemService(android.content.ClipboardManager::class.java)
                        ?.setPrimaryClip(android.content.ClipData.newPlainText("账号 ID", profile.userId))
                    Toast.makeText(context, "账号 ID 已复制", Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Rounded.ContentCopy, "复制账号 ID", Modifier.size(18.dp)) }
            }
        }
        Text("${state.membership.label} · ${state.membership.status}", style = MaterialTheme.typography.bodyMedium)
        state.membership.expireTime?.let { timestamp ->
            val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
            Text("到期日期  $date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun formatProfileCount(value: Int?): String = value?.let { NumberFormat.getIntegerInstance().format(it) } ?: "—"
