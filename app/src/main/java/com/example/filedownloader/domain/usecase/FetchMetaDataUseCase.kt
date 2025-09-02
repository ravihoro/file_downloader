package com.example.filedownloader.domain.usecase

import com.example.filedownloader.data.local.DownloadTask
import com.example.filedownloader.data.repository.DownloadTaskRepository
import com.example.filedownloader.data.repository.RemoteMetaDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FetchMetaDataUseCase @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val remoteMetaDataRepository: RemoteMetaDataRepository,
) {

    suspend operator fun invoke(url: String): Result<Long> = withContext(Dispatchers.IO) {
        try {

            val fileMeta = remoteMetaDataRepository.fetchMetaData(url)

            fileMeta.fold(
                onSuccess = { data ->
                    val task = DownloadTask(
                        url = data.url,
                        fileName = data.fileName,
                        totalBytes = data.contentLength,
                        mimeType = data.contentType,
                        supportsResume = data.supportsResume,
                    )

                    val id = repository.insertOrUpdate(task)
                    if (id == -1L) {
                        Result.failure(Exception("DB insert failed"))
                    } else {
                        Result.success(id)
                    }
                },
                onFailure = { e ->
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}