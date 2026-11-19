package com.ljyh.mei.ui.component.player

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ljyh.mei.constants.AccompanimentLyricTextBoldKey
import com.ljyh.mei.constants.AccompanimentLyricTextSizeKey
import com.ljyh.mei.constants.LyricTextSize
import com.ljyh.mei.constants.NormalLyricTextBoldKey
import com.ljyh.mei.constants.NormalLyricTextSizeKey
import com.ljyh.mei.constants.PhoneticLyricTextBoldKey
import com.ljyh.mei.constants.PhoneticLyricTextSizeKey
import com.ljyh.mei.constants.ShowLyricPhoneticKey
import com.ljyh.mei.constants.ShowLyricTransitionKey
import com.ljyh.mei.ui.theme.SFPro
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference

/** The player and the preview render the same persisted typography and visibility choices. */
internal data class PlayerLyricStyle(
    val main: TextStyle,
    val translation: TextStyle,
    val phonetic: TextStyle,
    val showTranslation: Boolean,
    val showPhonetic: Boolean,
)

@Composable
internal fun rememberPlayerLyricStyle(): PlayerLyricStyle {
    val mainSize by rememberEnumPreference(NormalLyricTextSizeKey, LyricTextSize.Size34)
    val mainBold by rememberPreference(NormalLyricTextBoldKey, true)
    val translationSize by rememberEnumPreference(AccompanimentLyricTextSizeKey, LyricTextSize.Size20)
    val translationBold by rememberPreference(AccompanimentLyricTextBoldKey, true)
    val phoneticSize by rememberEnumPreference(PhoneticLyricTextSizeKey, LyricTextSize.Size20)
    val phoneticBold by rememberPreference(PhoneticLyricTextBoldKey, true)
    val showTranslation by rememberPreference(ShowLyricTransitionKey, true)
    val showPhonetic by rememberPreference(ShowLyricPhoneticKey, true)
    val baseStyle = LocalTextStyle.current.copy(
        fontFamily = SFPro(),
        lineHeight = TextUnit.Unspecified,
        textMotion = TextMotion.Animated,
    )
    return PlayerLyricStyle(
        main = baseStyle.copy(fontSize = mainSize.text.sp, fontWeight = if (mainBold) FontWeight.Bold else FontWeight.Normal),
        translation = baseStyle.copy(fontSize = translationSize.text.sp, fontWeight = if (translationBold) FontWeight.Bold else FontWeight.Normal),
        phonetic = baseStyle.copy(fontSize = phoneticSize.text.sp, fontWeight = if (phoneticBold) FontWeight.Bold else FontWeight.Normal),
        showTranslation = showTranslation,
        showPhonetic = showPhonetic,
    )
}
