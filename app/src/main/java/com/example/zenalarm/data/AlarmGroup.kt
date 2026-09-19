package com.example.zenalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_groups")
data class AlarmGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val overrideDnd: Boolean = false,
    val challengeType: String = CHALLENGE_NONE,
    val challengeDifficulty: Int = DEFAULT_CHALLENGE_DIFFICULTY,
    val snoozeMinutes: Int = 9,
    val volumeRampSeconds: Int = 0,
) {
    companion object {
        const val CHALLENGE_NONE = "none"
        const val CHALLENGE_MATH = "math"
        const val MIN_CHALLENGE_DIFFICULTY = 1
        const val MAX_CHALLENGE_DIFFICULTY = 5
        const val DEFAULT_CHALLENGE_DIFFICULTY = 3
    }
}
