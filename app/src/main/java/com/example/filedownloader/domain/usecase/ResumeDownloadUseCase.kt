package com.example.filedownloader.domain.usecase

import com.example.filedownloader.data.local.DownloadTask
import javax.inject.Inject

class ResumeDownloadUseCase @Inject constructor(
    private val startDownloadUseCase: StartDownloadUseCase
) {

    suspend operator fun invoke(task: DownloadTask){
        startDownloadUseCase(task.copy(status = task.status))
    }

}