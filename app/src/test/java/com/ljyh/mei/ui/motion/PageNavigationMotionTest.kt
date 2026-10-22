package com.ljyh.mei.ui.motion

import com.ljyh.mei.constants.PageTransitionSpeed
import com.ljyh.mei.constants.PageTransitionStyle
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.preferences.toEnum
import org.junit.Assert.*
import org.junit.Test

class PageNavigationMotionTest {
    @Test
    fun rootSwitchesFadeButEveryDetailBoundarySlidesInBothDirections() {
        val roots = Screen.MainScreens.map { it.route }
        val details = listOf(
            "discover/playlists?category={category}", "discover/albums",
            "playlist/{id}", "album/{id}", "artist/{id}", "artist_songs/{id}",
            "search_result/{query}/{type}", "setting/appearance", "setting/play/usb",
        )
        for (from in roots) for (to in roots) {
            assertEquals(PageTransitionStyle.Fade, resolve(from, to).style)
        }
        for (root in roots) for (detail in details) {
            assertEquals(PageTransitionStyle.Slide, resolve(root, detail).style)
            assertEquals(PageTransitionStyle.Slide, resolve(detail, root).style)
        }
        for (from in details) for (to in details) {
            assertEquals(PageTransitionStyle.Slide, resolve(from, to).style)
        }
    }

    @Test
    fun disabledAnimationOverridesBothRootAndDetailTransitions() {
        for (from in listOf("home", "playlist/{id}")) for (to in listOf("library", "album/{id}")) {
            val spec = resolvePageTransition(from, to, PageTransitionStyle.None, PageTransitionSpeed.Relaxed)
            assertEquals(PageTransitionStyle.None, spec.style)
            assertEquals(0, spec.durationMillis)
        }
    }

    @Test
    fun fadeSelectionAlsoAppliesToDetailsAndSpeedAffectsBothKindsOfMotion() {
        val slowSlide = resolvePageTransition("home", "playlist/{id}", PageTransitionStyle.Slide, PageTransitionSpeed.Relaxed)
        val fastSlide = resolvePageTransition("home", "playlist/{id}", PageTransitionStyle.Slide, PageTransitionSpeed.Fast)
        val slowFade = resolvePageTransition("home", "playlist/{id}", PageTransitionStyle.Fade, PageTransitionSpeed.Relaxed)
        val fastFade = resolvePageTransition("home", "playlist/{id}", PageTransitionStyle.Fade, PageTransitionSpeed.Fast)
        assertEquals(PageTransitionStyle.Fade, slowFade.style)
        assertTrue(fastSlide.durationMillis < slowSlide.durationMillis)
        assertTrue(fastFade.durationMillis < slowFade.durationMillis)
    }

    @Test
    fun missingOrObsoletePreferencesUseTheDefaultsAndStartupDoesNotSlide() {
        assertEquals(PageTransitionStyle.Slide, "obsolete".toEnum(PageTransitionStyle.Slide))
        assertEquals(PageTransitionSpeed.Standard, (null as String?).toEnum(PageTransitionSpeed.Standard))
        assertEquals(PageTransitionStyle.None, resolve(null, "home").style)
    }

    private fun resolve(from: String?, to: String?) =
        resolvePageTransition(from, to, PageTransitionStyle.Slide, PageTransitionSpeed.Standard)
}
