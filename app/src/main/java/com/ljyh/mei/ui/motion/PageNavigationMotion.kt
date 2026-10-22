package com.ljyh.mei.ui.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import com.ljyh.mei.constants.PageTransitionSpeed
import com.ljyh.mei.constants.PageTransitionStyle
import com.ljyh.mei.ui.component.navigation.isMainDestination

internal data class PageTransitionSpec(val style: PageTransitionStyle, val durationMillis: Int)

/** Peer tabs have no forward/back hierarchy. Detail routes, including collections, do. */
internal fun resolvePageTransition(
    fromRoute: String?,
    toRoute: String?,
    style: PageTransitionStyle,
    speed: PageTransitionSpeed,
): PageTransitionSpec {
    val effectiveStyle = when {
        fromRoute == null || toRoute == null -> PageTransitionStyle.None
        style == PageTransitionStyle.Slide && fromRoute.isMainDestination() && toRoute.isMainDestination() -> PageTransitionStyle.Fade
        else -> style
    }
    val duration = when (effectiveStyle) {
        PageTransitionStyle.Slide -> speed.slideMillis
        PageTransitionStyle.Fade -> speed.fadeMillis
        PageTransitionStyle.None -> 0
    }
    return PageTransitionSpec(effectiveStyle, duration)
}

/** One host-level policy keeps both sides of a navigation transition in sync. */
class PageNavigationMotion(
    private val style: PageTransitionStyle,
    private val speed: PageTransitionSpeed,
) {
    private fun spec(scope: AnimatedContentTransitionScope<NavBackStackEntry>) = resolvePageTransition(
        scope.initialState.destination.route, scope.targetState.destination.route, style, speed,
    )

    fun enter(scope: AnimatedContentTransitionScope<NavBackStackEntry>, isPop: Boolean = false): EnterTransition {
        val spec = spec(scope)
        return with(scope) {
            when (spec.style) {
                PageTransitionStyle.None -> EnterTransition.None
                PageTransitionStyle.Fade -> fadeIn(tween(spec.durationMillis))
                PageTransitionStyle.Slide -> slideIntoContainer(
                    towards = if (isPop) AnimatedContentTransitionScope.SlideDirection.End else AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(spec.durationMillis, easing = FastOutSlowInEasing),
                    initialOffset = { if (isPop) it / 4 else it },
                )
            }
        }
    }

    fun exit(scope: AnimatedContentTransitionScope<NavBackStackEntry>, isPop: Boolean = false): ExitTransition {
        val spec = spec(scope)
        return with(scope) {
            when (spec.style) {
                PageTransitionStyle.None -> ExitTransition.None
                PageTransitionStyle.Fade -> fadeOut(tween(spec.durationMillis))
                PageTransitionStyle.Slide -> slideOutOfContainer(
                    towards = if (isPop) AnimatedContentTransitionScope.SlideDirection.End else AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(spec.durationMillis, easing = FastOutSlowInEasing),
                    targetOffset = { if (isPop) it else it / 4 },
                )
            }
        }
    }
}
