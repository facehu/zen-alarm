package com.example.zenalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.zenalarm.data.AlarmDatabase
import com.example.zenalarm.data.AppSettings
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM_TRIGGER) return

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return

        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        runBlocking {
            val dao = AlarmDatabase.getInstance(context).alarmDao()
            val alarm = dao.getAlarmById(alarmId) ?: return@runBlocking
            val group = dao.getGroupById(alarm.groupId) ?: return@runBlocking
            if (!alarm.enabled || !group.enabled) return@runBlocking

            AppSettings(context).clearPendingSnooze(alarmId)

            val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_IS_SNOOZE, isSnooze)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
            NextAlarmNotification.update(context)
        }
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.example.zenalarm.ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_IS_SNOOZE = "is_snooze"
    }
}
