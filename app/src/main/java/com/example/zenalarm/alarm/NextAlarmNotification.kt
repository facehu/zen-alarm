package com.example.zenalarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import com.example.zenalarm.MainActivity
import com.example.zenalarm.PermissionHelper
import com.example.zenalarm.R
import com.example.zenalarm.data.AlarmDatabase
import com.example.zenalarm.data.AppSettings
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.runBlocking
import java.util.Calendar

object NextAlarmNotification {
    private const val CHANNEL_ID = "next_alarm"
    private const val NOTIFICATION_ID = 1002

    fun update(context: Context) {
        val appContext = context.applicationContext
        val settings = AppSettings(appContext)
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!settings.showNextAlarmNotification ||
            !PermissionHelper(appContext).hasNotificationPermission()
        ) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        val upcoming = runBlocking {
            val dao = AlarmDatabase.getInstance(appContext).alarmDao()
            NextAlarmFinder.findNext(
                dao,
                settings,
                appContext.getString(R.string.default_alarm_label),
            )
        }

        if (upcoming == null) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        ensureChannel(appContext)

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            0,
            openAppIntent,
            pendingIntentUpdateFlags(),
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(appContext.getString(R.string.next_alarm_notification_title))
            .setContentText(formatBody(appContext, upcoming))
            .setSubText(upcoming.label)
            .setShowWhen(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatBody(context: Context, upcoming: UpcomingAlarm): String {
        val trigger = Calendar.getInstance().apply { timeInMillis = upcoming.triggerAtMillis }
        val now = Calendar.getInstance()
        val timeText = DateFormat.getTimeFormat(context).format(trigger.time)

        val sameDay = trigger.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            trigger.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        if (sameDay) {
            return timeText
        }

        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val isTomorrow = trigger.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
            trigger.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)
        if (isTomorrow) {
            return context.getString(R.string.next_alarm_tomorrow, timeText)
        }

        val dateText = DateFormat.getMediumDateFormat(context).format(trigger.time)
        return context.getString(R.string.next_alarm_date_time, dateText, timeText)
    }

    private fun ensureChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.next_alarm_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.next_alarm_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
