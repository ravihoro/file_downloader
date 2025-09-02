package com.example.filedownloader.domain.usecase

import com.example.filedownloader.data.repository.DownloadTaskRepository
import javax.inject.Inject

class AddDownloadUseCase @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val startDownloadUseCase: StartDownloadUseCase,
    private val fetchMetaDataUseCase: FetchMetaDataUseCase,
) {

    suspend operator fun invoke(url: String): Result<Unit> {
        return try {

            val result = fetchMetaDataUseCase(url)

            result.fold(
                onSuccess = { taskId ->
                    val task = repository.getTaskById(taskId.toInt())

                    if(task != null){
                        startDownloadUseCase(task)
                        Result.success(Unit)
                    }else{
                        Result.failure(Exception("Task not found after inserting"))
                    }
                },
                onFailure = { e ->
                    Result.failure(e)
                }
            )

        } catch (e: Exception){
            Result.failure(e)
        }
    }

}