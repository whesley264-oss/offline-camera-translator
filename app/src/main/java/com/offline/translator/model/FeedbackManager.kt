package com.offline.translator.model

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation

class FeedbackManager(private val context: Context, private val prefs: PreferencesManager) {
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Vibrate when translation is complete
     */
    fun vibrateOnTranslate() {
        if (!prefs.isHapticEnabled()) return
        
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use createWaveform for maximum compatibility (0 delay, vibrate once, no repeat)
                val timings = longArrayOf(0, 30)
                v.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(30)
            }
        }
    }
    
    /**
     * Vibrate on error
     */
    fun vibrateOnError() {
        if (!prefs.isHapticEnabled()) return
        
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Slightly longer vibration for error feedback
                val timings = longArrayOf(0, 100)
                v.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(100)
            }
        }
    }
    
    /**
     * Pulse animation on a view
     */
    fun animatePulse(view: View) {
        if (!prefs.isAnimationsEnabled()) return
        
        val pulse = ScaleAnimation(
            1f, 1.1f, 1f, 1.1f,
            view.width / 2f, view.height / 2f
        ).apply {
            duration = 100
            interpolator = OvershootInterpolator()
            repeatMode = android.view.animation.Animation.REVERSE
            repeatCount = 1
        }
        view.startAnimation(pulse)
    }
    
    /**
     * Bounce animation on success
     */
    fun animateSuccess(view: View) {
        if (!prefs.isAnimationsEnabled()) return
        
        val bounce = ScaleAnimation(
            1f, 0.9f, 1f, 0.9f,
            view.width / 2f, view.height / 2f
        ).apply {
            duration = 150
            interpolator = OvershootInterpolator(2f)
            repeatMode = android.view.animation.Animation.REVERSE
            repeatCount = 1
        }
        view.startAnimation(bounce)
    }
    
    /**
     * Shake animation on error
     */
    fun animateError(view: View) {
        if (!prefs.isAnimationsEnabled()) return
        
        val shake = android.view.animation.AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left) as? android.view.animation.TranslateAnimation
            ?: android.view.animation.TranslateAnimation(-10f, 10f, 0f, 0f).apply {
                duration = 50
                repeatCount = 3
                repeatMode = android.view.animation.Animation.REVERSE
            }
        view.startAnimation(shake)
    }
    
    /**
     * Fade in animation
     */
    fun animateFadeIn(view: View, duration: Long = 300) {
        if (!prefs.isAnimationsEnabled()) {
            view.alpha = 1f
            return
        }
        
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
    
    /**
     * Slide up animation
     */
    fun animateSlideUp(view: View, duration: Long = 300) {
        if (!prefs.isAnimationsEnabled()) {
            view.translationY = 0f
            return
        }
        
        view.translationY = 100f
        view.alpha = 0f
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
}