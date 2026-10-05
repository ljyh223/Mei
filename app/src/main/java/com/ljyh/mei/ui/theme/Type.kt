package com.ljyh.mei.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ljyh.mei.R

@OptIn(ExperimentalTextApi::class)
@Composable
private fun sfProFont(weight: FontWeight) =
    Font(
         R.font.sf_pro,
        weight = weight,
        variationSettings =
            remember(weight) { FontVariation.Settings(FontVariation.weight(weight.weight)) },
    )

@Composable
fun SFPro(): FontFamily {
    val extraLight = sfProFont(FontWeight.ExtraLight)
    val light = sfProFont(FontWeight.Light)
    val medium = sfProFont(FontWeight.Medium)
    val semiBold = sfProFont(FontWeight.SemiBold)
    val bold = sfProFont(FontWeight.Bold)
    val extraBold = sfProFont(FontWeight.ExtraBold)
    return remember(extraLight, light, medium, semiBold, bold, extraBold) {
        FontFamily(extraLight, light, medium, semiBold, bold, extraBold)
    }
}

@Composable
fun Typography(): Typography {
    val family = SFPro()
    return remember(family) {
        Typography(
            displayLarge = TextStyle(fontFamily = family),
            displayMedium = TextStyle(fontFamily = family),
            displaySmall = TextStyle(fontFamily = family),
            headlineLarge = TextStyle(fontFamily = family),
            headlineMedium = TextStyle(fontFamily = family),
            headlineSmall = TextStyle(fontFamily = family),
            titleLarge = TextStyle(fontFamily = family),
            titleMedium = TextStyle(fontFamily = family),
            titleSmall = TextStyle(fontFamily = family),
            bodyLarge =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.5.sp,
                ),
            bodyMedium = TextStyle(fontFamily = family),
            bodySmall = TextStyle(fontFamily = family),
            labelLarge = TextStyle(fontFamily = family),
            labelMedium = TextStyle(fontFamily = family),
            labelSmall = TextStyle(fontFamily = family),
        )
    }
}
