package com.example.zenalarm.data

import android.content.Context
import com.example.zenalarm.alarm.AlarmScheduler
import com.example.zenalarm.alarm.NextAlarmNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlarmRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = AlarmDatabase.getInstance(appContext).alarmDao()
    private val scheduler = AlarmScheduler(appContext)

    suspend fun ensureDefaultGroup(): AlarmGroup = withContext(Dispatchers.IO) {
        val groups = dao.getAllGroups()
        if (groups.isNotEmpty()) {
            return@withContext groups.first()
        }
        val id = dao.insertGroup(
            AlarmGroup(name = "Default", enabled = true, challengeType = AlarmGroup.CHALLENGE_MATH),
        )
        dao.getGroupById(id)!!
    }

    suspend fun getAllGroups(): List<AlarmGroup> = withContext(Dispatchers.IO) {
        dao.getAllGroups()
    }

    suspend fun getGroup(id: Long): AlarmGroup? = withContext(Dispatchers.IO) {
        dao.getGroupById(id)
    }

    suspend fun saveGroup(group: AlarmGroup): Long = withContext(Dispatchers.IO) {
        val id = if (group.id == 0L) {
            dao.insertGroup(group)
        } else {
            dao.updateGroup(group)
            group.id
        }
        rescheduleAllAlarms()
        id
    }

    suspend fun setGroupEnabled(groupId: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        val group = dao.getGroupById(groupId) ?: return@withContext
        dao.updateGroup(group.copy(enabled = enabled))
        rescheduleAllAlarms()
    }

    suspend fun deleteGroup(group: AlarmGroup) = withContext(Dispatchers.IO) {
        val alarms = dao.getAlarmsForGroup(group.id)
        alarms.forEach { scheduler.cancelAlarm(it.id) }
        dao.deleteGroup(group)
        refreshNextAlarmNotification()
    }

    suspend fun getAllAlarms(): List<Alarm> = withContext(Dispatchers.IO) {
        dao.getAllAlarms()
    }

    suspend fun getAlarm(id: Long): Alarm? = withContext(Dispatchers.IO) {
        dao.getAlarmById(id)
    }

    suspend fun saveAlarm(alarm: Alarm): Long = withContext(Dispatchers.IO) {
        val id = if (alarm.id == 0L) {
            dao.insertAlarm(alarm)
        } else {
            dao.updateAlarm(alarm)
            alarm.id
        }
        rescheduleAlarm(id)
        id
    }

    suspend fun deleteAlarm(alarm: Alarm) = withContext(Dispatchers.IO) {
        scheduler.cancelAlarm(alarm.id)
        dao.deleteAlarm(alarm)
        refreshNextAlarmNotification()
    }

    suspend fun rescheduleAllAlarms() = withContext(Dispatchers.IO) {
        dao.getAllAlarms().forEach { alarm ->
            val group = dao.getGroupById(alarm.groupId)
            if (alarm.enabled && group?.enabled == true) {
                scheduler.scheduleAlarm(alarm)
            } else {
                scheduler.cancelAlarm(alarm.id)
            }
        }
        refreshNextAlarmNotification()
    }

    suspend fun rescheduleAlarm(alarmId: Long) = withContext(Dispatchers.IO) {
        val alarm = dao.getAlarmById(alarmId) ?: return@withContext
        val group = dao.getGroupById(alarm.groupId)
        if (alarm.enabled && group?.enabled == true) {
            scheduler.scheduleAlarm(alarm)
        } else {
            scheduler.cancelAlarm(alarm.id)
        }
        refreshNextAlarmNotification()
    }

    fun refreshNextAlarmNotification() {
        NextAlarmNotification.update(appContext)
    }
}
