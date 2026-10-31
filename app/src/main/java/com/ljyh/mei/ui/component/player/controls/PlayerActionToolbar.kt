package com.ljyh.mei.ui.component.player.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.R
import com.ljyh.mei.playback.PlayMode
import com.ljyh.mei.ui.component.player.LocalPlayerForegroundColor
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.PlayerAction
import com.ljyh.mei.ui.model.PlayerActionProfile

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerActionToolbar(
    onLyricClick: () -> Unit,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier,
    profile: PlayerActionProfile = PlayerActionProfile.Phone,
    selectedAction: PlayerAction? = null,
) {
    val connection = LocalPlayerConnection.current ?: return
    val (saved) = rememberPlayerActionPreference(profile)
    val actions = remember(profile, saved) { profile.parse(saved) }
    val repeatMode by connection.repeatMode.collectAsState()
    val metadata by connection.mediaMetadata.collectAsState()
    val timerStatus = rememberSleepTimerStatus(connection.service.sleepTimer)
    PlayerActionRow(
        actions = actions,
        playMode = PlayMode.fromInt(repeatMode) ?: PlayMode.REPEAT_MODE_ALL,
        sleepTimerStatus = timerStatus,
        selectedAction = selectedAction,
        hasSong = metadata != null,
        canShare = metadata?.neteaseId?.let { it > 0 } == true,
        onAction = { if (it == PlayerAction.LYRICS) onLyricClick() else onAction(it) },
        modifier = modifier,
    )
}

/** Stateless action presentation, also used to verify availability and panel selection. */
@Composable
internal fun PlayerActionRow(
    actions: List<PlayerAction>,
    playMode: PlayMode,
    sleepTimerStatus: String?,
    selectedAction: PlayerAction?,
    hasSong: Boolean,
    canShare: Boolean,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier,
    foreground: Color = LocalPlayerForegroundColor.current,
) {
    Spacer(Modifier.height(16.dp))
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically) {
        actions.forEach { action ->
            val selected = selectedAction == action
            val enabled = action.isAvailable(hasSong, canShare)
            val tint = foreground.copy(alpha = foreground.alpha * when {
                !enabled -> .3f
                selected -> 1f
                else -> .7f
            })
            val timer = sleepTimerStatus.takeIf { action == PlayerAction.SLEEP_TIMER ||
                (action == PlayerAction.MORE && PlayerAction.SLEEP_TIMER !in actions) }
            IconButton(
                onClick = { onAction(action) }, enabled = enabled,
                modifier = Modifier.size(48.dp)
                    .background(foreground.copy(alpha = if (selected) .14f else 0f), CircleShape)
                    .semantics {
                        if (action == PlayerAction.QUEUE || action == PlayerAction.COMMENTS) this.selected = selected
                        if (timer != null) stateDescription = "剩余 $timer"
                    },
            ) {
                if (action == PlayerAction.PLAY_MODE) {
                    val icon = when (playMode) {
                        PlayMode.REPEAT_MODE_ONE -> R.drawable.ic_player_repeat_one
                        PlayMode.REPEAT_MODE_ALL -> R.drawable.ic_player_repeat
                        PlayMode.SHUFFLE_MODE_ALL -> R.drawable.ic_player_shuffle
                    }
                    AnimatedContent(icon, label = "PlayModeIcon") { target ->
                        Icon(painterResource(target), action.label, tint = tint)
                    }
                } else if (timer != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(action.icon, action.label, tint = tint, modifier = Modifier.size(18.dp))
                        Text(timer, color = tint, maxLines = 1,
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else Icon(action.icon, action.label, tint = tint)
            }
        }
    }
}

@Composable
fun ShadowedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(48.dp), content = content)
}
