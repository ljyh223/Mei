package com.ljyh.mei.ui.screen.artist

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import com.ljyh.mei.ui.component.artwork.backdrop.ArtworkVariableBlurShader
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Compile AGSL through native Skia, without an emulator, Activity or rendered screenshot. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Robolectric uses an older SkSL compiler on SDK 33/34 which cannot parse shader.eval;
// SDK 36 exercises AGSL itself. Production uses the documented Android 13 API surface.
class ArtistBackdropShaderTest {
    @Test fun squareAlbumArtworkSupportsAnExtendedClearRegion() {
        val effect = com.ljyh.mei.ui.component.artwork.backdrop.ArtworkVariableBlur()
            .effect(androidx.compose.ui.geometry.Size(400f, 600f), 1f, .6f)
        org.junit.Assert.assertNotNull(effect)
    }

    @Test fun variableBlurCompilesAndBindsBothPasses() {
        val horizontal = RuntimeShader(ArtworkVariableBlurShader)
        val vertical = RuntimeShader(ArtworkVariableBlurShader)
        for ((shader, axis) in listOf(horizontal to floatArrayOf(1f, 0f), vertical to floatArrayOf(0f, 1f))) {
            shader.setFloatUniform("bounds", 1280f, 800f)
            shader.setFloatUniform("axis", axis)
            shader.setFloatUniform("maxRadius", 160f)
            shader.setFloatUniform("clearHeight", 0f)
        }
        RenderEffect.createChainEffect(
            RenderEffect.createRuntimeShaderEffect(vertical, "image"),
            RenderEffect.createRuntimeShaderEffect(horizontal, "image"),
        )
    }
}
