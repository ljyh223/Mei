package com.ljyh.mei.playback


import android.animation.Animator
import android.animation.ValueAnimator
import android.media.MediaPlayer
import android.view.animation.LinearInterpolator
import androidx.media3.exoplayer.ExoPlayer

/**
 * 音频播放器（部分源码，淡入淡出部分）
 * 由 MediaPlayer 拓展，使用很简单
 *
 * AudioPlayer (Part of the source code)
 * The android.media.MediaPlayer extensions for audio play.
 * It is very easy to use.
 *
 * @version 20210730
 * @author Moriafly
 * @since 2021/07/26
 */
class AudioPlayer(private val exoPlayer: ExoPlayer): MediaPlayer() {

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

    override fun isPlaying(): Boolean {
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

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        super.setVolume(
            leftVolume * leftChannel,
            rightVolume * rightChannel
        )

        setExoPlayerVolume(leftVolume)
    }

    override fun reset() {
        pauseSmoothValueAnimator.cancel()
        startSmoothValueAnimator.cancel()
        super.reset()
    }

}