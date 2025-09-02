package com.example.filedownloader.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.repository.DownloadTaskRepository
import com.example.filedownloader.domain.usecase.CancelDownloadUseCase
import com.example.filedownloader.domain.usecase.PauseDownloadUseCase
import com.example.filedownloader.domain.usecase.ResumeDownloadUseCase
import com.example.filedownloader.domain.usecase.StartDownloadUseCase
import com.example.filedownloader.presentation.state.DownloadUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.lifecycle.viewModelScope
import com.example.filedownloader.domain.usecase.AddDownloadUseCase
import com.example.filedownloader.domain.usecase.FetchMetaDataUseCase
import com.example.filedownloader.presentation.event.DownloadEvent
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val startDownloadUseCase: StartDownloadUseCase,
    private val pauseDownloadUseCase: PauseDownloadUseCase,
    private val resumeDownloadUseCase: ResumeDownloadUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val addDownloadUseCase: AddDownloadUseCase,
) : ViewModel(){

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    init {
        observeDownloads()
    }

    private fun observeDownloads() {
        val activeFlow = repository.getTaskByStatus(DownloadStatus.ACTIVE)
        val completedFlow = repository.getTaskByStatus(DownloadStatus.COMPLETED)

        combine(activeFlow, completedFlow) { active, completed ->
            DownloadUiState(active = active, completed = completed)
        }.onEach { newState ->
            _uiState.value = newState
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: DownloadEvent){
        viewModelScope.launch {
            try{
                when(event){
                    is DownloadEvent.Add -> {
                        _uiState.update { it.copy(isLoading = true) }

                        val result = addDownloadUseCase(event.url)

                        result.onFailure { e ->
                            _uiState.update { it.copy(error = e.message) }
                        }

                        _uiState.update { it.copy(isLoading = false) }

                    }
                    is DownloadEvent.Start -> startDownloadUseCase(event.task)
                    is DownloadEvent.Pause -> pauseDownloadUseCase(event.taskId, event.downloadedBytes, event.progress)
                    is DownloadEvent.Resume -> resumeDownloadUseCase(event.task)
                    is DownloadEvent.Cancel -> cancelDownloadUseCase(event.taskId)
                }
            }catch(e: Exception){
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

}