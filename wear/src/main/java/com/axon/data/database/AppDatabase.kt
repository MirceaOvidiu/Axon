package com.axon.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.axon.domain.entity.RecordingSession
import com.axon.domain.entity.SensorData

@Database(
    entities = [SensorData::class, RecordingSession::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "axon_database",
                        ).fallbackToDestructiveMigration(true)
                        .build()
                INSTANCE = instance
                instance
            }
    }
}
