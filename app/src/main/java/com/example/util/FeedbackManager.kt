package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class FeedbackManager(private val context: Context) {

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        Log.e("FeedbackManager", "Failed to obtain vibrator", e)
        null
    }

    private var toneGenerator: ToneGenerator? = null

    private var lastAudioTriggerTime = 0L
    private var lastHapticTriggerTime = 0L

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.e("FeedbackManager", "Failed to initialize ToneGenerator", e)
        }
    }

    fun triggerPerfectFeedback(hapticsEnabled: Boolean, audioEnabled: Boolean) {
        val now = System.currentTimeMillis()

        if (hapticsEnabled) {
            // Rate-limit haptics (e.g. at least 1.5 seconds between continuous beeps/pulses unless it's a fresh align)
            if (now - lastHapticTriggerTime > 1500) {
                vibrateSuccess()
                lastHapticTriggerTime = now
            }
        }

        if (audioEnabled) {
            if (now - lastAudioTriggerTime > 1500) {
                playSuccessBeep()
                lastAudioTriggerTime = now
            }
        }
    }

    private fun vibrateSuccess() {
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(150)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FeedbackManager", "Vibrate error", e)
        }
    }

    private fun playSuccessBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            Log.e("FeedbackManager", "Audio playback error", e)
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.e("FeedbackManager", "Release error", e)
        }
    }
}
