package com.example.filedownloader.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.example.filedownloader.data.notification.DownloadNotificationManager
import com.example.filedownloader.data.local.AppDatabase
import com.example.filedownloader.data.local.DownloadTaskDao
import com.example.filedownloader.data.orchestrator.DownloadOrchestrator
//import com.example.filedownloader.data.orchestrator.WorkManagerDownloadOrchestrator
import com.example.filedownloader.data.repository.DownloadTaskRepository
import com.example.filedownloader.data.repository.FileRepository
import com.example.filedownloader.data.repository.RemoteDownloadDataRepository
import com.example.filedownloader.data.repository.RemoteMetaDataRepository
import com.example.filedownloader.domain.usecase.CancelDownloadUseCase
import com.example.filedownloader.domain.usecase.PauseDownloadUseCase
import com.example.filedownloader.domain.usecase.ResumeDownloadUseCase
import com.example.filedownloader.domain.usecase.StartDownloadUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "download_tasks_database"
        ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideDownloadTaskDao(db: AppDatabase): DownloadTaskDao = db.downloadTaskDao()

    @Provides
    @Singleton
    fun provideDownloadTaskRepository(dao: DownloadTaskDao): DownloadTaskRepository =
        DownloadTaskRepository(dao)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

    @Provides
    @Singleton
    fun provideNotificationManager(@ApplicationContext context: Context): DownloadNotificationManager =
        DownloadNotificationManager(context)

    @Provides
    @Singleton
    fun provideFileRepository(@ApplicationContext context: Context): FileRepository =
        FileRepository(context)

    @Provides
    @Singleton
    fun provideRemoteMetaDataRepository(client: OkHttpClient): RemoteMetaDataRepository =
        RemoteMetaDataRepository(client)

    @Provides
    @Singleton
    fun provideRemoteDownloadDataRepository(client: OkHttpClient): RemoteDownloadDataRepository =
        RemoteDownloadDataRepository(client)

//    @Provides
//    @Singleton
//    fun provideWorkManagerDownloadOrchestrator(
//        @ApplicationContext context: Context,
//        @IoScope ioScope: CoroutineScope,
//        repository: DownloadTaskRepository
//    ): DownloadOrchestrator =
//        WorkManagerDownloadOrchestrator(context, ioScope, repository)

}

