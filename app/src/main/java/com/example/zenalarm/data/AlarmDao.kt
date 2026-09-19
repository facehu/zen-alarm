package com.example.zenalarm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarm_groups ORDER BY name")
    suspend fun getAllGroups(): List<AlarmGroup>

    @Query("SELECT * FROM alarm_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): AlarmGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: AlarmGroup): Long

    @Update
    suspend fun updateGroup(group: AlarmGroup)

    @Delete
    suspend fun deleteGroup(group: AlarmGroup)

    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    suspend fun getAllAlarms(): List<Alarm>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): Alarm?

    @Query("SELECT * FROM alarms WHERE groupId = :groupId ORDER BY hour, minute")
    suspend fun getAlarmsForGroup(groupId: Long): List<Alarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)
}
