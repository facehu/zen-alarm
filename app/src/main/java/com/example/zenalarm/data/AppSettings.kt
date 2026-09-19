package com.example.zenalarm.data

import android.content.Context

class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var showNextAlarmNotification: Boolean
        get() = prefs.getBoolean(KEY_SHOW_NEXT_ALARM_NOTIFICATION, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_NEXT_ALARM_NOTIFICATION, value).apply()

    var pendingSnoozeAlarmId: Long
        get() = prefs.getLong(KEY_PENDING_SNOOZE_ALARM_ID, -1L)
        private set(value) = prefs.edit().putLong(KEY_PENDING_SNOOZE_ALARM_ID, value).apply()

    var pendingSnoozeAtMillis: Long
        get() = prefs.getLong(KEY_PENDING_SNOOZE_AT_MILLIS, 0L)
        private set(value) = prefs.edit().putLong(KEY_PENDING_SNOOZE_AT_MILLIS, value).apply()

    fun setPendingSnooze(alarmId: Long, triggerAtMillis: Long) {
        prefs.edit()
            .putLong(KEY_PENDING_SNOOZE_ALARM_ID, alarmId)
            .putLong(KEY_PENDING_SNOOZE_AT_MILLIS, triggerAtMillis)
            .apply()
    }

    fun clearPendingSnooze(alarmId: Long? = null) {
        if (alarmId != null && pendingSnoozeAlarmId != alarmId) return
        prefs.edit()
            .remove(KEY_PENDING_SNOOZE_ALARM_ID)
            .remove(KEY_PENDING_SNOOZE_AT_MILLIS)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "zenalarm_settings"
        private const val KEY_SHOW_NEXT_ALARM_NOTIFICATION = "show_next_alarm_notification"
        private const val KEY_PENDING_SNOOZE_ALARM_ID = "pending_snooze_alarm_id"
        private const val KEY_PENDING_SNOOZE_AT_MILLIS = "pending_snooze_at_millis"
    }
}
