package com.example.zenalarm

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.zenalarm.alarm.MathChallenge
import com.example.zenalarm.data.AlarmDatabase
import com.example.zenalarm.data.AlarmGroup
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

class RingingBridge(
    private val activity: AlarmRingingActivity,
    private val webView: WebView,
    private val alarmId: Long,
    private val getChallenge: () -> MathChallenge?,
    private val getChallengeType: () -> String,
    private val isChallengeSolved: () -> Boolean,
    private val setChallengeSolved: (Boolean) -> Unit,
    private val onDismiss: () -> Unit,
    private val onSnooze: () -> Unit,
    private val runOnUiThread: (Runnable) -> Unit,
) {
    private val gson = Gson()

    @JavascriptInterface
    fun getRingingState(): String {
        val state = runBlocking {
            val dao = AlarmDatabase.getInstance(activity).alarmDao()
            val alarm = dao.getAlarmById(alarmId)
            val group = alarm?.let { dao.getGroupById(it.groupId) }
            RingingState(
                label = resolveRingingLabel(alarm?.label, group?.name),
                groupName = group?.name ?: "",
                hour = alarm?.hour ?: 0,
                minute = alarm?.minute ?: 0,
                challengeType = group?.challengeType ?: AlarmGroup.CHALLENGE_NONE,
                challengeQuestion = getChallenge()?.question,
                snoozeMinutes = group?.snoozeMinutes ?: 9,
                canDismiss = canDismissNow(group?.challengeType ?: AlarmGroup.CHALLENGE_NONE),
            )
        }
        return gson.toJson(state)
    }

    @JavascriptInterface
    fun submitChallengeAnswer(answer: String): String {
        val challengeType = getChallengeType()
        if (challengeType == AlarmGroup.CHALLENGE_NONE) {
            setChallengeSolved(true)
            pushStateToJs()
            return gson.toJson(ChallengeResult(correct = true, canDismiss = true))
        }

        val expected = getChallenge()?.answer
        val parsed = answer.trim().toIntOrNull()
        val correct = parsed != null && parsed == expected
        if (correct) {
            setChallengeSolved(true)
        }
        pushStateToJs()
        return gson.toJson(ChallengeResult(correct = correct, canDismiss = correct))
    }

    @JavascriptInterface
    fun snooze() {
        onSnooze()
    }

    @JavascriptInterface
    fun dismiss() {
        if (canDismissNow(getChallengeType())) {
            onDismiss()
        }
    }

    fun pushStateToJs() {
        val json = getRingingState()
            .replace("\\", "\\\\")
            .replace("'", "\\'")
        runOnUiThread {
            webView.evaluateJavascript("window.onRingingState('$json');", null)
        }
    }

    private fun canDismissNow(challengeType: String): Boolean {
        return challengeType == AlarmGroup.CHALLENGE_NONE || isChallengeSolved()
    }

    private fun resolveRingingLabel(alarmLabel: String?, groupName: String?): String {
        val trimmedLabel = alarmLabel?.trim().orEmpty()
        if (trimmedLabel.isNotEmpty()) return trimmedLabel
        val trimmedGroup = groupName?.trim().orEmpty()
        if (trimmedGroup.isNotEmpty()) return trimmedGroup
        return activity.getString(R.string.default_alarm_label)
    }

    @JavascriptInterface
    fun getUiStrings(): String = UiStrings.toJson(activity)

    private data class RingingState(
        val label: String,
        val groupName: String,
        val hour: Int,
        val minute: Int,
        val challengeType: String,
        val challengeQuestion: String?,
        val snoozeMinutes: Int,
        val canDismiss: Boolean,
    )

    private data class ChallengeResult(
        val correct: Boolean,
        val canDismiss: Boolean,
    )
}
