package com.example.filedownloader

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DownloadTask::class], version = 6, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {

    abstract fun downloadTaskDao() : DownloadTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context) : AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "download_tasks_database"
                        ).fallbackToDestructiveMigration().build()
                    INSTANCE = instance
                    instance

            }
        }
    }
}