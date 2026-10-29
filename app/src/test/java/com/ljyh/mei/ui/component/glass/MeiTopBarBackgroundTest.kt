package com.ljyh.mei.ui.component.glass

import org.junit.Assert.assertEquals
import org.junit.Test

class MeiTopBarBackgroundTest {
    @Test fun automaticKeepsExistingDefaultsAndUsesOpticsOnlyWhenAvailable() {
        assertEquals(MeiTopBarBackgroundState(false, 1f),
            resolveMeiTopBarBackground(MeiTopBarBackground.Auto, false, false))
        assertEquals(MeiTopBarBackgroundState(true, 0.4f),
            resolveMeiTopBarBackground(MeiTopBarBackground.Auto, true, false, glassProgress = 0.4f))
    }

    @Test fun transparentPagesStayTransparentAtEveryScrollProgress() {
        for (progress in listOf(0f, 0.25f, 0.8f, 1f)) {
            assertEquals(MeiTopBarBackgroundState(false, 0f),
                resolveMeiTopBarBackground(MeiTopBarBackground.Transparent, false, false, glassProgress = progress))
        }
    }

    @Test fun explicitBackgroundIntentIsIndependentOfGlassAvailability() {
        for (available in listOf(false, true)) {
            assertEquals(MeiTopBarBackgroundState(false, 0f),
                resolveMeiTopBarBackground(MeiTopBarBackground.Transparent, available, false))
            assertEquals(MeiTopBarBackgroundState(false, 1f),
                resolveMeiTopBarBackground(MeiTopBarBackground.Solid, available, false))
        }
    }

    @Test fun reducedTransparencyOverridesEvenExplicitTransparentPages() {
        for (mode in MeiTopBarBackground.entries) for (available in listOf(false, true)) {
            assertEquals(MeiTopBarBackgroundState(false, 1f),
                resolveMeiTopBarBackground(mode, available, true, glassProgress = 0f))
        }
    }

    @Test fun rendererNeverReceivesInvalidOpacity() {
        assertEquals(0f, resolveMeiTopBarBackground(MeiTopBarBackground.Auto, true, false, glassProgress = -1f).opacity, 0f)
        assertEquals(1f, resolveMeiTopBarBackground(MeiTopBarBackground.Auto, true, false, glassProgress = 3f).opacity, 0f)
        assertEquals(1f, resolveMeiTopBarBackground(MeiTopBarBackground.Auto, true, false, glassProgress = Float.NaN).opacity, 0f)
    }
}
