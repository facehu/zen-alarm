package com.example.zenalarm.alarm

import android.content.Context
import android.content.Intent
import com.example.zenalarm.data.AlarmDatabase
import kotlinx.coroutines.runBlocking

object AlarmRingingController {
    const val ACTION_ALARM_DISMISSED = "com.example.zenalarm.ALARM_DISMISSED"
    const val EXTRA_ALARM_ID = AlarmReceiver.EXTRA_ALARM_ID

    fun dismissAlarm(context: Context, alarmId: Long) {
        val appContext = context.applicationContext
        runBlocking {
            val dao = AlarmDatabase.getInstance(appContext).alarmDao()
            val alarm = dao.getAlarmById(alarmId) ?: return@runBlocking
            val scheduler = AlarmScheduler(appContext)
            if (alarm.enabled) {
                scheduler.scheduleAlarm(alarm)
            } else {
                scheduler.cancelAlarm(alarm.id)
            }
        }
        AlarmRingingService.stop(appContext)
        NextAlarmNotification.update(appContext)
        appContext.sendBroadcast(
            Intent(ACTION_ALARM_DISMISSED).apply {
                setPackage(appContext.packageName)
                putExtra(EXTRA_ALARM_ID, alarmId)
            },
        )
    }
}
