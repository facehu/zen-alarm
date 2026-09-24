package com.example.zenalarm.alarm

import android.content.Context
import android.media.AudioManager
import kotlin.math.roundToInt

class AlarmStreamVolumeHelper(context: Context) {
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var savedVolume: Int? = null

    fun applyIfNeeded(volumePercent: Int) {
        if (volumePercent <= 0) return
        if (savedVolume != null) return

        savedVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val target = (max * volumePercent / 100f).roundToInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
    }

    fun restore() {
        val previous = savedVolume ?: return
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previous, 0)
        savedVolume = null
    }
}
