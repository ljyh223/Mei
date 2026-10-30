package com.ljyh.mei.ui.component.player.controls

import androidx.compose.runtime.Composable
import com.ljyh.mei.constants.PlayerActionKey
import com.ljyh.mei.constants.TabletPlayerActionKey
import com.ljyh.mei.ui.model.PlayerActionProfile
import com.ljyh.mei.utils.preferences.rememberPreference

@Composable
internal fun rememberPlayerActionPreference(profile: PlayerActionProfile) = rememberPreference(
    key = if (profile == PlayerActionProfile.Tablet) TabletPlayerActionKey else PlayerActionKey,
    defaultValue = "",
)
