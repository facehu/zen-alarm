package com.example.zenalarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.zenalarm.R
import com.google.gson.Gson

class PermissionHelper(private val context: Context) {
    private val gson = Gson()

    fun getStatusJson(): String {
        return gson.toJson(
            PermissionStatus(
                notifications = permissionEntry(
                    granted = hasNotificationPermission(),
                    required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                    canRequestInApp = true,
                    feature = context.getString(R.string.permission_feature_notifications),
                ),
                exactAlarms = permissionEntry(
                    granted = canScheduleExactAlarms(),
                    required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    canRequestInApp = false,
                    feature = context.getString(R.string.permission_feature_exact_alarms),
                ),
                dnd = permissionEntry(
                    granted = hasDndAccess(),
                    required = false,
                    canRequestInApp = false,
                    feature = context.getString(R.string.permission_feature_dnd),
                ),
            ),
        )
    }

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    fun hasDndAccess(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun createExactAlarmSettingsIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun createDndSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }

    fun createAppNotificationSettingsIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    private fun permissionEntry(
        granted: Boolean,
        required: Boolean,
        canRequestInApp: Boolean,
        feature: String,
    ): PermissionEntry {
        return PermissionEntry(
            granted = granted,
            required = required,
            canRequestInApp = canRequestInApp,
            feature = feature,
        )
    }

    data class PermissionStatus(
        val notifications: PermissionEntry,
        val exactAlarms: PermissionEntry,
        val dnd: PermissionEntry,
    )

    data class PermissionEntry(
        val granted: Boolean,
        val required: Boolean,
        val canRequestInApp: Boolean,
        val feature: String,
    )
}
