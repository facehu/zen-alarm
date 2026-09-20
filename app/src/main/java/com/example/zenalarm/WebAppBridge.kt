package com.example.zenalarm

import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.zenalarm.data.Alarm
import com.example.zenalarm.data.AlarmGroup
import com.example.zenalarm.data.AlarmRepository
import com.example.zenalarm.data.AppSettings
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

class WebAppBridge(
    private val activity: MainActivity,
    private val repository: AlarmRepository,
    private val permissionHelper: PermissionHelper,
    private val appSettings: AppSettings,
    private val webView: WebView,
    private val runOnUiThread: (Runnable) -> Unit,
) {
    private val gson = Gson()

    @JavascriptInterface
    fun getState(): String = runBlocking {
        val groups = repository.getAllGroups()
        val alarms = repository.getAllAlarms()
        gson.toJson(AppState(groups, alarms))
    }

    @JavascriptInterface
    fun getPermissions(): String = permissionHelper.getStatusJson()

    @JavascriptInterface
    fun getSettings(): String {
        return gson.toJson(
            AppSettingsState(
                showNextAlarmNotification = appSettings.showNextAlarmNotification,
            ),
        )
    }

    @JavascriptInterface
    fun setShowNextAlarmNotification(enabled: Boolean) {
        appSettings.showNextAlarmNotification = enabled
        repository.refreshNextAlarmNotification()
        notifySettingsChanged()
    }

    @JavascriptInterface
    fun requestPermission(kind: String) {
        runOnUiThread {
            when (kind) {
                "notifications" -> activity.requestNotificationPermissionFromJs()
                "exactAlarms" -> activity.openExactAlarmSettingsFromJs()
                "dnd" -> activity.openDndSettingsFromJs()
            }
        }
    }

    @JavascriptInterface
    fun openNotificationSettings() {
        runOnUiThread { activity.openNotificationSettingsFromJs() }
    }

    @JavascriptInterface
    fun saveGroup(json: String): String = runBlocking {
        val group = gson.fromJson(json, AlarmGroup::class.java)
        val id = repository.saveGroup(group)
        notifyStateChanged()
        id.toString()
    }

    @JavascriptInterface
    fun setGroupEnabled(groupId: String, enabled: Boolean) = runBlocking {
        repository.setGroupEnabled(groupId.toLong(), enabled)
        notifyStateChanged()
    }

    @JavascriptInterface
    fun setAlarmEnabled(alarmId: String, enabled: Boolean) = runBlocking {
        val alarm = repository.getAlarm(alarmId.toLong()) ?: return@runBlocking
        repository.saveAlarm(alarm.copy(enabled = enabled))
        notifyStateChanged()
    }

    @JavascriptInterface
    fun deleteGroup(groupId: String) = runBlocking {
        val group = repository.getGroup(groupId.toLong()) ?: return@runBlocking
        repository.deleteGroup(group)
        notifyStateChanged()
    }

    @JavascriptInterface
    fun saveAlarm(json: String): String = runBlocking {
        val alarm = gson.fromJson(json, Alarm::class.java)
        val id = repository.saveAlarm(alarm)
        notifyStateChanged()
        id.toString()
    }

    @JavascriptInterface
    fun deleteAlarm(alarmId: String) = runBlocking {
        val alarm = repository.getAlarm(alarmId.toLong()) ?: return@runBlocking
        repository.deleteAlarm(alarm)
        notifyStateChanged()
    }

    @JavascriptInterface
    fun getChallengeTypes(): String {
        return gson.toJson(
            listOf(
                ChallengeTypeOption(
                    AlarmGroup.CHALLENGE_NONE,
                    activity.getString(R.string.challenge_type_none),
                ),
                ChallengeTypeOption(
                    AlarmGroup.CHALLENGE_MATH,
                    activity.getString(R.string.challenge_type_math),
                ),
            ),
        )
    }

    @JavascriptInterface
    fun getUiStrings(): String = UiStrings.toJson(activity)

    fun notifyStateChanged() {
        pushJsonToJs("onStateChanged", getState())
    }

    fun notifyPermissionsChanged() {
        pushJsonToJs("onPermissionsChanged", getPermissions())
    }

    fun notifySettingsChanged() {
        pushJsonToJs("onSettingsChanged", getSettings())
    }

    private fun pushJsonToJs(callback: String, json: String) {
        val encoded = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        runOnUiThread {
            webView.evaluateJavascript(
                "window.$callback(window.decodePayload('$encoded'));",
                null,
            )
        }
    }

    private data class AppState(
        val groups: List<AlarmGroup>,
        val alarms: List<Alarm>,
    )

    private data class AppSettingsState(
        val showNextAlarmNotification: Boolean,
    )

    private data class ChallengeTypeOption(
        val id: String,
        val label: String,
    )
}
