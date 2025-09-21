package com.example.filedownloader.presentation.event

sealed class UrlInputDialogEvent {
    data object ShowDialog : UrlInputDialogEvent()
    data object HideDialog : UrlInputDialogEvent()
    data class FetchMetaData(val url: String) : UrlInputDialogEvent()
    data class OnInputChange(val url: String) : UrlInputDialogEvent()
}