package com.axon.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.axon.data.local.dao.SessionDao
import com.axon.data.local.entity.SensorDataEntity
import com.axon.data.local.entity.SessionEntity

@Database(
    entities = [SensorDataEntity::class, SessionEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

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
                            "axon_mobile_database",
                        ).fallbackToDestructiveMigration(true)
                        .build()
                INSTANCE = instance
                instance
            }
    }
}
