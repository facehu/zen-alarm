package com.example.zenalarm.alarm

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

object CallStateHelper {
    fun isInCall(context: Context): Boolean {
        if (isInCallViaAudio(context)) return true
        if (!hasReadPhoneState(context)) return false
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        @Suppress("DEPRECATION")
        val callState = telephony.callState
        return callState != TelephonyManager.CALL_STATE_IDLE
    }

    private fun isInCallViaAudio(context: Context): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when (audio.mode) {
            AudioManager.MODE_IN_CALL,
            AudioManager.MODE_IN_COMMUNICATION,
            -> true
            else -> false
        }
    }

    private fun hasReadPhoneState(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
