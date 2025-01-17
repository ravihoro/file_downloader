package com.example.filedownloader

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideDownloadTaskDao(appDatabase: AppDatabase): DownloadTaskDao {
        return appDatabase.downloadTaskDao();
    }

    @Provides
    @Singleton
    fun provideDownloadTaskRepository(downloadTaskDao: DownloadTaskDao): DownloadTaskRepository {
        return DownloadTaskRepository(downloadTaskDao);
    }

    @Provides
    @Singleton
    fun provideNotificationManager(@ApplicationContext context: Context): DownloadNotificationManager {
        return DownloadNotificationManager(context);
    }

}