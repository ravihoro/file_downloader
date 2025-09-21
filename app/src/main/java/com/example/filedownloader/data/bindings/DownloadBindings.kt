package com.example.filedownloader.data.bindings

import android.content.Context
import com.example.filedownloader.data.manager.InAppDownloadManager
import com.example.filedownloader.data.notification.DownloadNotificationManager
import com.example.filedownloader.data.orchestrator.DownloadOrchestrator
import com.example.filedownloader.data.orchestrator.InAppDownloadOrchestrator
import com.example.filedownloader.data.repository.DownloadTaskRepository
import com.example.filedownloader.data.repository.FileRepository
import com.example.filedownloader.data.repository.RemoteDownloadDataRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadBindings {

    @Binds
    abstract fun bindDownloadOrchestrator(impl: InAppDownloadOrchestrator): DownloadOrchestrator

    companion object {
        @Provides
        @Singleton
        fun provideInScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Provides
        @Singleton
        fun provideInAppDownloadManager (
            repository: DownloadTaskRepository,
            fileRepository: FileRepository,
            remoteRepo: RemoteDownloadDataRepository,
            notificationManager: DownloadNotificationManager,
            @ApplicationContext context: Context,
            ioScope: CoroutineScope
        ): InAppDownloadManager {
            return InAppDownloadManager(repository, fileRepository, remoteRepo, notificationManager, context, ioScope)
        }
    }

}