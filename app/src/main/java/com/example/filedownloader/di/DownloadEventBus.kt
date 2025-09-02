package com.example.filedownloader.di

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEventBus @Inject constructor() {

    private val _taskIdFlow = MutableSharedFlow<Int>(replay = 1)
    val taskIdFlow: SharedFlow<Int> = _taskIdFlow

    fun publish(taskId: Int){
        _taskIdFlow.tryEmit(taskId)
    }

}