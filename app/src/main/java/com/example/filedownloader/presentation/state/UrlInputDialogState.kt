package com.example.filedownloader.presentation.state

data class UrlInputDialogState(
    val urlInput: String = "",
    val isLoading: Boolean = false,
    val showDialog: Boolean = false,
    val error: String? = null
)