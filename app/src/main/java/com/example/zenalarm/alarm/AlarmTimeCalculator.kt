package com.example.zenalarm.alarm

import com.example.zenalarm.data.Alarm
import java.util.Calendar

object AlarmTimeCalculator {
    fun nextTriggerMillis(alarm: Alarm, afterMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = afterMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
        }

        if (alarm.repeatDays == 0) {
            if (cal.timeInMillis <= afterMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }

        for (dayOffset in 0..7) {
            if (dayOffset > 0) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, alarm.hour)
                cal.set(Calendar.MINUTE, alarm.minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            } else if (cal.timeInMillis <= afterMillis) {
                continue
            }

            val dayBit = dayBitForCalendarDay(cal.get(Calendar.DAY_OF_WEEK))
            if (alarm.repeatDays and dayBit != 0 && cal.timeInMillis > afterMillis) {
                return cal.timeInMillis
            }
        }

        cal.timeInMillis = afterMillis
        cal.set(Calendar.HOUR_OF_DAY, alarm.hour)
        cal.set(Calendar.MINUTE, alarm.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    fun dayBitForCalendarDay(dayOfWeek: Int): Int {
        return 1 shl (dayOfWeek - 1)
    }
}
