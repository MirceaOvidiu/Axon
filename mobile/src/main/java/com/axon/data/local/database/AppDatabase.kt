package com.axon.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.axon.data.local.dao.SessionDao
import com.axon.data.local.entity.Converters
import com.axon.data.local.entity.SensorDataEntity
import com.axon.data.local.entity.SessionEntity

@Database(
    entities = [
        SessionEntity::class,
        SensorDataEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
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
