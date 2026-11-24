package com.ljyh.mei.ui.screen.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.preferences.dataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Both archive pages share the library entry's cache, including after navigation restoration. */
@Composable
internal fun rememberListeningArchive(userId: String?): ListeningArchiveViewModel {
    val nav = LocalNavController.current
    val parentEntry = remember(nav) { nav.getBackStackEntry(Screen.Library.route) }
    val model: ListeningArchiveViewModel = hiltViewModel(parentEntry)
    val context = LocalContext.current
    val cookie by remember(context) {
        context.dataStore.data.map { it[CookieKey].orEmpty() }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    LaunchedEffect(userId, cookie) { model.bind(userId, cookie) }
    return model
}
