package com.ljyh.mei.utils.image

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Optional numeric calibration against local cover samples; no screenshots or device rendering. */
class CoverBackgroundSamplesTest {
    @Test
    fun reportLocalCoverDecisions() {
        val directory = System.getenv("MEI_COVER_SAMPLES")?.let(::File)
        assumeTrue("Set MEI_COVER_SAMPLES to run local calibration", directory?.isDirectory == true)
        directory!!.listFiles().orEmpty().filter {
            it.extension.lowercase() in setOf("jpg", "png") &&
                listOf("非常合适", "比较合适", "不合适", "不太合适").any(it.name::startsWith)
        }.sortedBy { it.name }.forEach { file ->
            val original = ImageIO.read(file) ?: return@forEach
            val reduced = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)
            val graphics = reduced.createGraphics()
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.drawImage(original, 0, 0, 128, 128, null)
            graphics.dispose()
            val pixels = reduced.getRGB(0, 0, 128, 128, null, 0, 128)
            val result = analyzeCoverBackground(pixels, 128, 128)
            println("COVER_SAMPLE ${file.name}: $result")
            CoverBackgroundPreset.entries.filter { it.config != null }.forEach { preset ->
                val classified = analyzeCoverBackground(pixels, 128, 128, preset.config!!)
                println("COVER_PRESET ${preset.name} ${file.name}: ${classified?.mode}")
            }
        }
    }
}
