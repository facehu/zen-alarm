package com.example.zenalarm.alarm

import android.app.PendingIntent
import android.os.Build

internal fun pendingIntentUpdateFlags(): Int =
    PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
