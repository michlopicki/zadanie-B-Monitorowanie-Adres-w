package com.example.bmonitoringurl

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MonitoredUrl::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun urlDao(): UrlDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "monitor_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}