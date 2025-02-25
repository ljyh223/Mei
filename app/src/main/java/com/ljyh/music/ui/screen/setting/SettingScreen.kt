package com.ljyh.music.ui.screen.setting

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.ljyh.music.constants.CookieKey
import com.ljyh.music.constants.NetEaseCloudMusicApiServiceHostKey
import com.ljyh.music.ui.component.utils.AlertDialogInput
import com.ljyh.music.ui.local.LocalNavController
import com.ljyh.music.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.music.utils.dataStore
import com.ljyh.music.utils.rememberPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen() {

    val showCookieDialog = remember { mutableStateOf(false) }
    val showServiceDialog = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val (cookie, setCookie) = rememberPreference(CookieKey, "")
    val (netEaseCloudMusicApiServiceHost, setNetEaseCloudMusicApiServiceHost) = rememberPreference(
        NetEaseCloudMusicApiServiceHostKey, ""
    )
    AlertDialogInput(
        showDialog = showCookieDialog.value,
        title = "NetEaseCloudMusic Cookie",
        label = "MUSIC_U",
        onDismiss = {
            showCookieDialog.value = false
        },
        onConfirm = { newCookie ->
            setCookie(newCookie)
            showCookieDialog.value = false
        }
    )

    AlertDialogInput(
        showDialog = showServiceDialog.value,
        title = "NetEaseCloudMusicServiceApi",
        label = "host",
        onDismiss = {
            showServiceDialog.value = false
        },
        onConfirm = { newService ->
            setNetEaseCloudMusicApiServiceHost(newService)
            showServiceDialog.value = false
        }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier =
            Modifier
                .consumeWindowInsets(padding)
                .verticalScroll(scrollState)
                .padding(top = padding.calculateTopPadding())
        )
        {
            SampleSection(
                title = "Service",
            ) {
                SettingsMenuLink(
                    title = { Text(text = "NetEaseCloudMusicServiceApi") },
                    subtitle = { Text(text = netEaseCloudMusicApiServiceHost) },
                    onClick = {
                        showServiceDialog.value = true
                    }
                )
            }


            SampleSection(
                title = "User",
            ) {
                SettingsMenuLink(
                    title = { Text(text = "Cookie") },
                    subtitle = { Text(text = cookie.take(10)) },
                    enabled = true,
                    onClick = {
                        showCookieDialog.value = true
                    },
                )

                SettingsMenuLink(
                    title = { Text(text = "Test Cookie") },
                    enabled = true,
                    onClick = {
                    },
                )
            }
        }
    }


}