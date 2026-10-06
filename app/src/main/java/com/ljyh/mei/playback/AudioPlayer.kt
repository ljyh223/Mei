package com.ljyh.mei.playback

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.media3.common.Player

/**
 * Controls ExoPlayer volume fades. It intentionally does not inherit MediaPlayer: creating an
 * unused platform player allocated native audio resources and made the two player lifecycles
 * appear coupled.
 */
class AudioPlayer(private val exoPlayer: Player) {

    private var volume = 1F

    var volumeSmoothDuration: Long = 500L
        set(value) {
            pauseSmoothValueAnimator.duration = value
            startSmoothValueAnimator.duration = value
            field = value
        }

    private fun setExoPlayerVolume(volume: Float) {
        exoPlayer.volume = volume
    }


    private val pauseSmoothValueAnimator = ValueAnimator.ofFloat(1F, 0F).apply {
        duration = volumeSmoothDuration
        interpolator = LinearInterpolator()
        addUpdateListener {
            volume = it.animatedValue as Float
            try {
//                setVolume(volume, volume)
                setExoPlayerVolume(volume)
            } catch (e: Exception) {
                it.cancel()
            }
        }
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                if (!isPauseSmoothing) return
                exoPlayer.volume = 0F
                exoPlayer.pause()
                isPauseSmoothing = false
            }

            override fun onAnimationCancel(animation: Animator) {
                isPauseSmoothing = false
            }

            override fun onAnimationRepeat(animation: Animator) { }
        })
    }

    private val startSmoothValueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
        duration = volumeSmoothDuration
        interpolator = LinearInterpolator()
        addUpdateListener {
            volume = it.animatedValue as Float
            try {
//                setVolume(volume, volume)
                setExoPlayerVolume(volume)
            } catch (e: Exception) {
                it.cancel()
            }
        }
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                exoPlayer.playWhenReady = true
            }

            override fun onAnimationEnd(animation: Animator) {
                if (!isStartSmoothing) return
                exoPlayer.volume = 1F
                isStartSmoothing = false
            }

            override fun onAnimationCancel(animation: Animator) {
                isStartSmoothing = false
            }

            override fun onAnimationRepeat(animation: Animator) { }
        })
    }

    var leftChannel: Float = 1F

    var rightChannel: Float = 1F

    private var isPauseSmoothing: Boolean = false

    private var isStartSmoothing: Boolean = false

    fun isPlaying(): Boolean {
        if (isPauseSmoothing) {
            return false
        }
        if (isStartSmoothing) {
            return true
        }
        return exoPlayer.isPlaying
    }

    fun pauseSmooth() {
        isPauseSmoothing = true
        startSmoothValueAnimator.cancel()
        pauseSmoothValueAnimator.start()
    }

    fun startSmooth() {
        isStartSmoothing = true
        pauseSmoothValueAnimator.cancel()
        startSmoothValueAnimator.start()
    }

    fun setVolume(leftVolume: Float, rightVolume: Float) {
        setExoPlayerVolume(leftVolume * leftChannel)
    }

    fun release() {
        pauseSmoothValueAnimator.cancel()
        startSmoothValueAnimator.cancel()
    }

    fun cancelFade() {
        pauseSmoothValueAnimator.cancel()
        startSmoothValueAnimator.cancel()
    }

}
