package com.example.zenalarm.alarm

import android.app.NotificationManager
import android.content.Context
import android.os.Build

class DndHelper(context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var previousFilter: Int? = null

    fun canOverrideDnd(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    fun applyOverrideIfNeeded(overrideDnd: Boolean) {
        if (!overrideDnd || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (!notificationManager.isNotificationPolicyAccessGranted) return

        previousFilter = notificationManager.currentInterruptionFilter
        if (notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

    fun restorePreviousFilter() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val previous = previousFilter ?: return
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(previous)
        }
        previousFilter = null
    }
}
