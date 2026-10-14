package com.deudom.deudom.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

object HapticManager {

    fun vibrateSuccess(context: Context) {
        val vibrator = getVibrator(context)
        if (vibrator?.hasVibrator() == true) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } catch (e: Exception) {
                // Ignore vibration errors
            }
        }
    }

    fun vibrateError(context: Context) {
        val vibrator = getVibrator(context)
        if (vibrator?.hasVibrator() == true) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                } else {
                    val pattern = longArrayOf(0, 50, 50, 50)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                }
            } catch (e: Exception) {
                // Ignore vibration errors
            }
        }
    }

    fun vibrateImpact(context: Context) {
        val vibrator = getVibrator(context)
        if (vibrator?.hasVibrator() == true) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, 50))
                }
            } catch (e: Exception) {
                // Ignore vibration errors
            }
        }
    }
    
    fun performHapticFeedback(view: View, feedbackConstant: Int) {
        view.performHapticFeedback(feedbackConstant)
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
