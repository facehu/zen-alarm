package com.example.zenalarm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

object UiStrings {
    fun toJson(context: Context): String {
        val json = JSONObject()
        val scalarKeys = listOf(
            "ui_settings" to R.string.ui_settings,
            "ui_new_group" to R.string.ui_new_group,
            "ui_group" to R.string.ui_group,
            "ui_name" to R.string.ui_name,
            "ui_group_enabled" to R.string.ui_group_enabled,
            "ui_override_dnd" to R.string.ui_override_dnd,
            "ui_wake_challenge" to R.string.ui_wake_challenge,
            "ui_math_difficulty" to R.string.ui_math_difficulty,
            "ui_snooze_minutes" to R.string.ui_snooze_minutes,
            "ui_volume_ramp_seconds" to R.string.ui_volume_ramp_seconds,
            "ui_volume_ramp_hint" to R.string.ui_volume_ramp_hint,
            "ui_delete_group" to R.string.ui_delete_group,
            "ui_cancel" to R.string.ui_cancel,
            "ui_save" to R.string.ui_save,
            "ui_alarm" to R.string.ui_alarm,
            "ui_time" to R.string.ui_time,
            "ui_label" to R.string.ui_label,
            "ui_label_placeholder" to R.string.ui_label_placeholder,
            "ui_alarm_enabled" to R.string.ui_alarm_enabled,
            "ui_repeat" to R.string.ui_repeat,
            "ui_repeat_hint" to R.string.ui_repeat_hint,
            "ui_delete_alarm" to R.string.ui_delete_alarm,
            "ui_permission" to R.string.ui_permission,
            "ui_permission_note" to R.string.ui_permission_note,
            "ui_no_thanks" to R.string.ui_no_thanks,
            "ui_continue" to R.string.ui_continue,
            "ui_show_next_alarm_notification" to R.string.ui_show_next_alarm_notification,
            "ui_show_next_alarm_hint" to R.string.ui_show_next_alarm_hint,
            "ui_done" to R.string.ui_done,
            "ui_confirm" to R.string.ui_confirm,
            "ui_delete" to R.string.ui_delete,
            "ui_repeat_daily" to R.string.ui_repeat_daily,
            "ui_repeat_every_day" to R.string.ui_repeat_every_day,
            "ui_group_meta_math" to R.string.ui_group_meta_math,
            "ui_group_meta_no_challenge" to R.string.ui_group_meta_no_challenge,
            "ui_group_meta_snooze" to R.string.ui_group_meta_snooze,
            "ui_group_meta_dnd_override" to R.string.ui_group_meta_dnd_override,
            "ui_group_meta_vol_ramp" to R.string.ui_group_meta_vol_ramp,
            "ui_tree_empty_groups" to R.string.ui_tree_empty_groups,
            "ui_tree_empty_alarms" to R.string.ui_tree_empty_alarms,
            "ui_add_alarm" to R.string.ui_add_alarm,
            "ui_toggle_alarm" to R.string.ui_toggle_alarm,
            "ui_toggle_collapse" to R.string.ui_toggle_collapse,
            "ui_toggle_group" to R.string.ui_toggle_group,
            "ui_perm_notifications" to R.string.ui_perm_notifications,
            "ui_perm_exact_alarms" to R.string.ui_perm_exact_alarms,
            "ui_perm_dnd" to R.string.ui_perm_dnd,
            "ui_perm_allow" to R.string.ui_perm_allow,
            "ui_perm_banner" to R.string.ui_perm_banner,
            "ui_perm_title_notifications" to R.string.ui_perm_title_notifications,
            "ui_perm_title_exact_alarms" to R.string.ui_perm_title_exact_alarms,
            "ui_perm_title_dnd" to R.string.ui_perm_title_dnd,
            "ui_perm_body_notifications" to R.string.ui_perm_body_notifications,
            "ui_perm_body_exact_alarms" to R.string.ui_perm_body_exact_alarms,
            "ui_perm_body_dnd" to R.string.ui_perm_body_dnd,
            "ui_perm_without_feature" to R.string.ui_perm_without_feature,
            "ui_settings_permissions" to R.string.ui_settings_permissions,
            "ui_perm_status_granted" to R.string.ui_perm_status_granted,
            "ui_perm_status_needed" to R.string.ui_perm_status_needed,
            "ui_perm_setup" to R.string.ui_perm_setup,
            "ui_open_notification_settings" to R.string.ui_open_notification_settings,
            "ui_edit_group" to R.string.ui_edit_group,
            "ui_new_group_title" to R.string.ui_new_group_title,
            "ui_edit_alarm" to R.string.ui_edit_alarm,
            "ui_new_alarm" to R.string.ui_new_alarm,
            "ui_delete_group_title" to R.string.ui_delete_group_title,
            "ui_delete_group_message" to R.string.ui_delete_group_message,
            "ui_delete_alarm_title" to R.string.ui_delete_alarm_title,
            "ui_delete_alarm_message" to R.string.ui_delete_alarm_message,
            "ui_ringing_solve" to R.string.ui_ringing_solve,
            "ui_ringing_answer_placeholder" to R.string.ui_ringing_answer_placeholder,
            "ui_ringing_submit" to R.string.ui_ringing_submit,
            "ui_ringing_snooze" to R.string.ui_ringing_snooze,
            "ui_ringing_snooze_minutes" to R.string.ui_ringing_snooze_minutes,
            "ui_ringing_dismiss" to R.string.ui_ringing_dismiss,
            "ui_ringing_correct" to R.string.ui_ringing_correct,
            "ui_ringing_wrong" to R.string.ui_ringing_wrong,
            "default_alarm_label" to R.string.default_alarm_label,
            "challenge_type_none" to R.string.challenge_type_none,
            "challenge_type_math" to R.string.challenge_type_math,
        )
        scalarKeys.forEach { (key, resId) ->
            json.put(key, context.getString(resId))
        }
        json.put("difficulty_labels", stringArrayToJson(context, R.array.difficulty_labels))
        json.put("day_labels", stringArrayToJson(context, R.array.day_labels))
        json.put(
            "first_day_of_week",
            Calendar.getInstance(Locale.getDefault()).firstDayOfWeek,
        )
        return json.toString()
    }

    private fun stringArrayToJson(context: Context, resId: Int): JSONArray {
        return JSONArray().apply {
            context.resources.getStringArray(resId).forEach { put(it) }
        }
    }
}
