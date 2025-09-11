package com.pauwma.glyphbeat.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

/**
 * Navigation animation specifications for GlyphBeat app.
 * Provides smooth, premium transitions between screens.
 */
object NavigationAnimations {

    // Animation durations
    const val TRANSITION_DURATION = 400
    const val FAST_TRANSITION = 250
    const val SLOW_TRANSITION = 600

    // Spring specs for natural motion
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val smoothSpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Easing curves
    val enteringEasing = FastOutSlowInEasing
    val exitingEasing = FastOutLinearInEasing

    /**
     * Slide in from right animation for forward navigation
     */
    fun slideInFromRight(): EnterTransition {
        return slideIn(
            initialOffset = { IntOffset(it.width, 0) },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = enteringEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION / 2,
                easing = LinearEasing
            )
        )
    }

    /**
     * Slide in from left animation for backward navigation
     */
    fun slideInFromLeft(): EnterTransition {
        return slideIn(
            initialOffset = { IntOffset(-it.width, 0) },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = enteringEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION / 2,
                easing = LinearEasing
            )
        )
    }

    /**
     * Slide out to left animation for forward navigation
     */
    fun slideOutToLeft(): ExitTransition {
        return slideOut(
            targetOffset = { IntOffset(-it.width, 0) },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = exitingEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION / 2,
                delayMillis = TRANSITION_DURATION / 4,
                easing = LinearEasing
            )
        )
    }

    /**
     * Slide out to right animation for backward navigation
     */
    fun slideOutToRight(): ExitTransition {
        return slideOut(
            targetOffset = { IntOffset(it.width, 0) },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = exitingEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION / 2,
                delayMillis = TRANSITION_DURATION / 4,
                easing = LinearEasing
            )
        )
    }

    /**
     * Parallax slide effect - slower movement for exiting screen
     */
    fun parallaxSlideInFromRight(): EnterTransition {
        return slideIn(
            initialOffset = { IntOffset(it.width, 0) },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION / 2,
                easing = LinearEasing
            )
        ) + scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = enteringEasing
            )
        )
    }

    fun parallaxSlideOutToLeft(): ExitTransition {
        return slideOut(
            targetOffset = { IntOffset(-it.width / 3, 0) }, // Slower movement
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = exitingEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = LinearEasing
            )
        ) + scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = exitingEasing
            )
        )
    }

    /**
     * Scale and fade transition for a more dramatic effect
     */
    fun scaleAndFadeIn(): EnterTransition {
        return scaleIn(
            initialScale = 0.92f,
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = enteringEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = FAST_TRANSITION,
                easing = LinearEasing
            )
        )
    }

    fun scaleAndFadeOut(): ExitTransition {
        return scaleOut(
            targetScale = 1.08f,
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = exitingEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = FAST_TRANSITION,
                delayMillis = 100,
                easing = LinearEasing
            )
        )
    }

    /**
     * Vertical slide animations for modal-like transitions
     */
    fun slideInFromBottom(): EnterTransition {
        return slideIn(
            initialOffset = { IntOffset(0, it.height) },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = FAST_TRANSITION,
                easing = LinearEasing
            )
        )
    }

    fun slideOutToBottom(): ExitTransition {
        return slideOut(
            targetOffset = { IntOffset(0, it.height) },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = exitingEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = FAST_TRANSITION,
                easing = LinearEasing
            )
        )
    }

    /**
     * Get appropriate enter transition based on navigation direction
     */
    fun getEnterTransition(fromRoute: String?, toRoute: String): EnterTransition {
        val fromIndex = getRouteIndex(fromRoute)
        val toIndex = getRouteIndex(toRoute)

        return when {
            fromIndex < toIndex -> parallaxSlideInFromRight()
            fromIndex > toIndex -> slideInFromLeft()
            else -> scaleAndFadeIn()
        }
    }

    /**
     * Get appropriate exit transition based on navigation direction
     */
    fun getExitTransition(fromRoute: String, toRoute: String?): ExitTransition {
        val fromIndex = getRouteIndex(fromRoute)
        val toIndex = getRouteIndex(toRoute)

        return when {
            fromIndex < toIndex -> parallaxSlideOutToLeft()
            fromIndex > toIndex -> slideOutToRight()
            else -> scaleAndFadeOut()
        }
    }

    private fun getRouteIndex(route: String?): Int {
        return when (route) {
            "media_player" -> 0
            "track_control" -> 1
            "settings" -> 2
            else -> -1
        }
    }
}

/**
 * Animated content wrapper for smooth transitions
 */
@Composable
fun <T> AnimatedNavContent(
    targetState: T,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    transitionSpec: AnimatedContentTransitionScope<T>.() -> ContentTransform = {
        fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                fadeOut(animationSpec = tween(90))
    },
    content: @Composable AnimatedContentScope.(T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = transitionSpec,
        content = content,
        label = "NavigationAnimation"
    )
}