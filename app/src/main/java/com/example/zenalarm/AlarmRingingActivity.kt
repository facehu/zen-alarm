package com.example.zenalarm

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.zenalarm.alarm.AlarmReceiver
import com.example.zenalarm.alarm.AlarmRingingService
import com.example.zenalarm.alarm.AlarmScheduler
import com.example.zenalarm.alarm.MathChallenge
import com.example.zenalarm.alarm.MathChallengeGenerator
import com.example.zenalarm.alarm.NextAlarmNotification
import com.example.zenalarm.data.AlarmDatabase
import com.example.zenalarm.data.AlarmGroup
import com.example.zenalarm.data.AppSettings
import kotlinx.coroutines.runBlocking

class AlarmRingingActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var bridge: RingingBridge

    private var alarmId: Long = -1L
    private var challenge: MathChallenge? = null
    private var challengeSolved = false
    private var challengeType: String = AlarmGroup.CHALLENGE_NONE

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenBehavior()
        setContentView(R.layout.activity_main)

        alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) {
            finish()
            return
        }

        runBlocking {
            val dao = AlarmDatabase.getInstance(this@AlarmRingingActivity).alarmDao()
            val alarm = dao.getAlarmById(alarmId)
            val group = alarm?.let { dao.getGroupById(it.groupId) }
            challengeType = group?.challengeType ?: AlarmGroup.CHALLENGE_NONE
            if (challengeType == AlarmGroup.CHALLENGE_MATH) {
                val difficulty = group?.challengeDifficulty ?: AlarmGroup.DEFAULT_CHALLENGE_DIFFICULTY
                challenge = MathChallengeGenerator.generate(difficulty)
            }
        }

        webView = findViewById(R.id.webView)
        bridge = RingingBridge(
            activity = this,
            webView = webView,
            alarmId = alarmId,
            getChallenge = { challenge },
            getChallengeType = { challengeType },
            isChallengeSolved = { challengeSolved },
            setChallengeSolved = { challengeSolved = it },
            onDismiss = { handleDismiss() },
            onSnooze = { handleSnooze() },
            runOnUiThread = { runnable -> runOnUiThread(runnable) },
        )

        WebViewHelper.configure(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                bridge.pushStateToJs()
            }
        }

        webView.addJavascriptInterface(bridge, "AndroidBridge")
        WebViewHelper.loadAssetHtml(webView, "ringing.html")
    }

    private fun configureLockScreenBehavior() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        )
    }

    private fun handleSnooze() {
        runBlocking {
            val dao = AlarmDatabase.getInstance(this@AlarmRingingActivity).alarmDao()
            val alarm = dao.getAlarmById(alarmId) ?: return@runBlocking
            val group = dao.getGroupById(alarm.groupId) ?: return@runBlocking
            val snoozeAt = System.currentTimeMillis() + group.snoozeMinutes * 60_000L
            AppSettings(this@AlarmRingingActivity).setPendingSnooze(alarmId, snoozeAt)
            AlarmScheduler(this@AlarmRingingActivity).scheduleSnooze(alarmId, snoozeAt)
        }
        AlarmRingingService.stop(this)
        NextAlarmNotification.update(this)
        finish()
    }

    private fun handleDismiss() {
        runBlocking {
            val dao = AlarmDatabase.getInstance(this@AlarmRingingActivity).alarmDao()
            val alarm = dao.getAlarmById(alarmId) ?: return@runBlocking
            val scheduler = AlarmScheduler(this@AlarmRingingActivity)

            if (alarm.enabled) {
                scheduler.scheduleAlarm(alarm)
            } else {
                scheduler.cancelAlarm(alarm.id)
            }
        }
        AlarmRingingService.stop(this)
        NextAlarmNotification.update(this)
        finish()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
