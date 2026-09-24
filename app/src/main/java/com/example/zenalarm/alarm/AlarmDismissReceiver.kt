package com.example.zenalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISMISS_ALARM) return
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return
        AlarmRingingController.dismissAlarm(context, alarmId)
    }

    companion object {
        const val ACTION_DISMISS_ALARM = "com.example.zenalarm.DISMISS_ALARM"
    }
}
