package com.example.zenalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.zenalarm.AlarmRingingActivity
import com.example.zenalarm.data.Alarm
import com.example.zenalarm.data.AlarmDatabase

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm, triggerAtMillis: Long? = null) {
        val trigger = triggerAtMillis ?: AlarmTimeCalculator.nextTriggerMillis(alarm)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, false)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(alarm.id, isSnooze = false),
            intent,
            pendingIntentUpdateFlags(),
        )

        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            Intent(context, AlarmRingingActivity::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            pendingIntentUpdateFlags(),
        )

        val info = AlarmManager.AlarmClockInfo(trigger, showIntent)
        alarmManager.setAlarmClock(info, pendingIntent)
    }

    fun scheduleSnooze(alarmId: Long, triggerAtMillis: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(alarmId, isSnooze = true),
            intent,
            pendingIntentUpdateFlags(),
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelAlarm(alarmId: Long) {
        cancelPendingIntent(alarmId, isSnooze = false)
        cancelPendingIntent(alarmId, isSnooze = true)
    }

    private fun cancelPendingIntent(alarmId: Long, isSnooze: Boolean) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(alarmId, isSnooze),
            intent,
            pendingIntentUpdateFlags(),
        )
        alarmManager.cancel(pendingIntent)
    }

    suspend fun rescheduleFromDatabase() {
        val dao = AlarmDatabase.getInstance(context).alarmDao()
        val alarms = dao.getAllAlarms()
        alarms.forEach { alarm ->
            val group = dao.getGroupById(alarm.groupId)
            if (alarm.enabled && group?.enabled == true) {
                scheduleAlarm(alarm)
            } else {
                cancelAlarm(alarm.id)
            }
        }
    }

    private fun requestCodeFor(alarmId: Long, isSnooze: Boolean): Int {
        val base = (alarmId and 0xFFFF).toInt()
        return if (isSnooze) base or 0x10000 else base
    }
}
