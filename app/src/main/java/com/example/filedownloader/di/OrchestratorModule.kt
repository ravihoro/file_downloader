package com.example.filedownloader.di

import com.example.filedownloader.data.orchestrator.DownloadOrchestrator
import com.example.filedownloader.data.orchestrator.WorkManagerDownloadOrchestrator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrchestratorModule {

    @Binds
    @Singleton
    abstract fun bindDownloadOrchestrator(
        impl: WorkManagerDownloadOrchestrator
    ): DownloadOrchestrator
}