package com.example.filedownloader.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filedownloader.di.DownloadEventBus
import com.example.filedownloader.domain.usecase.FetchMetaDataUseCase
import com.example.filedownloader.presentation.event.UrlInputDialogEvent
import com.example.filedownloader.presentation.state.UrlInputDialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UrlInputDialogViewModel @Inject constructor(
    private val fetchMetaDataUseCase: FetchMetaDataUseCase,
    private val eventBus: DownloadEventBus,
) : ViewModel() {

    private val _urlInputDialogState = MutableStateFlow(UrlInputDialogState())

    val urlInputDialogState: StateFlow<UrlInputDialogState> = _urlInputDialogState.asStateFlow()

    private fun fetchMetaData(url: String) {
        viewModelScope.launch {

            _urlInputDialogState.update { it.copy(isLoading = true) }

            val result = fetchMetaDataUseCase.invoke(url)

            result.fold(
                onSuccess = { taskId ->
                    _urlInputDialogState.update { it.copy(isLoading = false, showDialog = false) }
                    eventBus.publish(taskId.toInt())
                },
                onFailure = {
                    _urlInputDialogState.update { it.copy(isLoading = false, error = "Failed to start download") }
                },
            )
        }
    }

    fun onEvent(event: UrlInputDialogEvent) {
        when(event) {
            is UrlInputDialogEvent.ShowDialog -> _urlInputDialogState.update { it.copy(showDialog = true) }
            is UrlInputDialogEvent.HideDialog -> _urlInputDialogState.update { it.copy(showDialog = false) }
            is UrlInputDialogEvent.FetchMetaData -> fetchMetaData(event.url)
            is UrlInputDialogEvent.OnInputChange -> _urlInputDialogState.update { it.copy(urlInput = event.url) }
        }
    }
}