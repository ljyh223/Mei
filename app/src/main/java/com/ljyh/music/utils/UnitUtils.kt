package com.ljyh.music.utils

import android.content.res.Resources
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

object UnitUtils {
    val Int.textDp: TextUnit
        @Composable get() = this.textDp(density = LocalDensity.current)

    private fun Int.textDp(density: Density): TextUnit = with(density) {
        this@textDp.dp.toSp()
    }

    fun dp2px(dp:Float) :Float=
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().displayMetrics)


    // 扩展方法：Dp 转 Px
    fun Dp.toPx(context: android.content.Context): Float {
        return this.value * context.resources.displayMetrics.density
    }
}