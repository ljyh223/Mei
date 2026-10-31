package com.ljyh.mei.ui.tablet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable

class TabletContentTestActivity : ComponentActivity() {
    companion object { var content: @Composable () -> Unit = {} }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { content() }
    }
}
