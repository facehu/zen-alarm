package com.example.zenalarm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AlarmGroup::class, Alarm::class],
    version = 3,
    exportSchema = false,
)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var instance: AlarmDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists(
                    "alarm_groups",
                    "challengeDifficulty",
                    "INTEGER NOT NULL DEFAULT 3",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfNotExists(
                    "alarm_groups",
                    "volumeRampSeconds",
                    "INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        fun getInstance(context: Context): AlarmDatabase {
            val existing = instance
            if (existing != null) return existing

            return synchronized(this) {
                val current = instance
                if (current != null) {
                    current
                } else {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AlarmDatabase::class.java,
                        "alarms.db",
                    )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                        .build()
                        .also { instance = it }
                }
            }
        }
    }
}

private fun SupportSQLiteDatabase.addColumnIfNotExists(
    table: String,
    column: String,
    definition: String,
) {
    val cursor = query("PRAGMA table_info(`$table`)")
    cursor.use {
        while (it.moveToNext()) {
            if (it.getString(1) == column) return
        }
    }
    execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
}
