package com.ljyh.music.ui.component.player

import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MultipleStop
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import com.ljyh.music.AppContext
import com.ljyh.music.constants.ListItemHeight
import com.ljyh.music.constants.PlayModeKey
import com.ljyh.music.data.model.metadata
import com.ljyh.music.playback.PlayMode
import com.ljyh.music.ui.component.BottomSheet
import com.ljyh.music.ui.component.BottomSheetState
import com.ljyh.music.ui.component.LocalMenuState
import com.ljyh.music.ui.local.LocalPlayerConnection
import com.ljyh.music.utils.TimeUtils.makeTimeString
import com.ljyh.music.utils.dataStore
import com.ljyh.music.utils.rememberPreference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun Queue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    backgroundColor: Color,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var playModeValue by rememberPreference(PlayModeKey, 3)
    val playMode = PlayMode.fromInt(playModeValue)!!
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false }
        )
    }
    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }
    val sleepTimerEnabled = remember(
        playerConnection.service.sleepTimer.triggerTime,
        playerConnection.service.sleepTimer.pauseWhenSongEnd
    ) {
        playerConnection.service.sleepTimer.isActive
    }
    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                }
                delay(1000L)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
    ) {


        IconButton(
            onClick = {
                playModeValue = playerConnection.switchPlayMode(playMode)
            },
        ) {
            Icon(
                imageVector = when (playMode) {
                    PlayMode.REPEAT_MODE_OFF -> Icons.Rounded.MultipleStop
                    PlayMode.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                    PlayMode.REPEAT_MODE_ALL -> Icons.Rounded.Repeat
                    PlayMode.SHUFFLE_MODE_ALL -> Icons.Rounded.Shuffle
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )

        }
        IconButton(onClick = { state.expandSoft() }) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }


        AnimatedContent(
            label = "sleepTimer",
            targetState = sleepTimerEnabled
        ) { sleepTimerEnabled ->
            if (sleepTimerEnabled) {
                Text(
                    text = makeTimeString(sleepTimerTimeLeft),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = playerConnection.service.sleepTimer::clear)
                        .padding(8.dp)
                )
            } else {
                IconButton(onClick = { showSleepTimerDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }


    }

}

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var selectedOption by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.Bedtime, contentDescription = null) },
        title = { Text("睡眠定时器") },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    playerConnection.service.sleepTimer.start((selectedOption ?: "0").toInt())
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                val options = listOf("5", "10", "20", "30", "45", "60")
                val pluralString = "${selectedOption ?: "0"} 分钟"
                val endTimeString = SimpleDateFormat
                    .getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault())
                    .format(
                        Date(
                            System.currentTimeMillis() + ((selectedOption
                                ?: "0").toInt() * 60 * 1000).toLong()
                        )
                    )

                Text(
                    text = "$pluralString\n$endTimeString",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )




                SingleSelectCapsuleChips(
                    options = options,
                    selectedOption = selectedOption,
                    onOptionSelected = { option ->
                        selectedOption = option
                    }
                )

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        playerConnection.service.sleepTimer.start(-1)
                    }
                ) {
                    Text("这首歌播放完毕")
                }
            }
        }
    )
}


@Composable
fun SingleSelectCapsuleChips(
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selectedOption,
                onClick = { onOptionSelected(option) },
                label = { Text(option) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
