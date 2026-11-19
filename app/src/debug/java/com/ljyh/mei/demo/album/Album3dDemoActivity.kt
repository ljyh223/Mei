package com.ljyh.mei.demo.album

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.ljyh.mei.ui.theme.MeiShapes

/** Debug-only launcher; never participates in the music app's navigation or player. */
open class Album3dDemoActivity : ComponentActivity() {
    internal open val initialFormat: AlbumDemoFormat = AlbumDemoFormat.CD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFC4D4A0),
                    onPrimary = Color(0xFF243119),
                    primaryContainer = Color(0xFF334329),
                    onPrimaryContainer = Color(0xFFE4EBCF),
                    background = Color(0xFF0C100B),
                    surface = Color(0xFF0C100B),
                    onSurface = Color(0xFFF0F1E8),
                    onSurfaceVariant = Color(0xFFB0B8A6),
                    surfaceContainer = Color(0xFF1A2117),
                    outline = Color(0xFF89927D),
                ),
                shapes = MeiShapes,
            ) {
                Surface { Album3dDemoScreen(onClose = ::finish, initialFormat = initialFormat) }
            }
        }
    }
}

/** Separate launcher entries share the Demo shell and open their own format directly. */
class Cassette3dDemoActivity : Album3dDemoActivity() {
    internal override val initialFormat: AlbumDemoFormat = AlbumDemoFormat.Cassette
}

class Vinyl3dDemoActivity : Album3dDemoActivity() {
    internal override val initialFormat: AlbumDemoFormat = AlbumDemoFormat.Vinyl
}
