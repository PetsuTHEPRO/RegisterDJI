package com.sloth.registerapp.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SyncQueueEntity::class, MissionCacheEntity::class, FlightReportEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun missionCacheDao(): MissionCacheDao
    abstract fun flightReportDao(): FlightReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS flight_reports (
                        id TEXT NOT NULL PRIMARY KEY,
                        missionName TEXT NOT NULL,
                        aircraftName TEXT NOT NULL,
                        createdAtMs INTEGER NOT NULL,
                        startedAtMs INTEGER NOT NULL,
                        endedAtMs INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        finalObservation TEXT,
                        extraDataJson TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drone_app_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
