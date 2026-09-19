package com.example.zenalarm.alarm

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.content.pm.ServiceInfo
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.zenalarm.AlarmRingingActivity
import com.example.zenalarm.R
import com.example.zenalarm.data.AlarmDatabase
import kotlinx.coroutines.runBlocking

class AlarmRingingService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var volumeAnimator: ValueAnimator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val dndHelper by lazy { DndHelper(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId <= 0L) {
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmInfo = runBlocking {
            val dao = AlarmDatabase.getInstance(this@AlarmRingingService).alarmDao()
            val alarm = dao.getAlarmById(alarmId) ?: return@runBlocking null
            val group = dao.getGroupById(alarm.groupId) ?: return@runBlocking null
            Triple(alarm, group, alarm.label.ifBlank { group.name })
        } ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        val (_, group, label) = alarmInfo
        acquireWakeLock()
        dndHelper.applyOverrideIfNeeded(group.overrideDnd)
        startAlarmSound(group.volumeRampSeconds)
        startVibration()

        val isSnooze = intent?.getBooleanExtra(AlarmReceiver.EXTRA_IS_SNOOZE, false) ?: false
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, isSnooze)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            fullScreenIntent,
            pendingIntentUpdateFlags(),
        )

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setContentText(label)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        startActivity(fullScreenIntent)

        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        stopAlarmSound()
        stopVibration()
        dndHelper.restorePreviousFilter()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ZenAlarm:AlarmWakeLock",
        ).apply {
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun startAlarmSound(volumeRampSeconds: Int) {
        stopAlarmSound()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val rampVolume = volumeRampSeconds > 0
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@AlarmRingingService, uri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            isLooping = true
            if (rampVolume) setVolume(0f, 0f)
            prepare()
            start()
        }
        if (rampVolume) {
            volumeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = volumeRampSeconds * 1000L
                addUpdateListener { animation ->
                    val vol = animation.animatedValue as Float
                    mediaPlayer?.setVolume(vol, vol)
                }
                start()
            }
        }
    }

    private fun stopAlarmSound() {
        volumeAnimator?.cancel()
        volumeAnimator = null
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    private var vibrator: Vibrator? = null

    private fun startVibration() {
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 800, 400, 800, 400), 0),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 800, 400, 800, 400), 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.alarm_channel_description)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "alarm_ringing"
        private const val NOTIFICATION_ID = 1001

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, AlarmRingingService::class.java))
        }
    }
}
