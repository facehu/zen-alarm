package com.example.zenalarm.alarm

import com.example.zenalarm.data.Alarm
import com.example.zenalarm.data.AlarmDao
import com.example.zenalarm.data.AlarmGroup
import com.example.zenalarm.data.AppSettings

data class UpcomingAlarm(
    val alarm: Alarm,
    val triggerAtMillis: Long,
    val label: String,
)

object NextAlarmFinder {
    suspend fun findNext(dao: AlarmDao, settings: AppSettings): UpcomingAlarm? {
        val now = System.currentTimeMillis()
        var best: UpcomingAlarm? = null

        val snoozeId = settings.pendingSnoozeAlarmId
        val snoozeAt = settings.pendingSnoozeAtMillis
        if (snoozeId > 0L && snoozeAt > now) {
            val alarm = dao.getAlarmById(snoozeId)
            val group = alarm?.let { dao.getGroupById(it.groupId) }
            if (alarm != null && group != null && alarm.enabled && group.enabled) {
                best = UpcomingAlarm(
                    alarm = alarm,
                    triggerAtMillis = snoozeAt,
                    label = labelFor(alarm, group),
                )
            }
        }

        for (alarm in dao.getAllAlarms()) {
            val group = dao.getGroupById(alarm.groupId) ?: continue
            if (!alarm.enabled || !group.enabled) continue
            val trigger = AlarmTimeCalculator.nextTriggerMillis(alarm, now)
            val candidate = UpcomingAlarm(
                alarm = alarm,
                triggerAtMillis = trigger,
                label = labelFor(alarm, group),
            )
            if (best == null || candidate.triggerAtMillis < best.triggerAtMillis) {
                best = candidate
            }
        }

        return best
    }

    private fun labelFor(alarm: Alarm, group: AlarmGroup): String {
        val trimmedLabel = alarm.label.trim()
        if (trimmedLabel.isNotEmpty()) return trimmedLabel
        val trimmedGroup = group.name.trim()
        if (trimmedGroup.isNotEmpty()) return trimmedGroup
        return "Alarm"
    }
}
